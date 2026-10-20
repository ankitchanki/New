package com.vibe.stashdrop.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
    READY("Google voice service active · eligibility varies"),
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
    val deleteMany: (List<StashItemEntity>)->Unit = { selected -> selected.forEach(delete) },
    val collectMany: (List<StashItemEntity>,String)->Unit = { selected,name -> selected.forEach { collect(it,name) } },
)


@Composable
fun DashboardScreen(items: List<StashItemEntity>, settings: StashSettings, shizuku: ShizukuConnectionManager.State,
    running: Boolean, setup: SetupState, message: String, clipboardStatus: String, actions: DashboardActions,
    initialTab: Int = 0, initialInteractions: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0,2)) }
    var interactions by rememberSaveable { mutableStateOf(initialInteractions) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedCollection by rememberSaveable { mutableStateOf("All") }
    var collectionTargets by remember { mutableStateOf(emptyList<StashItemEntity>()) }
    var collection by rememberSaveable { mutableStateOf("") }
    var deleteTargets by remember { mutableStateOf(emptyList<StashItemEntity>()) }
    var confirmClipboard by rememberSaveable { mutableStateOf(false) }
    var assistantExpanded by rememberSaveable { mutableStateOf(false) }
    val selection = items.filter { it.id in selectedIds }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { if(message.isNotEmpty()) { snackbar.showSnackbar(message); actions.messageShown() } }
    fun moveSelection(targets: List<StashItemEntity>) { collectionTargets=targets; collection=targets.firstOrNull()?.collection.orEmpty() }
    BackHandler(enabled=interactions || selection.isNotEmpty()) {
        if(interactions) interactions=false else selectedIds=emptyList()
    }
    Scaffold(containerColor=MaterialTheme.colorScheme.background,snackbarHost={ SnackbarHost(snackbar) },bottomBar={
        Column {
            if(tab==1 && selection.isNotEmpty()) SelectionBar(selection.size,
                onClear={selectedIds=emptyList()}, onShare={actions.share(selection,false)}, onWhatsApp={actions.share(selection,true)},
                onMove={moveSelection(selection)}, onDelete={deleteTargets=selection})
            HorizontalDivider(color=MaterialTheme.colorScheme.surfaceVariant)
            NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
                listOf("Home" to Icons.Outlined.Home,"Shelf" to Icons.Outlined.Layers,"Settings" to Icons.Outlined.Tune).forEachIndexed { i,(label,icon) ->
                    NavigationBarItem(tab==i,{tab=i;interactions=false;selectedIds=emptyList()},icon={Icon(icon,null)},label={Text(label)})
                }
            }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                if(interactions) IconButton(onClick={interactions=false}) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings") }
                Text(when { interactions -> "INTERACTIONS"; tab==1 -> "YOUR SPACE"; tab==2 -> "MAKE IT YOURS"; else -> "STASHDROP" },
                    Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
                if(tab==0) Surface(shape=RoundedCornerShape(20.dp),color=if(running) Mint.copy(alpha=.12f) else Ice.copy(alpha=.1f)) {
                    Text(if(running) "Bubble on" else "Bubble off",Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.labelSmall,color=if(running) Mint else Ice)
                }
            }
            when {
                tab==0 -> {
                    Headline("Everything within reach.","Catch an idea. Keep it close.")
                    Playground(settings.bounce,items.size)
                    Button(onClick=actions.toggleDock,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp)) {
                        Icon(Icons.Outlined.Layers,null,Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Text(if(running) "Hide floating bubble" else if(setup.overlay) "Show floating bubble" else "Allow floating bubble")
                    }
                    ActionRow("Circle to Search",if(settings.googleCircle) "Google route · eligibility varies" else "Built-in · circle, copy, save",Icons.Outlined.CropFree,actions.search)
                    ActionRow("Setup & connections",if(setup.complete) "Review access and integrations" else "A few permissions, at your pace",Icons.Outlined.Tune) {tab=2}
                    SectionTitle("Recent","${items.size} saved") { TextButton(onClick={tab=1}) { Text("See all") } }
                    if(items.isEmpty()) EmptyCard { adding=true }
                    else items.take(3).forEach { item -> StashRow(item,false,{tab=1;selectedCollection="All";selectedIds=listOf(item.id)},{moveSelection(listOf(item))}) }
                }
                tab==1 -> {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("Your shelf.",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                        Button(onClick={adding=true},shape=RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.Add,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Note") }
                    }
                    Text("${items.size} saved · tap cards to select",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    val collections=listOf("All")+items.map { it.collection }.distinct().filter { it!="All" }
                    val activeCollection=selectedCollection.takeIf { it in collections } ?: "All"
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        collections.forEach { name -> FilterChip(activeCollection==name,{selectedCollection=name;selectedIds=emptyList()},label={Text(name)},shape=RoundedCornerShape(20.dp),border=null,
                            colors=FilterChipDefaults.filterChipColors(containerColor=MaterialTheme.colorScheme.surface,selectedContainerColor=Ice.copy(alpha=.16f),labelColor=MaterialTheme.colorScheme.onSurfaceVariant,selectedLabelColor=Ice)) }
                    }
                    val visible=items.filter { activeCollection=="All" || it.collection==activeCollection }
                    if(visible.isEmpty()) EmptyCard { adding=true }
                    visible.forEach { item -> StashRow(item,item.id in selectedIds,{
                        selectedIds=if(item.id in selectedIds) selectedIds-item.id else selectedIds+item.id
                    },{moveSelection(listOf(item))}) }
                    Text("Share text, images or files to StashDrop from another app's Share menu.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                interactions -> {
                    Headline("Your way.","Gesture strips run with the floating bubble.")
                    Card { SettingSwitch("Gesture strips","Touch areas above the navigation bar",settings.gesturesEnabled) {actions.preference("bar",it)} }
                    Card { SettingSwitch("Swipe up","Pull up 64 dp or flick to search",settings.gestures.swipe) {actions.preference("swipe",it)} }
                    Card { SettingSwitch("Corner swipe","Pull inward from either corner",settings.gestures.corners) {actions.preference("corners",it)} }
                    Card { SettingSwitch("Press and hold","Hold the strip for 400 ms",settings.gestures.hold) {actions.preference("hold",it)} }
                    Card { SettingSwitch("Double tap","Two taps within 300 ms",settings.gestures.doubleTap) {actions.preference("double",it)} }
                    Card { SettingSwitch("Auto-stash copied text","May retain passwords, codes or private text",settings.autoStash) {
                        if(it) confirmClipboard=true else actions.preference("auto",false)
                    } }
                    if(settings.autoStash) Text(clipboardStatus,style=MaterialTheme.typography.bodySmall,color=Mint)
                    Card { SettingSwitch("Haptic feedback","Feel drags, stacks and circles",settings.haptics) {actions.preference("haptics",it)} }
                    Card {
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("Springiness",Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                            Text(java.lang.String.format(java.util.Locale.ROOT,"%.1f×",settings.bounce),color=Ice)
                        }
                        Slider(settings.bounce,actions.bounce,Modifier.padding(horizontal=16.dp),valueRange=.5f..1.5f)
                    }
                }
                else -> {
                    Headline("Settings.","Choose the access you need.")
                    SectionTitle("Setup",if(setup.complete) "Ready" else "Tap to review")
                    if(shizuku.connected && !setup.complete) Button(onClick=actions.setupAll,Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
                        Icon(Icons.Outlined.AutoFixHigh,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Set up with Shizuku")
                    }
                    Card { SetupRow(Icons.Outlined.BubbleChart,"Floating bubble",setup.overlay,"Allow",actions.allowOverlay) }
                    Card { SetupRow(Icons.Outlined.Notifications,"Notifications",setup.notifications,"Allow",actions.allowNotifications) }
                    Card { SetupRow(Icons.Outlined.Hub,"Shizuku",shizuku.connected,if(shizuku.granted) "Check" else "Connect",actions.connectShizuku,
                        statusText=if(shizuku.connected) "Bridge connected" else shizuku.message) }
                    Card { SetupRow(Icons.Outlined.Screenshot,"Screenshot access",setup.accessibility,"Turn on",actions.enableAccessibility,
                        statusText=if(setup.accessibility) "Accessibility enabled" else if(shizuku.connected) "Shizuku or capture consent available" else "Optional · capture consent is also available") }
                    SectionTitle("Search & assistant","Choose a route")
                    Card {
                        SettingSwitch("Use Google Circle to Search",if(settings.googleCircle) setup.google.label else "Built-in capture and on-device OCR",settings.googleCircle) {actions.preference("google_cts",it)}
                        Text("Gemini opens instead? Turn Google mode off.",Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick=actions.search,modifier=Modifier.padding(horizontal=8.dp)) { Icon(Icons.Outlined.CropFree,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Try it now") }
                    }
                    if(setup.assistantSupported) {
                        ActionRow("Digital assistant",when {
                            setup.assistant && setup.google==GoogleState.READY -> "StashDrop gestures · Google voice active"
                            setup.assistant -> "StashDrop selected"
                            else -> "System picker and optional Shizuku"
                        },Icons.Outlined.AssistantPhoto) {assistantExpanded=!assistantExpanded}
                        if(assistantExpanded) Card {
                            Text("Assistant gestures and Google's voice service are separate settings. Google decides native search eligibility.",Modifier.padding(16.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick=actions.chooseAssistant) {Text("Open assistant picker")}
                            if(shizuku.connected) {
                                TextButton(onClick=if(setup.assistant) actions.restoreAssistant else actions.makeAssistant) {Text(if(setup.assistant) "Restore previous" else "Use StashDrop now")}
                                if(setup.assistant && setup.google==GoogleState.NOT_ASSISTANT) TextButton(onClick=actions.linkGoogle) {Text("Connect Google")}
                            } else TextButton(onClick=actions.connectShizuku) {Text("Connect Shizuku")}
                        }
                    }
                    ActionRow("Gestures, clipboard & feel","Customize interactions and motion",Icons.Outlined.Tune) {interactions=true}
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    setup.crash?.let { report -> AlertDialog(onDismissRequest=actions.dismissCrash,title={Text("StashDrop closed unexpectedly")},
        text={Column {
            Text("Review this local report before sharing it.")
            Text(report,Modifier.padding(top=12.dp).heightIn(max=260.dp).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.labelSmall,
                fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace)
        }},confirmButton={TextButton(onClick={actions.copyCrash(report)}) {Text("Copy report")}},dismissButton={TextButton(onClick=actions.dismissCrash) {Text("Dismiss")}}) }
    if(adding) AlertDialog(onDismissRequest={adding=false},title={Text("Keep a thought")},text={OutlinedTextField(text,{text=it},label={Text("Text or link")},minLines=3)},
        confirmButton={TextButton(enabled=text.isNotBlank(),onClick={actions.addText(text);text="";adding=false}) {Text("Save note")}},dismissButton={TextButton(onClick={adding=false}) {Text("Cancel")}})
    if(collectionTargets.isNotEmpty()) AlertDialog(onDismissRequest={collectionTargets=emptyList()},title={Text("Move to collection")},
        text={OutlinedTextField(collection,{collection=it.take(80)},label={Text("Collection name")},supportingText={Text("Leave blank for Inbox")},singleLine=true)},
        confirmButton={TextButton(onClick={actions.collectMany(collectionTargets,collection);collectionTargets=emptyList();selectedIds=emptyList()}) {Text("Move selected")}},
        dismissButton={TextButton(onClick={collectionTargets=emptyList()}) {Text("Cancel")}})
    if(deleteTargets.isNotEmpty()) AlertDialog(onDismissRequest={deleteTargets=emptyList()},title={Text("Delete ${deleteTargets.size} saved item(s)?")},
        text={Text("The selected items and their cached files will be permanently removed. There is no trash or restore.")},
        confirmButton={TextButton(onClick={actions.deleteMany(deleteTargets);deleteTargets=emptyList();selectedIds=emptyList()}) {Text("Delete permanently")}},
        dismissButton={TextButton(onClick={deleteTargets=emptyList()}) {Text("Keep items")}})
    if(confirmClipboard) AlertDialog(onDismissRequest={confirmClipboard=false},title={Text("Save copied text?")},
        text={Text("Auto-stash can retain passwords, codes and private text on your shelf. Turning it off later does not remove earlier saved items.")},
        confirmButton={TextButton(onClick={actions.preference("auto",true);confirmClipboard=false}) {Text("Enable auto-stash")}},
        dismissButton={TextButton(onClick={confirmClipboard=false}) {Text("Keep it off")}})
}

@Composable private fun Headline(title: String,subtitle: String) {
    Column(Modifier.padding(top=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
        Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable private fun SectionTitle(title: String,subtitle: String,trailing: @Composable ()->Unit={}) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text("  ·  $subtitle",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        trailing()
    }
}
@Composable private fun Card(content: @Composable ColumnScope.()->Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {Column(Modifier.fillMaxWidth().padding(vertical=4.dp),content=content)}
}
@Composable private fun ActionRow(title: String,subtitle: String,icon: ImageVector,onClick: ()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().heightIn(min=76.dp).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(icon,null,Modifier.size(26.dp),tint=Ice)
            Column(Modifier.weight(1f).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleSmall)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight,null,Modifier.size(20.dp),tint=Ice)
        }
    }
}
@Composable private fun SetupRow(icon: ImageVector,title: String,done: Boolean,action: String,onClick: ()->Unit,statusText: String=if(done) "On" else "Not enabled") {
    Row(Modifier.fillMaxWidth().heightIn(min=68.dp).padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=if(done) Mint else Ice,modifier=Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,style=MaterialTheme.typography.bodyMedium)
            Text(statusText,style=MaterialTheme.typography.bodySmall,color=if(done) Mint else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(done) Icon(Icons.Outlined.CheckCircle,"Enabled",tint=Mint)
        else TextButton(onClick=onClick) {Text(action)}
    }
}
@Composable private fun SettingSwitch(title: String,subtitle: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked,role=Role.Switch,onValueChange=onChange).heightIn(min=68.dp).padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.bodyMedium)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked,onCheckedChange=null)
    }
}
@Composable private fun EmptyCard(onAdd: ()->Unit) {
    Card {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Nothing saved yet",style=MaterialTheme.typography.titleMedium)
            Text("Share into StashDrop or add your first note.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick=onAdd) {Text("Add note")}
        }
    }
}
@Composable private fun StashRow(item: StashItemEntity,chosen: Boolean,onSelect: ()->Unit,onCollect: ()->Unit) {
    val isLink=item.text?.let {it.startsWith("https://") || it.startsWith("http://")}==true
    val title=item.text?.replace('\n',' ') ?: if(item.mimeType.startsWith("image/")) "Saved image" else "Saved file"
    Surface(onClick=onSelect,modifier=Modifier.fillMaxWidth().semantics {selected=chosen},shape=RoundedCornerShape(20.dp),
        color=if(chosen) Ice.copy(alpha=.16f) else MaterialTheme.colorScheme.surface,
        border=if(chosen) BorderStroke(1.dp,Ice) else null) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center) {
                Icon(when {chosen -> Icons.Outlined.Check;isLink -> Icons.Outlined.Link;item.fileName==null -> Icons.AutoMirrored.Outlined.Notes;item.mimeType.startsWith("image/") -> Icons.Outlined.Image;else -> Icons.AutoMirrored.Outlined.InsertDriveFile},null,tint=Ice,modifier=Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                Text(title,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                Text(item.collection,Modifier.clickable(role=Role.Button,onClick=onCollect).heightIn(min=32.dp).padding(vertical=8.dp),color=Ice,style=MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=Ice)
        }
    }
}
@Composable private fun SelectionBar(count: Int,onClear: ()->Unit,onShare: ()->Unit,onWhatsApp: ()->Unit,onMove: ()->Unit,onDelete: ()->Unit) {
    var more by remember {mutableStateOf(false)}
    Surface(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onClear) {Icon(Icons.Outlined.Close,"Clear selection")}
            Text("$count selected",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
            IconButton(onClick=onShare) {Icon(Icons.Outlined.IosShare,"Share selected")}
            IconButton(onClick=onWhatsApp) {Icon(Icons.AutoMirrored.Outlined.Send,"Send selected to WhatsApp")}
            Box {
                IconButton(onClick={more=true}) {Icon(Icons.Outlined.MoreHoriz,"More selection actions")}
                DropdownMenu(more,{more=false}) {
                    DropdownMenuItem(text={Text("Move to collection")},onClick={more=false;onMove()},leadingIcon={Icon(Icons.Outlined.Folder,null)})
                    DropdownMenuItem(text={Text("Delete selected")},onClick={more=false;onDelete()},leadingIcon={Icon(Icons.Outlined.DeleteOutline,null)})
                }
            }
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
    Surface(shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().height(210.dp)) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(onDragStart={pull=0f;touch=it;haptics.reset()},onDragEnd={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}},onDragCancel={scope.launch {progress.animateTo(0f,IosSpringSpecs.liquid(latestIntensity))}}) {change,amount->
                change.consume();touch=change.position;pull=(pull-amount.y/density).coerceIn(0f,100f);haptics.tension(pull)
                scope.launch {progress.snapTo((pull/72f).coerceIn(0f,1f))}
            }
        },contentAlignment=Alignment.Center) {
            AuroraWave(progress.value,touch,Modifier.fillMaxSize())
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                ThumbDockIconComposable(count,progress.value>.5f,{scope.launch {progress.animateTo(1f,IosSpringSpecs.liquid(intensity));haptics.confirm();progress.animateTo(0f,IosSpringSpecs.liquid(intensity))}}, {},{}, {})
                Text("A little space. For everything.",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp).size(44.dp,4.dp).background(Color.White.copy(alpha=.28f),RoundedCornerShape(4.dp)))
        }
    }
}
