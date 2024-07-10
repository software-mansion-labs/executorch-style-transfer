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
import java.io.File
import java.io.FileOutputStream

class StyleTransferModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "StyleTransferModule"
    private val modelResourceIds: Map<String, Int> = mapOf(
        "mosaic" to R.raw.mosaic_xnnpack_640_fp32,
        "candy" to R.raw.candy_xnnpack_640_fp32,
        "rain_princess" to R.raw.rain_princess_xnnpack_640_fp32,
        "udnie" to R.raw.udnie_xnnpack_640_fp32
    )
    private val modules: MutableMap<String, Module> = mutableMapOf()

    @ReactMethod
    fun initModules(promise: Promise) {
        try {
            modelResourceIds.forEach { (modelName, resourceId) ->
                modules[modelName] = loadModuleFromRawResource(resourceId)
            }
            promise.resolve("All models loaded successfully")
        } catch (e: Exception) {
            promise.reject("Cannot load all models", e)
        }
    }

    @ReactMethod
    fun applyStyleTransfer(styleName: String, imageUri: String, promise: Promise) {
        try {
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

            // run the model
            val outputTensor = modules[styleName]!!.forward(EValue.from(inputTensor))[0].toTensor()

            // convert the output back to a bitmap and save the result
            val outputBitmap = TensorUtils.float32TensorToBitmap(outputTensor)
            val outputUri = BitmapUtils.saveToTempFile(outputBitmap, styleName)

            promise.resolve(outputUri.toString())
        } catch (e: Exception) {
            promise.reject("Error", e)
        }
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