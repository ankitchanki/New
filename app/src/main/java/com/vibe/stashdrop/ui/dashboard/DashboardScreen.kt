package com.vibe.stashdrop.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.core.physics.IosSpringSpecs
import com.vibe.stashdrop.core.shaders.AuroraWave
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.floatingdock.ThumbDockIconComposable
import com.vibe.stashdrop.features.stashshelf.*
import com.vibe.stashdrop.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(items: List<StashItemEntity>,settings: StashSettings,shizuku: ShizukuConnectionManager.State,
    running: Boolean,overlayAllowed: Boolean,message: String,clipboardStatus: String,onToggleDock: ()->Unit,onShizuku: ()->Unit,
    onAccessibility: ()->Unit,onSearch: (Boolean)->Unit,onPreference: (String,Boolean)->Unit,onBounce: (Float)->Unit,
    onAssistant: (Boolean)->Unit,onAddText: (String)->Unit,onDelete: (StashItemEntity)->Unit,
    onShare: (List<StashItemEntity>,Boolean)->Unit,onCollect: (StashItemEntity,String)->Unit,
    initialTab: Int = 0) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0,2)) }
    var adding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var collectionItem by remember { mutableStateOf<StashItemEntity?>(null) }
    var collection by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf("All") }
    Scaffold(containerColor=MaterialTheme.colorScheme.background,bottomBar={
        NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
            listOf("Overview" to Icons.Outlined.SpaceDashboard,"Shelf" to Icons.Outlined.Layers,"Controls" to Icons.Outlined.Tune).forEachIndexed { i,(label,icon) ->
                NavigationBarItem(tab==i,{tab=i},icon={Icon(icon,null)},label={Text(label)})
            }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("STASHDROP",style=MaterialTheme.typography.labelLarge,letterSpacing=3.sp)
                Surface(shape=RoundedCornerShape(20.dp),color=if(running) Mint.copy(alpha=.12f) else Ice.copy(alpha=.1f)) {
                    Text(if(running) "●  Shelf active" else "○  Shelf paused",Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelSmall,color=if(running) Mint else Ice)
                }
            }
            when(tab) {
                0 -> {
                    Column(Modifier.padding(top=26.dp,bottom=6.dp)) {
                        Text("A little space.\nFor everything.",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.SemiBold)
                        Text("Catch an idea. Keep it close. Drop it anywhere.",Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge)
                    }
                    Playground(settings.bounce,items.size)
                    Button(onClick=onToggleDock,Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp)) {
                        Icon(if(running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,null)
                        Spacer(Modifier.width(8.dp)); Text(if(running) "Pause floating shelf" else if(overlayAllowed) "Start floating shelf" else "Allow floating shelf")
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        ActionTile("Circle Studio","Select · extract · keep",Icons.Outlined.CropFree,Modifier.weight(1f)) {onSearch(true)}
                        ActionTile("Circle to Search","Google with fallback",Icons.Outlined.AutoAwesome,Modifier.weight(1f)) {onSearch(false)}
                    }
                    SectionTitle("Your essentials","${items.size} saved")
                    if(items.isEmpty()) Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Your next idea belongs here.",style=MaterialTheme.typography.titleMedium)
                            Text("Share into StashDrop from any app, or add a note.",Modifier.padding(top=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick={adding=true}) { Text("＋  Add your first note") }
                        }
                    } else items.take(3).forEach { item -> StashRow(item,{onShare(listOf(item),false)},{onDelete(item)},{collectionItem=item; collection=item.collection}) }
                    StatusCard(shizuku,onShizuku)
                }
                1 -> {
                    Text("Everything,\nwithin reach.",style=MaterialTheme.typography.displaySmall,modifier=Modifier.padding(top=28.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("${items.size} saved items",color=Ice); TextButton(onClick={adding=true}) { Text("＋ Add note") } }
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        (listOf("All")+items.map { it.collection }.distinct()).forEach { name -> FilterChip(selectedCollection==name,{selectedCollection=name},label={Text(name)}) }
                    }
                    val visible=items.filter { selectedCollection=="All" || it.collection==selectedCollection }
                    if(visible.isEmpty()) Text("Nothing here yet. Share text, photos, or files to StashDrop.",Modifier.padding(vertical=40.dp))
                    visible.forEach { item -> StashRow(item,{onShare(listOf(item),false)},{onDelete(item)},{collectionItem=item; collection=item.collection}) }
                }
                else -> {
                    Text("Make it\nyour own.",style=MaterialTheme.typography.displaySmall,modifier=Modifier.padding(top=28.dp))
                    SectionTitle("Connections","Device permissions")
                    StatusCard(shizuku,onShizuku)
                    OutlinedButton(onClick=onAccessibility,Modifier.fillMaxWidth()) { Text("Accessibility screenshot access") }
                    Text("Optional on Android 11+. Capture runs only when you invoke Studio. Android 9–10 can use screen-sharing consent.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    SectionTitle("Gestures","Opt in to each control")
                    SettingSwitch("Bottom touch strips","Uses small touch regions above navigation",settings.gesturesEnabled) {onPreference("bar",it)}
                    Text("Enabled strips consume touches in their region. System home-button interception and pass-through replay are unavailable to normal apps. Strips hide in landscape; immersive visibility depends on OEM insets.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingSwitch("Swipe up","Pull 64 dp or flick to search",settings.gestures.swipe) {onPreference("swipe",it)}
                    SettingSwitch("Corner swipe","Pull inward from either corner",settings.gestures.corners) {onPreference("corners",it)}
                    SettingSwitch("Press and hold","Hold the strip for 400 ms",settings.gestures.hold) {onPreference("hold",it)}
                    SettingSwitch("Double tap","Two taps within 300 ms",settings.gestures.doubleTap) {onPreference("double",it)}
                    SectionTitle("Clipboard","Stored locally")
                    SettingSwitch("Auto-stash copied text","While the floating service is active",settings.autoStash) {onPreference("auto",it)}
                    Text("May save sensitive text, including passwords and one-time codes. Off by default. Background access needs Shizuku and can be restricted by your OS. Delete saved items in Shelf.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(clipboardStatus,style=MaterialTheme.typography.bodySmall,color=Mint)
                    SectionTitle("Motion","Spring response")
                    Slider(settings.bounce,onBounce,valueRange=.5f..1.5f)
                    Playground(settings.bounce,items.size)
                    SectionTitle("Google assistant","Optional Shizuku action")
                    OutlinedButton(onClick={onAssistant(true)},enabled=shizuku.connected,modifier=Modifier.fillMaxWidth()) { Text("Use Google as assistant") }
                    TextButton(onClick={onAssistant(false)},enabled=shizuku.connected) { Text("Restore previous assistant") }
                }
            }
            if(message.isNotEmpty()) Text(message,color=Ice,style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
        }
    }
    if(adding) AlertDialog(onDismissRequest={adding=false},title={Text("Keep a thought")},text={OutlinedTextField(text,{text=it},label={Text("Text or link")},minLines=3)},
        confirmButton={TextButton(enabled=text.isNotBlank(),onClick={onAddText(text); text=""; adding=false}) {Text("Save")}},dismissButton={TextButton(onClick={adding=false}) {Text("Cancel")}})
    collectionItem?.let { item -> AlertDialog(onDismissRequest={collectionItem=null},title={Text("Move to collection")},text={OutlinedTextField(collection,{collection=it},label={Text("Collection name")})},
        confirmButton={TextButton(onClick={onCollect(item,collection);collectionItem=null}) {Text("Move")}},dismissButton={TextButton(onClick={collectionItem=null}) {Text("Cancel")}}) }
}
@Composable private fun SectionTitle(title: String,subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        Text(title,style=MaterialTheme.typography.titleMedium); Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun ActionTile(title: String,subtitle: String,icon: androidx.compose.ui.graphics.vector.ImageVector,modifier: Modifier,onClick: ()->Unit) {
    Surface(onClick=onClick,modifier=modifier.height(138.dp),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Icon(icon,null,tint=Ice)
            Column {Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}
@Composable private fun StatusCard(state: ShizukuConnectionManager.State,onClick: ()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Hub,null,tint=if(state.connected) Mint else Ice,modifier=Modifier.padding(end=12.dp))
            Column(Modifier.weight(1f)) {Text("Shizuku",style=MaterialTheme.typography.titleSmall);Text(state.message,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            TextButton(onClick=onClick) {Text(if(state.granted) "Check" else "Connect")}
        }
    }
}
@Composable private fun SettingSwitch(title: String,subtitle: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.bodyLarge);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked,onChange)
    }
}
@Composable private fun StashRow(item: StashItemEntity,onShare: ()->Unit,onDelete: ()->Unit,onCollect: ()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(if(item.text!=null) Icons.AutoMirrored.Outlined.Notes else Icons.Outlined.Image,null,tint=Ice,modifier=Modifier.padding(end=12.dp))
                Text(item.text ?: item.mimeType,Modifier.weight(1f),maxLines=3,overflow=TextOverflow.Ellipsis)
                IconButton(onClick=onShare) {Icon(Icons.Outlined.IosShare,"Share item")}
                IconButton(onClick=onDelete) {Icon(Icons.Outlined.DeleteOutline,"Delete item")}
            }
            TextButton(onClick=onCollect,contentPadding=PaddingValues(0.dp)) {Text(item.collection,style=MaterialTheme.typography.labelSmall)}
        }
    }
}
@Composable private fun Playground(intensity: Float,count: Int) {
    val scope=rememberCoroutineScope()
    val progress=remember { Animatable(0f) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    var pull by remember { mutableFloatStateOf(0f) }
    val density=LocalDensity.current.density
    val context=androidx.compose.ui.platform.LocalContext.current
    val haptics=remember {TactileHapticEngine(context)}
    val latestIntensity by rememberUpdatedState(intensity)
    Surface(shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().height(230.dp)) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(onDragStart={pull=0f;touch=it;haptics.reset()},onDragEnd={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}},onDragCancel={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}}) {change,amount->
                change.consume();touch=change.position;pull=(pull-amount.y/density).coerceIn(0f,100f);haptics.tension(pull)
                scope.launch {progress.snapTo((pull/72f).coerceIn(0f,1f))}
            }
        },contentAlignment=Alignment.Center) {
            AuroraWave(progress.value,touch,Modifier.fillMaxSize())
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                ThumbDockIconComposable(count,progress.value>.5f,{scope.launch {progress.animateTo(1f,IosSpringSpecs.liquid(intensity));haptics.confirm();progress.animateTo(0f,IosSpringSpecs.liquid(intensity))}}, {},{}, {})
                Text("Motion playground",style=MaterialTheme.typography.titleSmall)
                Text("Pull upward to feel the aurora",Modifier.padding(top=4.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp).size(44.dp,4.dp).background(Color.White.copy(alpha=.28f),RoundedCornerShape(4.dp)))
        }
    }
}
