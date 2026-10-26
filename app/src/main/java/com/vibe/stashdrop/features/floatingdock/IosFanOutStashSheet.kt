package com.vibe.stashdrop.features.floatingdock

import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.stashdrop.core.physics.*
import com.vibe.stashdrop.features.dragdrop.IosCardStackDragShadow
import com.vibe.stashdrop.features.stashshelf.*
import com.vibe.stashdrop.ui.shelf.*
import com.vibe.stashdrop.ui.theme.*

@Composable
fun IosFanOutStashSheet(items: List<StashItemEntity>, repository: StashRepository, onDismiss: ()->Unit, onSearch: ()->Unit,
    onShare: (List<StashItemEntity>,Boolean)->Unit, onDelete: (StashItemEntity)->Unit,
    onGlobalDrag: (View,List<StashItemEntity>)->Unit, compact: Boolean=false) {
    var filter by remember { mutableStateOf(ShelfCategory.ALL) }
    val visible=items.filter { filter.matches(it) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var deleting by remember { mutableStateOf<StashItemEntity?>(null) }
    val currentItems by rememberUpdatedState(items)
    val currentSelected by rememberUpdatedState(selected)
    LaunchedEffect(items) { selected=selected.intersect(items.map { it.id }.toSet()) }
    val view=LocalView.current
    val context=androidx.compose.ui.platform.LocalContext.current
    val haptics=remember { com.vibe.stashdrop.core.haptics.TactileHapticEngine(context) }
    var dragVelocity by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val tracker=remember { androidx.compose.ui.input.pointer.util.VelocityTracker() }
    val inspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val reveal=remember { Animatable(if (inspection) 0f else 28f) }
    LaunchedEffect(Unit) { if (!inspection) reveal.animateTo(0f,IosSpringSpecs.liquid()) }
    Box(Modifier.fillMaxSize().then(if(compact) Modifier else Modifier.background(Color.Black.copy(alpha=.24f)).clickable(onClick=onDismiss)),contentAlignment=Alignment.BottomCenter) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(if(compact) 1f else .60f).graphicsLayer { translationY=reveal.value }.clickable(interactionSource=null,indication=null) {},
            shape=RoundedCornerShape(28.dp),color=Origin.Ground,shadowElevation=if(compact) 0.dp else 12.dp) {
            Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start=4.dp)) {
                        Text("Shelf",style=MaterialTheme.typography.titleLarge,color=Origin.Ink,maxLines=1)
                        Text("Hold a card to drag",fontSize=11.sp,color=Origin.Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    IconButton(onClick=onSearch,colors=IconButtonDefaults.iconButtonColors(containerColor=MaterialTheme.colorScheme.primary,contentColor=Color.White)) {
                        Icon(Icons.Outlined.Search,"Circle to Search",Modifier.size(20.dp)) }
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick=onDismiss,colors=IconButtonDefaults.iconButtonColors(containerColor=Color.White,contentColor=Origin.Ink)) {
                        Icon(Icons.Outlined.Close,"Close shelf",Modifier.size(20.dp)) }
                }
                ShelfTypeFilters(filter) { filter=it; selected=emptySet() }
                if(visible.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                    IconBadge(Icons.Outlined.Inventory2,MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.primary,size=48.dp,corner=16.dp)
                    Text("No ${filter.label.lowercase()} here",Modifier.padding(top=8.dp),style=MaterialTheme.typography.titleMedium,maxLines=1)
                    Text("Share to StashDrop from another app",color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
                }
                else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(visible,key={_,item->item.id}) { index,item ->
                        val chosen=item.id in selected
                        val angle by animateFloatAsState(if(chosen) listOf(-4f,2f,5f)[index%3] else 0f,IosSpringSpecs.follow(),label="card tilt")
                        val scale by animateFloatAsState(if(chosen) .96f else 1f,IosSpringSpecs.landing(),label="stack scale")
                        SwipeDeleteItem(onDeleteRequest={deleting=item}) {
                        Card(modifier=Modifier.fillMaxWidth().graphicsLayer { rotationZ=angle; scaleX=scale; scaleY=scale; cameraDistance=12*density; shadowElevation=if(chosen) 14f else 2f
                            rotationX=MagneticTrajectoryEngine.tilt(-dragVelocity.y)
                            rotationY=MagneticTrajectoryEngine.tilt(dragVelocity.x)
                        }
                            .pointerInput(item.id) { detectDragGesturesAfterLongPress(onDragStart={
                                tracker.resetTracking(); haptics.confirm()
                                val picked=currentItems.filter { it.id in currentSelected }.ifEmpty { listOf(item) }
                                onGlobalDrag(view,picked)
                            },onDragEnd={dragVelocity=androidx.compose.ui.geometry.Offset.Zero},onDragCancel={dragVelocity=androidx.compose.ui.geometry.Offset.Zero},onDrag={change,_->
                                change.consume();tracker.addPosition(change.uptimeMillis,change.position)
                                val velocity=tracker.calculateVelocity()
                                dragVelocity=androidx.compose.ui.geometry.Offset(velocity.x,velocity.y)
                            }) },
                            onClick={ haptics.select(); selected=if(chosen) selected-item.id else selected+item.id },
                            shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),
                            border=if(chosen) BorderStroke(2.dp,MaterialTheme.colorScheme.primary) else null) {
                            if(item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")) {
                                Box(Modifier.fillMaxWidth().padding(6.dp)) {
                                    ShelfMediaThumbnail(item,repository.file(item),Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(16.dp)))
                                    if(chosen) Icon(Icons.Outlined.CheckCircle,"Selected",Modifier.align(Alignment.TopEnd).padding(6.dp),tint=MaterialTheme.colorScheme.primary)
                                }
                            } else Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                ShelfMediaThumbnail(item,repository.file(item),Modifier.padding(end=10.dp).size(56.dp).clip(RoundedCornerShape(14.dp)))
                                Column(Modifier.weight(1f)) {
                                    item.text?.let { Text(it.replace('\n',' '),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium) }
                                    if(item.text==null && !item.mimeType.startsWith("image/") && !item.mimeType.startsWith("video/"))
                                        Text(item.category().label,style=MaterialTheme.typography.bodySmall)
                                }
                                if(chosen) Icon(Icons.Outlined.CheckCircle,"Selected",tint=MaterialTheme.colorScheme.primary)
                            }
                        }
                        }
                    }
                }
                if(selected.isNotEmpty()) Row(Modifier.padding(top=8.dp).fillMaxWidth().background(Origin.Ink,RoundedCornerShape(24.dp)).padding(end=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.width(118.dp).height(88.dp).pointerInput(selected) {
                        detectDragGesturesAfterLongPress(onDragStart={haptics.confirm(); onGlobalDrag(view,currentItems.filter {it.id in currentSelected})},onDrag={change,_->change.consume()})
                    },contentAlignment=Alignment.Center) {
                        val stack=items.filter {it.id in selected}.take(3)
                        stack.reversed().forEachIndexed { index,item ->
                            Surface(Modifier.width(92.dp).height(64.dp).graphicsLayer {
                                rotationZ=listOf(-4f,2f,5f)[index%3]; translationY=-index*5*density; shadowElevation=8*density
                            },shape=RoundedCornerShape(14.dp),color=Color.White) {
                                if(item.fileName!=null) ShelfMediaThumbnail(item,repository.file(item),Modifier.fillMaxSize())
                                else Text(item.text.orEmpty(),Modifier.padding(10.dp),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("${selected.size}",Modifier.align(Alignment.TopEnd).padding(4.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(12.dp)).padding(horizontal=8.dp,vertical=3.dp),color=Color.White,fontSize=12.sp)
                    }
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={haptics.select(); onShare(items.filter { it.id in selected },true)}) { Text("WhatsApp",maxLines=1) }
                        Button(onClick={haptics.select(); onShare(items.filter { it.id in selected },false)},
                            colors=ButtonDefaults.buttonColors(containerColor=Color.White.copy(alpha=.14f),contentColor=Color.White)) { Text("Share",maxLines=1) }
                    }
                }
            }
        }
        deleting?.let { item ->
            Surface(Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth().clickable(interactionSource=null,indication=null) {},
                shape=RoundedCornerShape(28.dp),color=Color.White,shadowElevation=16.dp) {
                Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text("Delete saved item?",style=MaterialTheme.typography.titleLarge)
                    Text("This removes the item and its cached file permanently.",style=MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.End) {
                        TextButton(onClick={deleting=null}) { Text("Keep item") }
                        TextButton(onClick={onDelete(item);deleting=null},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) { Text("Delete permanently") }
                    }
                }
            }
        }
    }
}
