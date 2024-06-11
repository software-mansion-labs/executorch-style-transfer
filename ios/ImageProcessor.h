//
//  ImageProcessor.h
//  ExpoExecutorch
//
//  Created by Wojtek Jasiński on 21/05/2024.
//

#import <UIKit/UIKit.h>

@interface ImageProcessor : NSObject

+ (UIImage *)resizeImage:(UIImage *)image toSize:(CGSize)newSize;
+ (float *)imageToFloatArray:(UIImage *)image size:(CGSize *)outSize;
+ (UIImage *)imageFromFloatArray:(const float *)array size:(CGSize)size;

@end
