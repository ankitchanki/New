package com.vibe.stashdrop.features.dragdrop

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.vibe.stashdrop.core.physics.*

/** In-app flight feedback. External drag tracking and landing belong to Android's drag shadow. */
@Composable
fun CrossAppDropFlightOverlay(flight: MagneticFlight?, modifier: Modifier=Modifier, onComplete: ()->Unit) {
    val progress=remember { Animatable(0f) }
    LaunchedEffect(flight) { if(flight!=null) { progress.snapTo(0f); progress.animateTo(1f,IosSpringSpecs.landing()); onComplete() } }
    Canvas(modifier) {
        val path=flight ?: return@Canvas
        val p=path.position(progress.value); val scale=1f-.85f*progress.value.coerceIn(0f,1f)
        drawCircle(Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFB4C9FF),androidx.compose.ui.graphics.Color.Transparent),Offset(p.x,p.y),48f*scale),48f*scale,Offset(p.x,p.y))
    }
}
