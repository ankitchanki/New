package com.vibe.stashdrop.features.circletosearch

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AuroraCircleOverlayCanvas(bitmap: Bitmap, modifier: Modifier = Modifier, onSelection: (List<Offset>, IntSize) -> Unit,
    initialSelection: List<Offset> = emptyList()) {
    var points by remember(bitmap) { mutableStateOf(initialSelection) }
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    Canvas(modifier.onSizeChanged { dimensions=it }.pointerInput(bitmap) {
        detectDragGestures(onDragStart={ points=listOf(it) }, onDragCancel={ points=emptyList() },
            onDragEnd={ if(points.size>=3) onSelection(points,dimensions) }, onDrag={ change,_ -> change.consume(); points=points.takeLast(2047)+change.position })
    }) {
        drawImage(bitmap.asImageBitmap(),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
        drawRect(Color.Black.copy(alpha=.12f))
        if(points.isNotEmpty()) {
            val path=Path().apply { moveTo(points.first().x,points.first().y); points.drop(1).forEach { lineTo(it.x,it.y) } }
            val colors=listOf(Color(0xFF4285F4),Color(0xFF00E5FF),Color(0xFF9B72CB),Color(0xFFD96570))
            drawPath(path,Brush.linearGradient(colors),style=Stroke(18f),alpha=.2f)
            drawPath(path,Brush.linearGradient(colors),style=Stroke(5f))
            points.takeLast(48).forEachIndexed { i,p -> if(i%3==0) drawCircle(colors[i%4],2f+i/18f,p,alpha=i/48f) }
        }
    }
}
suspend fun cropLasso(bitmap: Bitmap, points: List<Offset>, dimensions: IntSize): Bitmap = withContext(Dispatchers.Default) {
    require(dimensions.width>0 && dimensions.height>0 && points.size>=3)
    val sx=bitmap.width.toFloat()/dimensions.width; val sy=bitmap.height.toFloat()/dimensions.height
    val mapped=points.map { Offset((it.x*sx).coerceIn(0f,bitmap.width-1f),(it.y*sy).coerceIn(0f,bitmap.height-1f)) }
    val left=mapped.minOf { it.x }.toInt(); val top=mapped.minOf { it.y }.toInt()
    val right=kotlin.math.ceil(mapped.maxOf { it.x }.toDouble()).toInt().coerceAtMost(bitmap.width)
    val bottom=kotlin.math.ceil(mapped.maxOf { it.y }.toDouble()).toInt().coerceAtMost(bitmap.height)
    require(right-left>=8 && bottom-top>=8) { "Circle a larger area" }
    val result=Bitmap.createBitmap(right-left,bottom-top,Bitmap.Config.ARGB_8888)
    val canvas=android.graphics.Canvas(result)
    val path=android.graphics.Path().apply {
        moveTo(mapped.first().x-left,mapped.first().y-top)
        mapped.drop(1).forEach { lineTo(it.x-left,it.y-top) }; close()
    }
    canvas.save(); canvas.clipPath(path); canvas.drawBitmap(bitmap,-left.toFloat(),-top.toFloat(),android.graphics.Paint(3)); canvas.restore()
    result
}
