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
            Box(Modifier.width(if(absorbing) 8.dp else 4.dp).height(44.dp)
                .background(Ice.copy(alpha=if(absorbing) .9f else .4f),RoundedCornerShape(4.dp)))
        }
        return
    }
    val size by animateDpAsState(if(absorbing) 68.dp else 54.dp,IosSpringSpecs.liquid(),label="dock size")
    val absorption by animateFloatAsState(if(absorbing) 1f else 0f,IosSpringSpecs.liquid(),label="absorption")
    val pulse=remember { Animatable(1f) }
    LaunchedEffect(count) { if(count>0) { pulse.animateTo(1.12f,IosSpringSpecs.landing()); pulse.animateTo(1f,IosSpringSpecs.landing()) } }
    Box(modifier.size(88.dp).pointerInput(Unit) { detectTapGestures(onTap={onTap()},onLongPress={onHold()}) }
        .pointerInput(Unit) { detectDragGestures(onDragEnd=onDragEnd,onDragCancel=onDragEnd) { change,amount -> change.consume(); onDrag(amount) } },contentAlignment=Alignment.Center) {
        // Soft glow ring while something is dragged over the bubble.
        if(absorption>.01f) Box(Modifier.size(size+14.dp).graphicsLayer { alpha=absorption }
            .background(Brush.radialGradient(listOf(Ice.copy(alpha=.45f),Mint.copy(alpha=.18f),Color.Transparent)),CircleShape))
        Box(Modifier.size(size).graphicsLayer { scaleX=pulse.value; scaleY=pulse.value; shadowElevation=10*density; shape=CircleShape; clip=true }
            .background(Brush.linearGradient(listOf(Color(0xFF2C3754),Color(0xFF151A27))))
            .border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=.55f),Ice.copy(alpha=.12f),Mint.copy(alpha=.4f))),CircleShape),
            contentAlignment=Alignment.Center) {
            if(absorption>.01f) LiquidMetaball(absorption,Modifier.fillMaxSize().graphicsLayer { alpha=absorption })
            Icon(Icons.Rounded.Layers,"Open stash shelf",Modifier.size(24.dp),tint=Ice)
        }
        if(count>0) Box(Modifier.align(Alignment.TopEnd).padding(top=12.dp,end=12.dp).defaultMinSize(20.dp,20.dp)
            .background(Mint,CircleShape).border(2.dp,Color(0xFF151A27),CircleShape).padding(horizontal=5.dp),contentAlignment=Alignment.Center) {
            Text(if(count>99) "99+" else count.toString(),color=Color(0xFF0C0E15),fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
        }
    }
}
