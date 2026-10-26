package com.vibe.stashdrop.features.floatingdock

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.*
import com.vibe.stashdrop.core.physics.IosSpringSpecs
import com.vibe.stashdrop.core.shaders.LiquidMetaball
import com.vibe.stashdrop.ui.theme.*

/** Round floating bubble: tap opens the shelf, hold starts Circle to Search, drag moves it. */
@Composable
fun ThumbDockIconComposable(count: Int, absorbing: Boolean, onTap: ()->Unit, onHold: ()->Unit,
    onDrag: (androidx.compose.ui.geometry.Offset)->Unit, onDragEnd: ()->Unit, modifier: Modifier=Modifier, edgeHandle: Boolean=false, onDragCancel: ()->Unit=onDragEnd) {
    if(edgeHandle) {
        Box(modifier.width(40.dp).height(96.dp).semantics {
            contentDescription="Open edge shelf"; role=Role.Button
            onClick { onTap(); true }
            onLongClick("Google Circle to Search") { onHold(); true }
        }
            .pointerInput(Unit) { detectTapGestures(onTap={onTap()},onLongPress={onHold()}) }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd={
                    // The service handles positioning/dismiss; it opens the panel for inward horizontal pulls.
                    onDragEnd()
                },onDragCancel=onDragCancel) { change,amount -> change.consume(); onDrag(amount) }
            },contentAlignment=Alignment.Center) {
            // Accent pill with the shelf glyph; it widens while something is dragged onto it.
            val accent=MaterialTheme.colorScheme.primary
            val width by animateDpAsState(if(absorbing) 34.dp else 24.dp,IosSpringSpecs.liquid(),label="handle width")
            Box(Modifier.width(width).height(72.dp)
                .graphicsLayer { shadowElevation=8*density; shape=RoundedCornerShape(14.dp); clip=true }
                .background(accent).border(1.dp,Color.White.copy(alpha=.35f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                Icon(Icons.Rounded.Layers,null,Modifier.size(16.dp),tint=Color.White)
            }
        }
        return
    }
    val size by animateDpAsState(if(absorbing) 68.dp else 54.dp,IosSpringSpecs.liquid(),label="dock size")
    val absorption by animateFloatAsState(if(absorbing) 1f else 0f,IosSpringSpecs.liquid(),label="absorption")
    val pulse=remember { Animatable(1f) }
    LaunchedEffect(count) { if(count>0) { pulse.animateTo(1.12f,IosSpringSpecs.landing()); pulse.animateTo(1f,IosSpringSpecs.landing()) } }
    Box(modifier.size(88.dp).pointerInput(Unit) { detectTapGestures(onTap={onTap()},onLongPress={onHold()}) }
        .pointerInput(Unit) { detectDragGestures(onDragEnd=onDragEnd,onDragCancel=onDragEnd) { change,amount -> change.consume(); onDrag(amount) } },contentAlignment=Alignment.Center) {
        val accent=MaterialTheme.colorScheme.primary
        // Soft accent glow while something is dragged over the bubble.
        if(absorption>.01f) Box(Modifier.size(size+14.dp).graphicsLayer { alpha=absorption }
            .background(Brush.radialGradient(listOf(accent.copy(alpha=.35f),accent.copy(alpha=.12f),Color.Transparent)),CircleShape))
        Box(Modifier.size(size).graphicsLayer { scaleX=pulse.value; scaleY=pulse.value; shadowElevation=10*density; shape=CircleShape; clip=true }
            .background(Color.White).border(1.dp,Origin.Hairline,CircleShape),
            contentAlignment=Alignment.Center) {
            if(absorption>.01f) LiquidMetaball(absorption,Modifier.fillMaxSize().graphicsLayer { alpha=absorption })
            Box(Modifier.size(size-16.dp).background(accent,CircleShape),contentAlignment=Alignment.Center) {
                Icon(Icons.Rounded.Layers,"Open stash shelf",Modifier.size(20.dp),tint=Color.White)
            }
        }
        if(count>0) Box(Modifier.align(Alignment.TopEnd).padding(top=12.dp,end=12.dp).defaultMinSize(20.dp,20.dp)
            .background(Origin.Ink,CircleShape).border(2.dp,Color.White,CircleShape).padding(horizontal=5.dp),contentAlignment=Alignment.Center) {
            Text(if(count>99) "99+" else count.toString(),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
        }
    }
}
