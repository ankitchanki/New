package com.vibe.stashdrop.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
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

/** What the device has granted StashDrop; each missing item can be fixed from the Settings tab. */
data class SetupState(val overlay: Boolean = false, val notifications: Boolean = true, val accessibility: Boolean = false,
    val assistant: Boolean = false, val assistantSupported: Boolean = true, val google: GoogleState = GoogleState.READY,
    val crash: String? = null) {
    val complete get() = overlay && notifications && accessibility
}
/** Whether Google's Circle to Search can be opened from StashDrop. */
enum class GoogleState(val label: String) {
    READY("Google is your digital assistant · ready"),
    NOT_ASSISTANT("Google's assistant service is off"),
    MISSING("Google app is not installed"),
}

class DashboardActions(
    val toggleDock: ()->Unit = {}, val connectShizuku: ()->Unit = {}, val search: ()->Unit = {},
    val preference: (String,Boolean)->Unit = {_,_->}, val bounce: (Float)->Unit = {},
    val addText: (String)->Unit = {}, val delete: (StashItemEntity)->Unit = {},
    val share: (List<StashItemEntity>,Boolean)->Unit = {_,_->}, val collect: (StashItemEntity,String)->Unit = {_,_->},
    val allowOverlay: ()->Unit = {}, val allowNotifications: ()->Unit = {}, val enableAccessibility: ()->Unit = {},
    val makeAssistant: ()->Unit = {}, val restoreAssistant: ()->Unit = {}, val setupAll: ()->Unit = {},
    val chooseAssistant: ()->Unit = {}, val linkGoogle: ()->Unit = {}, val copyCrash: (String)->Unit = {}, val dismissCrash: ()->Unit = {},
    val messageShown: ()->Unit = {},
)

@Composable
fun DashboardScreen(items: List<StashItemEntity>, settings: StashSettings, shizuku: ShizukuConnectionManager.State,
    running: Boolean, setup: SetupState, message: String, clipboardStatus: String, actions: DashboardActions,
    initialTab: Int = 0) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0,2)) }
    var adding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var collectionItem by remember { mutableStateOf<StashItemEntity?>(null) }
    var collection by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf("All") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { if(message.isNotEmpty()) { snackbar.showSnackbar(message); actions.messageShown() } }
    Scaffold(containerColor=MaterialTheme.colorScheme.background,snackbarHost={ SnackbarHost(snackbar) },bottomBar={
        NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
            listOf("Home" to Icons.Outlined.SpaceDashboard,"Shelf" to Icons.Outlined.Layers,"Settings" to Icons.Outlined.Tune).forEachIndexed { i,(label,icon) ->
                NavigationBarItem(tab==i,{tab=i},icon={Icon(icon,null)},label={Text(label,maxLines=1)})
            }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("STASHDROP",style=MaterialTheme.typography.labelLarge,letterSpacing=3.sp,maxLines=1)
                Surface(shape=RoundedCornerShape(20.dp),color=if(running) Mint.copy(alpha=.12f) else Ice.copy(alpha=.1f)) {
                    Text(if(running) "●  Bubble on" else "○  Bubble off",Modifier.padding(horizontal=12.dp,vertical=6.dp),style=MaterialTheme.typography.labelSmall,color=if(running) Mint else Ice,maxLines=1)
                }
            }
            when(tab) {
                0 -> {
                    Headline("Everything within reach","Catch an idea, keep it close, drop it anywhere")
                    Playground(settings.bounce,items.size)
                    Button(onClick=actions.toggleDock,Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(18.dp)) {
                        Icon(if(running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,null)
                        Spacer(Modifier.width(8.dp)); Text(if(running) "Hide floating bubble" else if(setup.overlay) "Show floating bubble" else "Allow floating bubble",maxLines=1)
                    }
                    ActionRow("Circle to Search","Hold the bubble or swipe up",Icons.Outlined.CropFree,actions.search)
                    if(!setup.complete) ActionRow("Finish setup","A few one-tap permissions",Icons.Outlined.TaskAlt) { tab=2 }
                    SectionTitle("Recent","${items.size} saved") { if(items.size>3) TextButton(onClick={tab=1}) { Text("See all",maxLines=1) } }
                    if(items.isEmpty()) EmptyCard { adding=true }
                    else items.take(3).forEach { item -> StashRow(item,{actions.share(listOf(item),false)},{actions.delete(item)},{collectionItem=item; collection=item.collection}) }
                }
                1 -> {
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("Your shelf",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold,maxLines=1)
                        FilledTonalButton(onClick={adding=true}) { Icon(Icons.Outlined.Add,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Note",maxLines=1) }
                    }
                    val collections=listOf("All")+items.map { it.collection }.distinct()
                    if(selectedCollection !in collections) selectedCollection="All"
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        collections.forEach { name -> FilterChip(selectedCollection==name,{selectedCollection=name},label={Text(name,maxLines=1)}) }
                    }
                    val visible=items.filter { selectedCollection=="All" || it.collection==selectedCollection }
                    if(visible.isEmpty()) EmptyCard { adding=true }
                    visible.forEach { item -> StashRow(item,{actions.share(listOf(item),false)},{actions.delete(item)},{collectionItem=item; collection=item.collection}) }
                }
                else -> {
                    Headline("Settings","Change everything here, no system menus needed")
                    SectionTitle("Setup",if(setup.complete) "All set" else "Tap to fix")
                    if(shizuku.connected && !setup.complete) Button(onClick=actions.setupAll,Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
                        Icon(Icons.Outlined.AutoFixHigh,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Set up everything with Shizuku",maxLines=1)
                    }
                    Card {
                        SetupRow(Icons.Outlined.BubbleChart,"Floating bubble",setup.overlay,"Allow",actions.allowOverlay)
                        SetupRow(Icons.Outlined.Notifications,"Notifications",setup.notifications,"Allow",actions.allowNotifications)
                        SetupRow(Icons.Outlined.Hub,"Shizuku",shizuku.connected,if(shizuku.granted) "Check" else "Connect",actions.connectShizuku,statusText=if(shizuku.connected) "Connected" else shizuku.message)
                        SetupRow(Icons.Outlined.Screenshot,"Screenshot access",setup.accessibility,"Turn on",actions.enableAccessibility)
                    }
                    SectionTitle("Circle to Search","Bubble, swipe and notification")
                    Card {
                        SettingSwitch("Use Google Circle to Search",if(settings.googleCircle) setup.google.label else "Off · StashDrop's own circle screen",settings.googleCircle) {actions.preference("google_cts",it)}
                        Text(if(settings.googleCircle) "Gemini opens instead? Turn this off" else "Circle, then search with Google, Lens, copy or stash",
                            Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick=actions.search) { Icon(Icons.Outlined.CropFree,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Try it now",maxLines=1) }
                            if(settings.googleCircle && setup.google==GoogleState.NOT_ASSISTANT) TextButton(onClick=if(setup.assistant) actions.linkGoogle else actions.chooseAssistant) {
                                Text(if(setup.assistant) "Connect Google" else "Choose Google",maxLines=1)
                            }
                        }
                    }
                    if(setup.assistantSupported) {
                        SectionTitle("Digital assistant","Pick it by hand in system settings")
                        Card {
                            SetupRow(Icons.Outlined.AssistantPhoto,"StashDrop as assistant",setup.assistant,"Choose",actions.chooseAssistant,
                                statusText=when {
                                    setup.assistant && setup.google==GoogleState.READY -> "Corner swipe → Google Circle to Search"
                                    setup.assistant -> "Corner swipe → StashDrop circle"
                                    setup.google==GoogleState.READY -> "Google is the assistant"
                                    else -> "Another app is the assistant"
                                })
                            if(setup.assistant && setup.google==GoogleState.NOT_ASSISTANT) SetupRow(Icons.Outlined.Link,"Google Circle to Search",false,
                                if(shizuku.connected) "Connect" else "Shizuku",if(shizuku.connected) actions.linkGoogle else actions.connectShizuku,
                                statusText="Off while StashDrop is assistant")
                            Text("Google: its Circle to Search · StashDrop: own circle",
                                Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick=actions.chooseAssistant) { Text("Open assistant picker",maxLines=1) }
                                if(shizuku.connected) TextButton(onClick=if(setup.assistant) actions.restoreAssistant else actions.makeAssistant) {
                                    Text(if(setup.assistant) "Restore previous" else "Use StashDrop now",maxLines=1)
                                }
                            }
                        }
                    }
                    SectionTitle("Gestures","Bottom-edge touch strips")
                    Card {
                        SettingSwitch("Gesture strips","Touch areas above the nav bar",settings.gesturesEnabled) {actions.preference("bar",it)}
                        SettingSwitch("Swipe up","Pull up 64 dp or flick to search",settings.gestures.swipe) {actions.preference("swipe",it)}
                        SettingSwitch("Corner swipe","Pull inward from either corner",settings.gestures.corners) {actions.preference("corners",it)}
                        SettingSwitch("Press and hold","Hold the strip for 400 ms",settings.gestures.hold) {actions.preference("hold",it)}
                        SettingSwitch("Double tap","Two taps within 300 ms",settings.gestures.doubleTap) {actions.preference("double",it)}
                    }
                    SectionTitle("Clipboard","Stored only on this phone")
                    Card {
                        SettingSwitch("Auto-stash copied text","May include passwords or codes",settings.autoStash) {actions.preference("auto",it)}
                    }
                    if(settings.autoStash) Text(clipboardStatus,style=MaterialTheme.typography.bodySmall,color=Mint,maxLines=1,overflow=TextOverflow.Ellipsis)
                    SectionTitle("Feel","Touch and motion")
                    Card {
                        SettingSwitch("Haptic feedback","Feel drags, stacks and circles",settings.haptics) {actions.preference("haptics",it)}
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("Springiness",style=MaterialTheme.typography.bodyLarge,maxLines=1)
                            Slider(settings.bounce,actions.bounce,Modifier.padding(start=16.dp).weight(1f),valueRange=.5f..1.5f)
                        }
                    }
                    Playground(settings.bounce,items.size)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    setup.crash?.let { report -> AlertDialog(onDismissRequest=actions.dismissCrash,title={Text("StashDrop closed unexpectedly")},
        text={ Column {
            Text("Copy this report and send it so the exact problem can be fixed.",style=MaterialTheme.typography.bodyMedium)
            Text(report,Modifier.padding(top=12.dp).heightIn(max=260.dp).verticalScroll(rememberScrollState()),
                style=MaterialTheme.typography.labelSmall,fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace)
        } },
        confirmButton={TextButton(onClick={actions.copyCrash(report)}) {Text("Copy report")}},dismissButton={TextButton(onClick=actions.dismissCrash) {Text("Dismiss")}}) }
    if(adding) AlertDialog(onDismissRequest={adding=false; text=""},title={Text("Keep a thought")},text={OutlinedTextField(text,{text=it},label={Text("Text or link")},minLines=3)},
        confirmButton={TextButton(enabled=text.isNotBlank(),onClick={actions.addText(text); text=""; adding=false}) {Text("Save")}},dismissButton={TextButton(onClick={adding=false; text=""}) {Text("Cancel")}})
    collectionItem?.let { item -> AlertDialog(onDismissRequest={collectionItem=null},title={Text("Move to collection")},text={OutlinedTextField(collection,{collection=it},label={Text("Collection name")},singleLine=true)},
        confirmButton={TextButton(onClick={actions.collect(item,collection);collectionItem=null}) {Text("Move")}},dismissButton={TextButton(onClick={collectionItem=null}) {Text("Cancel")}}) }
}
@Composable private fun Headline(title: String,subtitle: String) {
    Column(Modifier.padding(top=8.dp)) {
        Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(subtitle,Modifier.padding(top=4.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
@Composable private fun SectionTitle(title: String,subtitle: String,trailing: @Composable ()->Unit={}) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,style=MaterialTheme.typography.titleMedium,maxLines=1)
        Text("  ·  $subtitle",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        trailing()
    }
}
@Composable private fun Card(content: @Composable ColumnScope.()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(vertical=6.dp),content=content) }
}
@Composable private fun ActionRow(title: String,subtitle: String,icon: ImageVector,onClick: ()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Ice.copy(alpha=.12f),CircleShape),contentAlignment=Alignment.Center) { Icon(icon,null,tint=Ice) }
            Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
                Text(title,style=MaterialTheme.typography.titleSmall,maxLines=1)
                Text(subtitle,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun SetupRow(icon: ImageVector,title: String,done: Boolean,action: String,onClick: ()->Unit,statusText: String=if(done) "On" else "Off") {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=if(done) Mint else Ice)
        Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
            Text(title,style=MaterialTheme.typography.bodyLarge,maxLines=1)
            Text(statusText,style=MaterialTheme.typography.labelMedium,color=if(done) Mint else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(done) Icon(Icons.Outlined.CheckCircle,"Done",tint=Mint)
        else FilledTonalButton(onClick=onClick,contentPadding=PaddingValues(horizontal=14.dp)) { Text(action,maxLines=1) }
    }
}
@Composable private fun SettingSwitch(title: String,subtitle: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=12.dp)) {
            Text(title,style=MaterialTheme.typography.bodyLarge,maxLines=1)
            Text(subtitle,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Switch(checked,onChange)
    }
}
@Composable private fun EmptyCard(onAdd: ()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nothing saved yet",style=MaterialTheme.typography.titleSmall,maxLines=1)
                Text("Share into StashDrop or add a note",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            FilledTonalButton(onClick=onAdd) { Text("Add note",maxLines=1) }
        }
    }
}
@Composable private fun StashRow(item: StashItemEntity,onShare: ()->Unit,onDelete: ()->Unit,onCollect: ()->Unit) {
    Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(start=16.dp,top=6.dp,bottom=6.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(if(item.text!=null) Icons.AutoMirrored.Outlined.Notes else Icons.Outlined.Image,null,tint=Ice)
            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                Text(item.text?.replace('\n',' ') ?: "Image",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                Text(item.collection,Modifier.clickable(onClick=onCollect),style=MaterialTheme.typography.labelSmall,color=Ice,maxLines=1)
            }
            IconButton(onClick=onShare) {Icon(Icons.Outlined.IosShare,"Share item")}
            IconButton(onClick=onDelete) {Icon(Icons.Outlined.DeleteOutline,"Delete item")}
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
    Surface(shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().height(200.dp)) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(onDragStart={pull=0f;touch=it;haptics.reset()},onDragEnd={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}},onDragCancel={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}}) {change,amount->
                change.consume();touch=change.position;pull=(pull-amount.y/density).coerceIn(0f,100f);haptics.tension(pull)
                scope.launch {progress.snapTo((pull/72f).coerceIn(0f,1f))}
            }
        },contentAlignment=Alignment.Center) {
            AuroraWave(progress.value,touch,Modifier.fillMaxSize())
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                ThumbDockIconComposable(count,progress.value>.5f,{scope.launch {progress.animateTo(1f,IosSpringSpecs.liquid(intensity));haptics.confirm();progress.animateTo(0f,IosSpringSpecs.liquid(intensity))}}, {},{}, {})
                Text("Pull up to feel the aurora",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp).size(44.dp,4.dp).background(Color.White.copy(alpha=.28f),RoundedCornerShape(4.dp)))
        }
    }
}
