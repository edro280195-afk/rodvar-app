package com.rodvarled.admin.core.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

object InventoryLabelRenderer {
    // La B1 trabaja a 203 dpi y tiene un cabezal de 384 puntos (~48 mm).
    // Mantener el lienzo en ese ancho evita recortes y paquetes BLE demasiado grandes.
    const val LABEL_SIZE = 384

    fun render(productName: String, variantName: String?, code: String): Bitmap {
        val bitmap = Bitmap.createBitmap(LABEL_SIZE, LABEL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        canvas.drawRect(0f, 0f, LABEL_SIZE.toFloat(), 48f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = fitText(productName, 22f, LABEL_SIZE - 32, paint)
        canvas.drawText(productName.take(25), 16f, 31f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 16f
        variantName?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText(it.take(28), 16f, 69f, paint)
        }

        // Margen de seguridad amplio: el QR conserva su zona silenciosa aun con
        // pequeñas variaciones de contraste o alineación de la etiqueta.
        val qrSize = 244
        val qr = createQr(code, qrSize)
        canvas.drawBitmap(qr, ((LABEL_SIZE - qrSize) / 2).toFloat(), 78f, paint)

        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 22f
        val codeWidth = paint.measureText(code)
        canvas.drawText(code, (LABEL_SIZE - codeWidth) / 2f, 348f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        val footer = "ESCANEA PARA INVENTARIO"
        val footerWidth = paint.measureText(footer)
        canvas.drawText(footer, (LABEL_SIZE - footerWidth) / 2f, 371f, paint)
        return bitmap
    }

    private fun fitText(text: String, initialSize: Float, maxWidth: Int, paint: Paint): Float {
        var size = initialSize
        paint.textSize = size
        while (size > 12f && paint.measureText(text.take(25)) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }
        return size
    }

    private fun createQr(content: String, size: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, 4)
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H)
        }
        val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        return bitmap
    }
}
