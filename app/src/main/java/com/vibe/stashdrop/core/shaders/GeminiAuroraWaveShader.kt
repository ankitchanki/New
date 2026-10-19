package com.vibe.stashdrop.core.shaders

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

object GeminiAuroraWaveShader {
    const val source = """
        uniform float2 u_resolution;
        uniform float2 u_touchPos;
        uniform float u_time;
        uniform float u_progress;
        half4 main(float2 p) {
            float2 uv = p / u_resolution;
            float finger = u_touchPos.x / u_resolution.x;
            float dome = exp(-pow((uv.x - finger)*4.0, 2.0));
            float wave = sin(uv.x*13.0 + u_time*1.8)*0.018 + sin(uv.x*24.0-u_time)*0.009;
            float crest = 1.0 - u_progress*(0.10 + dome*0.72) + wave*u_progress;
            float glow = exp(-abs(uv.y-crest)*42.0);
            float body = smoothstep(crest-0.008, crest+0.02, uv.y)*0.50;
            float band = 0.5 + 0.5*sin(uv.x*6.0-u_time*0.6 + dome*2.0);
            half3 blue = half3(0.259,0.522,0.957);
            half3 violet = half3(0.608,0.447,0.796);
            half3 cyan = half3(0.0,0.898,1.0);
            half3 pink = half3(0.851,0.396,0.439);
            half3 color = mix(mix(blue,violet,band),mix(cyan,pink,band),sin(uv.x*4.0+u_time)*0.5+0.5);
            float edge = min(min(p.x, u_resolution.x-p.x), min(p.y,u_resolution.y-p.y));
            float shock = exp(-edge/7.0)*smoothstep(0.85,1.0,u_progress);
            half alpha = half(clamp((glow+body)*u_progress + shock,0.0,0.92));
            return half4(color*alpha, alpha);
        }
    """
}
@RequiresApi(33)
private class AuroraRuntime {
    val shader = RuntimeShader(GeminiAuroraWaveShader.source)
    val paint = android.graphics.Paint().apply { shader = this@AuroraRuntime.shader }
}
@Composable
fun AuroraWave(progress: Float, touch: Offset, modifier: Modifier = Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress > .001f) {
        if (progress > .001f) { val start = withFrameNanos { it }; while (true) withFrameNanos { time = (it-start)/1_000_000_000f } }
    }
    val runtime = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { AuroraRuntime() }.getOrNull() else null }
    Canvas(modifier) {
        if (progress <= 0f) return@Canvas
        if (Build.VERSION.SDK_INT >= 33 && runtime != null) {
            runtime.shader.setFloatUniform("u_resolution", size.width, size.height)
            runtime.shader.setFloatUniform("u_touchPos", touch.x, touch.y)
            runtime.shader.setFloatUniform("u_time", time)
            runtime.shader.setFloatUniform("u_progress", progress.coerceIn(0f,1f))
            drawContext.canvas.nativeCanvas.drawRect(0f,0f,size.width,size.height,runtime.paint)
        } else {
            val height = size.height*progress*.8f
            val path = Path().apply {
                moveTo(0f,size.height)
                cubicTo(touch.x*.45f, size.height-height*.12f, touch.x*.65f, size.height-height, touch.x, size.height-height)
                cubicTo(touch.x+(size.width-touch.x)*.35f,size.height-height,size.width*.95f,size.height-height*.12f,size.width,size.height)
                close()
            }
            val colors = listOf(Color(0xFF4285F4),Color(0xFF9B72CB),Color(0xFF00E5FF),Color(0xFFD96570))
            drawPath(path, Brush.radialGradient(colors, Offset(touch.x + sin(time)*30f, size.height), size.width*.9f), alpha = .8f)
            val nativePath = path.asAndroidPath()
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE; strokeWidth = 14f; color = android.graphics.Color.CYAN
                maskFilter = android.graphics.BlurMaskFilter(18f,android.graphics.BlurMaskFilter.Blur.NORMAL)
                alpha = (progress*180).toInt()
            }
            drawContext.canvas.nativeCanvas.drawPath(nativePath,paint)
            if (progress > .85f) drawRect(Brush.linearGradient(colors), style=Stroke(4f), alpha=(progress-.85f)/.15f)
        }
    }
}
