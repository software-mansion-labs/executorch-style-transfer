package com.anonymous.ExpoExecutorch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
                Log.d("StyleTransferModule", "Loading model: $modelName")
                modules[modelName] = loadModuleFromRawResource(resourceId)
                Log.d("StyleTransferModule", "$modelName model loaded")
            }
            promise.resolve("All models loaded successfully")
        } catch (e: Exception) {
            Log.e("StyleTransferModule", "Error loading models", e)
            promise.reject("Cannot load all models", e)
        }
    }

    @ReactMethod
    fun applyStyleTransfer(styleName: String, imageUri: String, promise: Promise) {
        try {
            Log.i("StyleTransferModule", "applyStyleTransfer called with imageUri: $imageUri")
            val uri = Uri.parse(imageUri)
            val bitmapInputStream = reactApplicationContext.contentResolver.openInputStream(uri)!!
            val rawBitmap = BitmapFactory.decodeStream(bitmapInputStream)
            bitmapInputStream.close()

            val rotatedBitmap = BitmapUtils.handleBitmapOrientation(uri, reactApplicationContext.contentResolver, rawBitmap)
            val inputBitmap = Bitmap.createScaledBitmap(
                rotatedBitmap,
                640, 640, true
            )
            val inputTensor = TensorUtils.bitmapToFloat32Tensor(inputBitmap)
            val t1 = System.currentTimeMillis()
            val outputTensor = modules[styleName]!!.forward(EValue.from(inputTensor))[0].toTensor()
            val t2 = System.currentTimeMillis()
            val outputBitmap = TensorUtils.float32TensorToBitmap(outputTensor)
            val outputUri = BitmapUtils.saveToTempFile(outputBitmap, styleName)

            val inferenceTime = t2 - t1
            Log.i("StyleTransferModule", "applyStyleTransfer inference time: $inferenceTime ms, returns imageUri$outputUri, ")
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