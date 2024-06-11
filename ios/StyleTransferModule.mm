#import "StyleTransferModule.h"
#import "ImageProcessor.h"
#import <UIKit/UIKit.h>
#import <executorch/extension/module/module.h>

using namespace ::torch::executor;

const int32_t imageSize = 640;
const int32_t numChannels = 3;

@implementation StyleTransferModule {
  std::unordered_map<std::string, std::unique_ptr<Module>> *_models;
}

RCT_EXPORT_METHOD(initModules:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  static std::unordered_map<std::string, std::unique_ptr<Module>> models;

  NSDictionary *modelNames = @{
    @"candy": @"__candy_coreml_all",
    @"mosaic": @"__mosaic_coreml_all",
    @"rain_princess": @"__rain_princess_coreml_all",
    @"udnie": @"__udnie_coreml_all"
  };
  
  for (NSString *key in modelNames) {
    NSString *resourceName = [modelNames[key] stringByDeletingPathExtension];
    NSString *styleTransferPath = [[NSBundle mainBundle] pathForResource:resourceName ofType:@"pte"];

    if (!styleTransferPath) {
      reject(@"file_not_found", [NSString stringWithFormat:@"File not found for key %@", key], nil);
      return;
    }
    std::unique_ptr<Module> model = std::make_unique<Module>(styleTransferPath.UTF8String);
    
    if (!model) {
      reject(@"module_error", [NSString stringWithFormat:@"Failed to create module %@", key], nil);
      return;
    }
    
    models[key.UTF8String] = std::move(model);
  }
  
  _models = &models;

  resolve(@"Modules created successfully");
}

RCT_EXPORT_METHOD(applyStyleTransfer:(NSString *)moduleName image:(NSString *)imageUri resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  //get module of choice
  if (!_models) {
   reject(@"module_not_initialized", @"Modules not initialized", nil);
   return;
  }

  // get the module
  std::string moduleNameStr = [moduleName UTF8String];
  auto it = _models->find(moduleNameStr);
  if (it == _models->end()) {
   reject(@"module_not_found", [NSString stringWithFormat:@"Module not found: %@", moduleName], nil);
   return;
  }
  std::unique_ptr<Module> &_module = it->second;
  
  // load image from URI
  NSURL *url = [NSURL URLWithString:imageUri];
  NSData *data = [NSData dataWithContentsOfURL:url];
  if (!data) {
    reject(@"img_loading_error", @"Unable to load image data", nil);
    return;
  }
  UIImage *inputImage = [UIImage imageWithData:data];
  
  // resize
  CGSize targetSize = CGSizeMake(imageSize, imageSize);
  UIImage *resizedImage = [ImageProcessor resizeImage:inputImage toSize:targetSize];
  
  // to float array
  CGSize outSize = {imageSize, imageSize};
  float *imageData = [ImageProcessor imageToFloatArray:resizedImage size:&outSize];
  
  // make it a tensor
  int32_t sizes[] = {1, numChannels, imageSize, imageSize};
  TensorImpl inputTensorImpl(ScalarType::Float, std::size(sizes), sizes, imageData);
  Tensor inputTensor = Tensor(&inputTensorImpl);
  
  // run the model
  const auto result = _module.get()->forward({EValue(inputTensor)});
  if (!result.ok()) {
    NSError *error = [NSError
                      errorWithDomain:@"ModelForwardFailure"
                      code:NSInteger(result.error())
                      userInfo:@{NSLocalizedDescriptionKey: [NSString stringWithFormat:@"Failed to run forward on the torch module, error code: %i", result.error()]}];
    reject(@"model_failure", error.localizedDescription, error);
  }
  const float *outputData = result->at(0).toTensor().const_data_ptr<float>();
  
  free(imageData);
  
  // from float array to image
  CGSize outputSize = CGSizeMake(imageSize, imageSize);
  UIImage *outputImage = [ImageProcessor imageFromFloatArray:outputData size:outputSize];
  
  // save img to tmp dir, return URI
  NSString *outputPath = [NSTemporaryDirectory() stringByAppendingPathComponent:[moduleName stringByAppendingString:@".png"]];
  if ([UIImagePNGRepresentation(outputImage) writeToFile:outputPath atomically:YES]) {
    NSURL *fileURL = [NSURL fileURLWithPath:outputPath];
    resolve([fileURL absoluteString]);
  } else {
    reject(@"img_write_error", @"Failed to write processed image to file", nil);
  }
}

RCT_EXPORT_MODULE(StyleTransferModule);

@end
