package com.vibe.stashdrop.features.floatingdock

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vibe.stashdrop.core.physics.IosSpringSpecs
import com.vibe.stashdrop.core.shaders.*
import com.vibe.stashdrop.core.shaders.GlassSpecularBlurEffect.specularGlass

@Composable
fun ThumbDockIconComposable(count: Int, absorbing: Boolean, onTap: ()->Unit, onHold: ()->Unit,
    onDrag: (androidx.compose.ui.geometry.Offset)->Unit, onDragEnd: ()->Unit, modifier: Modifier=Modifier) {
    val size by animateDpAsState(if(absorbing) 88.dp else 52.dp,IosSpringSpecs.liquid(),label="dock size")
    val absorption by animateFloatAsState(if(absorbing) 1f else 0f,IosSpringSpecs.liquid(),label="absorption")
    val pulse=remember { Animatable(1f) }
    LaunchedEffect(count) { if(count>0) { pulse.animateTo(1.18f,IosSpringSpecs.landing()); pulse.animateTo(1f,IosSpringSpecs.landing()) } }
    Box(modifier.size(88.dp).pointerInput(Unit) { detectTapGestures(onTap={onTap()},onLongPress={onHold()}) }
        .pointerInput(Unit) { detectDragGestures(onDragEnd=onDragEnd,onDragCancel=onDragEnd) { change,amount -> change.consume(); onDrag(amount) } },contentAlignment=Alignment.Center) {
        Box(Modifier.size(size).graphicsLayer { scaleX=pulse.value; scaleY=pulse.value; shape=RoundedCornerShape(22.dp); clip=true }
            .specularGlass(),contentAlignment=Alignment.Center) {
            LiquidMetaball(absorption,Modifier.fillMaxSize().graphicsLayer {
                if (android.os.Build.VERSION.SDK_INT >= 31 && absorption > .01f)
                    renderEffect = GlassSpecularBlurEffect.platformBlur(2f + absorption * 4f)?.asComposeRenderEffect()
            })
            Icon(Icons.Outlined.Layers,"Open stash shelf",Modifier.size(27.dp),tint=Color(0xFFD3DEFF))
        }
        if(count>0) Surface(Modifier.align(Alignment.TopEnd).padding(8.dp),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primary) {
            Text(if(count>99) "99+" else count.toString(),Modifier.padding(horizontal=6.dp,vertical=1.dp),style=MaterialTheme.typography.labelSmall)
        }
    }
}
