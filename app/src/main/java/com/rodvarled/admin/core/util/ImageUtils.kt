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

private const val MAX_DIMENSION = 1280
private const val MAX_SOURCE_BYTES = 25 * 1024 * 1024

/** Lee una imagen desde una Uri, la reduce de tamaño y la regresa como JPEG en base64 listo para subir. */
fun Uri.toCompressedBase64(context: Context, quality: Int = 82): String? {
    return runCatching {
        val input = context.contentResolver.openInputStream(this) ?: return null
        val bytes = input.use { it.readBytes() }
        if (bytes.isEmpty() || bytes.size > MAX_SOURCE_BYTES) return null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        )
            ?: return null

        val rotated = fixOrientation(decoded, bytes)
        try {
            val output = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, quality, output)
            return "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (rotated !== decoded && !rotated.isRecycled) rotated.recycle()
            if (!decoded.isRecycled) decoded.recycle()
        }
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
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap && !bitmap.isRecycled) bitmap.recycle()
        }
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
