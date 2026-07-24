package com.rodvarled.admin.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Estado hoisted del pad de firma: guarda los trazos y sabe exportarse a PNG base64. */
class SignaturePadState {
    val strokes = mutableStateListOf<List<Offset>>()

    // El trazo en curso también es estado observable: sin esto, la firma no se veía
    // mientras se arrastraba el dedo (solo aparecía al soltar = "delay horrible").
    val currentStroke = mutableStateListOf<Offset>()
    var canvasSize by mutableStateOf(IntSize.Zero)
        internal set

    val hasSignature: Boolean get() = strokes.isNotEmpty() || currentStroke.size > 1

    internal fun startStroke(point: Offset) {
        currentStroke.clear()
        currentStroke.add(point)
    }

    internal fun appendToStroke(point: Offset) {
        currentStroke.add(point)
    }

    internal fun endStroke() {
        if (currentStroke.size > 1) strokes.add(currentStroke.toList())
        currentStroke.clear()
    }

    fun clear() {
        strokes.clear()
        currentStroke.clear()
    }

    /** Redibuja los trazos capturados sobre un bitmap real y lo exporta como PNG base64 (fondo blanco). */
    fun exportPngBase64(): String? {
        if (!hasSignature || canvasSize.width <= 0 || canvasSize.height <= 0) return null

        val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint().apply {
            color = AndroidColor.BLACK
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val allStrokes = strokes + listOf(currentStroke.toList())
        allStrokes.forEach { stroke ->
            for (i in 0 until stroke.size - 1) {
                canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
            }
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

@Composable
fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> state.startStroke(offset) },
                    onDrag = { change, _ -> state.appendToStroke(change.position) },
                    onDragEnd = { state.endStroke() },
                    onDragCancel = { state.endStroke() }
                )
            }
    ) {
        state.canvasSize = IntSize(size.width.toInt(), size.height.toInt())

        // Trazos terminados + el trazo en curso (para que la firma se vea en tiempo real)
        val finished = state.strokes
        val inProgress = state.currentStroke

        finished.forEach { stroke ->
            if (stroke.size > 1) {
                val path = Path()
                path.moveTo(stroke.first().x, stroke.first().y)
                for (point in stroke.drop(1)) path.lineTo(point.x, point.y)
                drawPath(path, color = Color.Black, style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }

        if (inProgress.size > 1) {
            val path = Path()
            path.moveTo(inProgress.first().x, inProgress.first().y)
            for (point in inProgress.drop(1)) path.lineTo(point.x, point.y)
            drawPath(path, color = Color.Black, style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}
