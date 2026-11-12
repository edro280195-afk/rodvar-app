package com.rodvarled.admin.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Componente que genera y dibuja un Código QR real escaneable para folios de garantía,
 * citas o validación de clientes sin depender de librerías externas pesadas.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 160.dp
) {
    val qrPixelSize = with(LocalDensity.current) { sizeDp.roundToPx() }.coerceAtLeast(1)
    val bitmap = remember(content, qrPixelSize) {
        generateQrBitmap(content, qrPixelSize, qrPixelSize)
    }

    Box(
        modifier = modifier
            .size(sizeDp + 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR de Validación",
            modifier = Modifier.size(sizeDp)
        )
    }
}

internal fun generateQrMatrix(data: String, width: Int, height: Int): BitMatrix {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 4
    )

    return QRCodeWriter().encode(
        data,
        BarcodeFormat.QR_CODE,
        width,
        height,
        hints
    )
}

private fun generateQrBitmap(data: String, width: Int, height: Int): Bitmap {
    val matrix = generateQrMatrix(data, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
