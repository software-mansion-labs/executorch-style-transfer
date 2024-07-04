package com.anonymous.ExpoExecutorch

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BitmapUtils {
    companion object {
        fun saveToTempFile(bitmap: Bitmap, fileName: String): Uri {
            val tempFile = File.createTempFile(fileName, ".png")
            var outputStream : FileOutputStream? = null
            try {
                outputStream = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            finally {
                outputStream?.close()
            }
            return tempFile.toUri()
        }

        fun handleBitmapOrientation(
            imageUri: Uri,
            contentResolver: ContentResolver,
            bitmap: Bitmap,
        ): Bitmap {
            val exifInputStream = contentResolver.openInputStream(imageUri)!!
            val exif = ExifInterface(exifInputStream)
            exifInputStream.close()
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(
                    bitmap,
                    horizontal = true,
                    vertical = false
                )
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(
                    bitmap,
                    horizontal = false,
                    vertical = true
                )
                else -> bitmap
            }
        }

        private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.preScale(
                if (horizontal) -1f else 1f,
                if (vertical) -1f else 1f
            )
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}