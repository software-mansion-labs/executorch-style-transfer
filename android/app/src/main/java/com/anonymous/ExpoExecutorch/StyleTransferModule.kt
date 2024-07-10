package com.anonymous.ExpoExecutorch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream

class StyleTransferModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "StyleTransferModule"
    private val modelNameToResourceId: Map<String, Int> = mapOf(
        "mosaic" to R.raw.mosaic_xnnpack_640_fp32,
        "candy" to R.raw.candy_xnnpack_640_fp32,
        "rain_princess" to R.raw.rain_princess_xnnpack_640_fp32,
        "udnie" to R.raw.udnie_xnnpack_640_fp32
    )
    private val models: MutableMap<String, Module> = mutableMapOf()

    @ReactMethod
    fun initModules(promise: Promise) {
        try {
            modelNameToResourceId.forEach { (modelName, resourceId) ->
                models[modelName] = loadModuleFromRawResource(resourceId)
            }
            promise.resolve("All models loaded successfully")
        } catch (e: Exception) {
            promise.reject("InitModulesError", e)
        }
    }

    @ReactMethod
    fun applyStyleTransfer(moduleName: String, imageUri: String, promise: Promise) {
        val module = models[moduleName]
        if (module == null) {
            val errorMessage = "Module $moduleName was not initialized!"
            promise.reject("ModelNotInitializedError", Error(errorMessage))
            return
        }
        // load an image (bitmap) from URI
        val uri = Uri.parse(imageUri)
        val bitmapInputStream = reactApplicationContext.contentResolver.openInputStream(uri)!!
        val rawBitmap = BitmapFactory.decodeStream(bitmapInputStream)
        bitmapInputStream.close()

        // rotate if needed, resize and  make it a tensor
        val rotatedBitmap = BitmapUtils.handleBitmapOrientation(uri, reactApplicationContext.contentResolver, rawBitmap)
        val inputBitmap = Bitmap.createScaledBitmap(
            rotatedBitmap,
            640, 640, true
        )
        val inputTensor = TensorUtils.bitmapToFloat32Tensor(inputBitmap)

        // wrap the tensor with EValue, pass it to the .forward() method of the module
        lateinit var outputTensor : Tensor
        try {
            module.forward(EValue.from(inputTensor))[0].toTensor()
                .also { outputTensor = it }
        } catch(e: Exception) {
            promise.reject("InferenceError", e)
        }

        // convert the output back to a bitmap and save the result
        val outputBitmap = TensorUtils.float32TensorToBitmap(outputTensor)
        val outputUri = BitmapUtils.saveToTempFile(outputBitmap, moduleName)
        // return the URI to the saved file
        promise.resolve(outputUri.toString())
    }

    private fun loadModuleFromRawResource(resourceId: Int): Module {
        reactApplicationContext.resources.openRawResource(resourceId).use { inputStream ->
            val file = File(
                reactApplicationContext.filesDir,
                reactApplicationContext.resources.getResourceEntryName(resourceId) + ".pte"
            )
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            return Module.load(file.absolutePath)
        }
    }

}