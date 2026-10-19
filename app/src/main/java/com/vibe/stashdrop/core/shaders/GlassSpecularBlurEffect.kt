package com.vibe.stashdrop.core.shaders

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke

object GlassSpecularBlurEffect {
    fun platformBlur(radius: Float): android.graphics.RenderEffect? = if(Build.VERSION.SDK_INT>=31)
        android.graphics.RenderEffect.createBlurEffect(radius.coerceAtLeast(.1f),radius.coerceAtLeast(.1f),android.graphics.Shader.TileMode.CLAMP) else null
    /** Specular surface. Android overlays cannot sample the pixels of other apps for backdrop blur. */
    fun Modifier.specularGlass() = drawWithContent {
        drawRoundRect(Color(0xD916181F),cornerRadius=CornerRadius(22f,22f))
        drawContent()
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=.65f),Color(0xFF8CA9FF).copy(alpha=.08f),Color(0xFFDDAEFF).copy(alpha=.5f))),
            cornerRadius=CornerRadius(22f,22f),style=Stroke(1.2f*density))
    }
}
