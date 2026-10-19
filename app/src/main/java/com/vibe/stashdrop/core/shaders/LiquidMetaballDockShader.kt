package com.vibe.stashdrop.core.shaders

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.math.*

object LiquidMetaballDockShader {
    const val source = """
        uniform float2 u_resolution;
        uniform float u_time;
        uniform float u_absorption;
        half4 main(float2 p) {
            float2 uv = (p-u_resolution*0.5)/min(u_resolution.x,u_resolution.y);
            float d1 = length(uv);
            float2 satellite = float2(cos(u_time),sin(u_time))*0.28*u_absorption;
            float d2 = length(uv-satellite);
            float field = 0.075/(d1*d1+0.025) + 0.030*u_absorption/(d2*d2+0.015);
            float alpha = smoothstep(0.70,0.85,field);
            float rim = exp(-abs(field-0.85)*6.0);
            half3 tint = mix(half3(0.05,0.07,0.12),half3(0.32,0.65,1.0),half(rim));
            tint += half3(0.30,0.06,0.25)*half(sin(uv.x*9.0+u_time)*0.5+0.5)*half(rim);
            return half4(tint*half(alpha),half(alpha*0.96));
        }
    """
}
@RequiresApi(33)
private class MetaballRuntime {
    val shader = RuntimeShader(LiquidMetaballDockShader.source)
    val paint = android.graphics.Paint().apply { shader = this@MetaballRuntime.shader }
}
@Composable
fun LiquidMetaball(absorption: Float, modifier: Modifier = Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(absorption > .01f) { if (absorption > .01f) { val start=withFrameNanos { it }; while(true) withFrameNanos { time=(it-start)/1e9f } } }
    val runtime = remember { if (Build.VERSION.SDK_INT>=33) runCatching { MetaballRuntime() }.getOrNull() else null }
    Canvas(modifier) {
        if (Build.VERSION.SDK_INT>=33 && runtime!=null) {
            runtime.shader.setFloatUniform("u_resolution",size.width,size.height)
            runtime.shader.setFloatUniform("u_time",time); runtime.shader.setFloatUniform("u_absorption",absorption)
            drawContext.canvas.nativeCanvas.drawRect(0f,0f,size.width,size.height,runtime.paint)
        } else {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF567BC1),Color(0xFF16181F))),size.minDimension*.43f)
            if(absorption>.01f) drawCircle(Brush.radialGradient(listOf(Color(0xFF00E5FF),Color.Transparent)),
                size.minDimension*.25f*absorption, center+Offset(cos(time),sin(time))*size.minDimension*.2f)
        }
    }
}
