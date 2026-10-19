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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibe.stashdrop.core.physics.*
import com.vibe.stashdrop.features.dragdrop.IosCardStackDragShadow
import com.vibe.stashdrop.features.stashshelf.*

@Composable
fun IosFanOutStashSheet(items: List<StashItemEntity>, repository: StashRepository, onDismiss: ()->Unit,
    onShare: (List<StashItemEntity>,Boolean)->Unit, onDelete: (StashItemEntity)->Unit,
    onGlobalDrag: (View,List<StashItemEntity>)->Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val currentItems by rememberUpdatedState(items)
    val currentSelected by rememberUpdatedState(selected)
    LaunchedEffect(items) { selected=selected.intersect(items.map { it.id }.toSet()) }
    val view=LocalView.current
    var dragVelocity by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val tracker=remember { androidx.compose.ui.input.pointer.util.VelocityTracker() }
    val inspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val reveal=remember { Animatable(if (inspection) 0f else 300f) }
    LaunchedEffect(Unit) { if (!inspection) reveal.animateTo(0f,IosSpringSpecs.liquid()) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.24f)).clickable(onClick=onDismiss),contentAlignment=Alignment.BottomCenter) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.55f).graphicsLayer { translationY=reveal.value }.clickable(enabled=false) {},
            shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp),color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal=20.dp,vertical=12.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp).background(Color.White.copy(alpha=.2f),RoundedCornerShape(3.dp)))
                Row(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column { Text("Your shelf",style=MaterialTheme.typography.headlineSmall); Text("${items.size} items · hold a card to drag",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary) }
                    IconButton(onClick=onDismiss) { Icon(Icons.Outlined.Close,"Close shelf") }
                }
                if(selected.isNotEmpty()) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.width(118.dp).height(88.dp).pointerInput(selected) {
                        detectDragGesturesAfterLongPress(onDragStart={onGlobalDrag(view,currentItems.filter {it.id in currentSelected})},onDrag={change,_->change.consume()})
                    },contentAlignment=Alignment.Center) {
                        val stack=items.filter {it.id in selected}.take(3)
                        stack.reversed().forEachIndexed { index,item ->
                            Surface(Modifier.width(92.dp).height(64.dp).graphicsLayer {
                                rotationZ=listOf(-4f,2f,5f)[index%3]; translationY=-index*5*density; shadowElevation=8*density
                            },shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                                Text(item.text ?: item.mimeType,Modifier.padding(10.dp),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("${selected.size}",Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(12.dp)).padding(6.dp),color=MaterialTheme.colorScheme.onPrimary)
                    }
                    Column {
                        Button(onClick={onShare(items.filter { it.id in selected },true)}) { Text("WhatsApp") }
                        TextButton(onClick={onShare(items.filter { it.id in selected },false)}) { Text("Share selection") }
                    }
                }
                if(items.isEmpty()) Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center) {
                    Icon(Icons.Outlined.Inventory2,null,tint=MaterialTheme.colorScheme.primary)
                    Text("Keep something close",style=MaterialTheme.typography.titleLarge)
                    Text("Share text or images to StashDrop, or capture a cutout in Circle Studio.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(items,key={_,item->item.id}) { index,item ->
                        val chosen=item.id in selected
                        val angle by animateFloatAsState(if(chosen) listOf(-4f,2f,5f)[index%3] else 0f,IosSpringSpecs.follow(),label="card tilt")
                        val scale by animateFloatAsState(if(chosen) .96f else 1f,IosSpringSpecs.landing(),label="stack scale")
                        Card(modifier=Modifier.fillMaxWidth().graphicsLayer { rotationZ=angle; scaleX=scale; scaleY=scale; cameraDistance=12*density; shadowElevation=if(chosen) 14f else 2f
                            rotationX=MagneticTrajectoryEngine.tilt(-dragVelocity.y)
                            rotationY=MagneticTrajectoryEngine.tilt(dragVelocity.x)
                        }
                            .pointerInput(item.id) { detectDragGesturesAfterLongPress(onDragStart={
                                tracker.resetTracking()
                                val picked=currentItems.filter { it.id in currentSelected }.ifEmpty { listOf(item) }
                                onGlobalDrag(view,picked)
                            },onDragEnd={dragVelocity=androidx.compose.ui.geometry.Offset.Zero},onDragCancel={dragVelocity=androidx.compose.ui.geometry.Offset.Zero},onDrag={change,_->
                                change.consume();tracker.addPosition(change.uptimeMillis,change.position)
                                val velocity=tracker.calculateVelocity()
                                dragVelocity=androidx.compose.ui.geometry.Offset(velocity.x,velocity.y)
                            }) },
                            onClick={ selected=if(chosen) selected-item.id else selected+item.id },
                            colors=CardDefaults.cardColors(containerColor=if(chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(if(item.fileName!=null) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.Notes,null,Modifier.padding(end=12.dp),tint=MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(item.text ?: item.mimeType,maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                                    Text(item.collection,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick={onShare(listOf(item),true)}) { Icon(Icons.AutoMirrored.Outlined.Send,"Send to WhatsApp") }
                                IconButton(onClick={onDelete(item)}) { Icon(Icons.Outlined.DeleteOutline,"Delete item") }
                            }
                        }
                    }
                }
            }
        }
    }
}
