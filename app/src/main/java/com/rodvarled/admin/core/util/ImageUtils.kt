package com.rodvarled.admin.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val MAX_DIMENSION = 1600

/** Lee una imagen desde una Uri, la reduce de tamaño y la regresa como JPEG en base64 listo para subir. */
fun Uri.toCompressedBase64(context: Context, quality: Int = 82): String? {
    return runCatching {
        val input = context.contentResolver.openInputStream(this) ?: return null
        val bytes = input.use { it.readBytes() }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        var sampleSize = 1
        while (options.outWidth / sampleSize > MAX_DIMENSION || options.outHeight / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return null

        val rotated = fixOrientation(decoded, bytes)

        val output = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, quality, output)
        "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()
}

private fun fixOrientation(bitmap: Bitmap, originalBytes: ByteArray): Bitmap {
    return runCatching {
        val exif = ExifInterface(originalBytes.inputStream())
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrDefault(bitmap)
}

fun createTempCameraFile(context: Context): File {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    return File(dir, "capture_${System.currentTimeMillis()}.jpg")
}

fun String.base64ToBitmap(): Bitmap? = runCatching {
    val data = if (contains(",")) substringAfter(",") else this
    val bytes = Base64.decode(data, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()
