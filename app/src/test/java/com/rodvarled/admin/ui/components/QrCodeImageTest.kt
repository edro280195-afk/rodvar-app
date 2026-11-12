package com.rodvarled.admin.ui.components

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeImageTest {
    @Test
    fun generatedQrMatrixDecodesOriginalContent() {
        val content = "https://rodvarled.store/garantia/GAR-260929-4532"
        val matrix = generateQrMatrix(content, 600, 600)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            val x = index % matrix.width
            val y = index / matrix.width
            if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        val source = RGBLuminanceSource(matrix.width, matrix.height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(
            bitmap,
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        )

        assertEquals(content, result.text)
    }
}
