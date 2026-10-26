package com.vibe.stashdrop.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.stashshelf.*
import com.vibe.stashdrop.ui.shelf.*
import com.vibe.stashdrop.ui.theme.*
import java.io.File

/** What the device has granted StashDrop; each missing item can be fixed from the Settings tab. */
data class SetupState(val overlay: Boolean = false, val notifications: Boolean = true,
    val assistant: Boolean = false, val assistantSupported: Boolean = true, val google: GoogleState = GoogleState.READY,
    val crash: String? = null) {
    val complete get() = overlay && notifications
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
    val allowOverlay: ()->Unit = {}, val allowNotifications: ()->Unit = {},
    val makeAssistant: ()->Unit = {}, val restoreAssistant: ()->Unit = {}, val setupAll: ()->Unit = {},
    val chooseAssistant: ()->Unit = {}, val linkGoogle: ()->Unit = {}, val copyCrash: (String)->Unit = {}, val dismissCrash: ()->Unit = {},
    val messageShown: ()->Unit = {},
    val deleteMany: (List<StashItemEntity>)->Unit = { selected -> selected.forEach(delete) },
    val collectMany: (List<StashItemEntity>,String)->Unit = { selected,name -> selected.forEach { collect(it,name) } },
    val accent: (Int)->Unit = {},
)

@Composable
fun DashboardScreen(items: List<StashItemEntity>, settings: StashSettings, shizuku: ShizukuConnectionManager.State,
    running: Boolean, setup: SetupState, message: String, clipboardStatus: String, actions: DashboardActions,
    initialTab: Int = 0, initialInteractions: Boolean = false, mediaFile: (StashItemEntity)->File? = { null }) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0,2)) }
    var interactions by rememberSaveable { mutableStateOf(initialInteractions) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedType by rememberSaveable { mutableStateOf(ShelfCategory.ALL) }
    var selectedCollection by rememberSaveable { mutableStateOf("All") }
    var collectionTargets by remember { mutableStateOf(emptyList<StashItemEntity>()) }
    var collection by rememberSaveable { mutableStateOf("") }
    var deleteTargets by remember { mutableStateOf(emptyList<StashItemEntity>()) }
    var confirmClipboard by rememberSaveable { mutableStateOf(false) }
    var assistantExpanded by rememberSaveable { mutableStateOf(false) }
    val selection = items.filter { it.id in selectedIds }
    val snackbar = remember { SnackbarHostState() }
    val pageScroll = rememberScrollState()
    LaunchedEffect(tab,interactions) { pageScroll.scrollTo(0) }
    LaunchedEffect(message) { if(message.isNotEmpty()) { snackbar.showSnackbar(message); actions.messageShown() } }
    fun moveSelection(targets: List<StashItemEntity>) { collectionTargets=targets; collection=targets.firstOrNull()?.collection.orEmpty() }
    fun autoStash(on: Boolean) { if(on) confirmClipboard=true else actions.preference("auto",false) }
    fun openItem(item: StashItemEntity) { tab=1; query=""; selectedCollection="All"; selectedType=ShelfCategory.ALL; selectedIds=listOf(item.id) }
    BackHandler(enabled=interactions || selection.isNotEmpty()) {
        if(interactions) interactions=false else selectedIds=emptyList()
    }
    val selecting = tab==1 && selection.isNotEmpty()
    // The bottom bars sit below the scroll viewport so scrolled-to content is never hidden behind them.
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(pageScroll).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal=18.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(16.dp))
            when {
                tab==0 -> HomeTab(items,settings,running,setup,clipboardStatus,actions,mediaFile,
                    onOpenShelf={tab=1}, onOpenSettings={tab=2}, onOpenItem=::openItem, onAutoStash=::autoStash, onAdd={adding=true})
                tab==1 -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("Shelf",Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge,fontSize=30.sp,maxLines=1)
                        Surface(onClick={adding=true},shape=CircleShape,color=Origin.Ink) {
                            Row(Modifier.heightIn(min=44.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Add,null,Modifier.size(18.dp),tint=Color.White); Spacer(Modifier.width(6.dp))
                                Text("Note",color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                            }
                        }
                    }
                    SearchField(query) { query=it; selectedIds=emptyList() }
                    val collections=listOf("All")+items.map { it.collection }.distinct().filter { it!="All" && it!="Inbox" && it!="Images" }
                    val activeCollection=selectedCollection.takeIf { it in collections } ?: "All"
                    if(collections.size>1) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        collections.forEach { name -> OriginChip(name,activeCollection==name) { selectedCollection=name; selectedIds=emptyList() } }
                    }
                    ShelfTypeFilters(selectedType) { selectedType=it; selectedIds=emptyList() }
                    val visible=items.filter { (activeCollection=="All" || it.collection==activeCollection) && selectedType.matches(it) && it.matches(query) }
                    Text("${items.size} saved · tap cards to select",Modifier.padding(horizontal=4.dp),style=MaterialTheme.typography.bodySmall,color=Origin.Muted)
                    if(visible.isEmpty()) Text(when {
                        items.isEmpty() -> "Share an item or add a note to get started."
                        query.isNotBlank() -> "Nothing matches “${query.trim()}”."
                        else -> "No ${selectedType.label.lowercase()} in this collection."
                    },Modifier.padding(horizontal=4.dp,vertical=20.dp),color=Origin.Muted)
                    ShelfGrid(visible,selectedIds,mediaFile,
                        onSelect={ item -> selectedIds=if(item.id in selectedIds) selectedIds-item.id else selectedIds+item.id },
                        onCollect={ moveSelection(listOf(it)) }, onDeleteRequest={ deleteTargets=listOf(it) })
                    Text("Share text, images or files to StashDrop from another app's Share menu.",Modifier.padding(horizontal=4.dp),
                        style=MaterialTheme.typography.bodySmall,color=Origin.Muted)
                }
                interactions -> InteractionsTab(settings,clipboardStatus,actions,onBack={interactions=false},onAutoStash=::autoStash)
                else -> SettingsTab(shizuku,setup,actions,assistantExpanded,onToggleAssistant={assistantExpanded=!assistantExpanded},onOpenInteractions={interactions=true})
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(top=6.dp,bottom=14.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            SnackbarHost(snackbar)
            if(selecting) SelectionBar(selection.size,
                onClear={selectedIds=emptyList()}, onShare={actions.share(selection,false)}, onWhatsApp={actions.share(selection,true)},
                onMove={moveSelection(selection)}, onDelete={deleteTargets=selection})
            PillNavigation(tab) { tab=it; interactions=false; selectedIds=emptyList() }
        }
    }
    setup.crash?.let { report -> AlertDialog(onDismissRequest=actions.dismissCrash,title={Text("StashDrop closed unexpectedly")},
        text={Column {
            Text("Review this local report before sharing it.")
            Text(report,Modifier.padding(top=12.dp).heightIn(max=260.dp).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.labelSmall,
                fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace)
        }},confirmButton={TextButton(onClick={actions.copyCrash(report)}) {Text("Copy report")}},dismissButton={TextButton(onClick=actions.dismissCrash) {Text("Dismiss")}}) }
    if(adding) AlertDialog(onDismissRequest={adding=false},title={Text("Keep a thought")},text={OutlinedTextField(text,{text=it},label={Text("Text or link")},minLines=3,shape=RoundedCornerShape(18.dp))},
        confirmButton={TextButton(enabled=text.isNotBlank(),onClick={actions.addText(text);text="";adding=false}) {Text("Save note")}},dismissButton={TextButton(onClick={adding=false}) {Text("Cancel")}})
    if(collectionTargets.isNotEmpty()) AlertDialog(onDismissRequest={collectionTargets=emptyList()},title={Text("Move to collection")},
        text={OutlinedTextField(collection,{collection=it.take(80)},label={Text("Collection name")},supportingText={Text("Leave blank for Inbox")},singleLine=true,shape=RoundedCornerShape(18.dp))},
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

private fun StashItemEntity.matches(query: String): Boolean {
    val q=query.trim(); if(q.isEmpty()) return true
    return text.orEmpty().contains(q,true) || collection.contains(q,true) || fileName.orEmpty().contains(q,true) || category().label.contains(q,true)
}
private fun StashItemEntity.isMedia() = fileName!=null && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))

// ---------- Home ----------

@Composable private fun HomeTab(items: List<StashItemEntity>, settings: StashSettings, running: Boolean, setup: SetupState, clipboardStatus: String,
    actions: DashboardActions, mediaFile: (StashItemEntity)->File?, onOpenShelf: ()->Unit, onOpenSettings: ()->Unit,
    onOpenItem: (StashItemEntity)->Unit, onAutoStash: (Boolean)->Unit, onAdd: ()->Unit) {
    val accent=MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("StashDrop",Modifier.weight(1f),fontFamily=Outfit,fontWeight=FontWeight.Bold,fontSize=20.sp)
        Box(Modifier.size(44.dp).shadow(6.dp,CircleShape,ambientColor=Origin.Ink.copy(alpha=.08f),spotColor=Origin.Ink.copy(alpha=.08f))
            .clip(CircleShape).background(Origin.Card).clickable(onClickLabel="Open settings",role=Role.Button,onClick=onOpenSettings)
            .semantics { contentDescription="Open settings" },contentAlignment=Alignment.Center) {
            Icon(Icons.Outlined.Settings,null,Modifier.size(20.dp),tint=Origin.Ink)
        }
    }
    Column(Modifier.padding(start=4.dp,end=4.dp,top=8.dp,bottom=4.dp)) {
        Text("Welcome back",fontSize=14.sp,color=Origin.Muted)
        Text("Everything within reach",style=MaterialTheme.typography.headlineLarge,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-.5).sp)
    }
    // Circle to Search hero widget.
    Box(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(30.dp)).background(accent)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha=.16f),radius=67.dp.toPx(),center=Offset(size.width-49.dp.toPx(),49.dp.toPx()),style=Stroke(18.dp.toPx()))
            drawCircle(Color.White.copy(alpha=.9f),radius=27.dp.toPx(),center=Offset(size.width-61.dp.toPx(),61.dp.toPx()),
                style=Stroke(3.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(),6.dp.toPx()))))
        }
        Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.padding(end=96.dp)) {
                Text("Circle to Search",color=Color.White,style=MaterialTheme.typography.titleLarge,fontSize=22.sp,maxLines=1)
                Text("Google only · hold the edge handle",color=Color.White.copy(alpha=.88f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Surface(onClick=actions.search,shape=CircleShape,color=Color.White) {
                Text("Try it now",Modifier.heightIn(min=40.dp).wrapContentHeight().padding(horizontal=18.dp),color=Origin.Ink,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
            }
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).height(150.dp).clip(RoundedCornerShape(28.dp)).background(Origin.Card)
            .toggleable(running,role=Role.Switch) { actions.toggleDock() }.padding(18.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Layers,MaterialTheme.colorScheme.primaryContainer,accent,corner=18.dp)
                OriginSwitch(running)
            }
            Column {
                Text("Edge shelf",style=MaterialTheme.typography.titleMedium,fontSize=17.sp,maxLines=1)
                Text(when { running -> "On · drag to X to hide"; !setup.overlay -> "Needs permission"; else -> "Off · tap to show" },
                    fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.weight(1f).height(150.dp).clip(RoundedCornerShape(28.dp)).background(Origin.Card)
            .clickable(onClickLabel="Open shelf",onClick=onOpenShelf).padding(18.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Text("ON YOUR SHELF",fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold,color=Origin.Muted,letterSpacing=.6.sp,maxLines=1)
            Column {
                Text("${items.size}",fontFamily=Outfit,fontWeight=FontWeight.Bold,fontSize=52.sp,lineHeight=54.sp,letterSpacing=(-1.5).sp,maxLines=1)
                val collections=items.map { it.collection }.distinct().size
                Text("items · $collections ${if(collections==1) "collection" else "collections"}",fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=1)
            }
        }
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Origin.Card).toggleable(settings.autoStash,role=Role.Switch,onValueChange=onAutoStash)
        .padding(horizontal=18.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        IconBadge(Icons.Outlined.ContentPaste,Origin.OrangeSoft,Origin.Orange,size=40.dp,corner=20.dp)
        Column(Modifier.weight(1f)) {
            Text("Auto-stash clipboard",style=MaterialTheme.typography.titleMedium,fontSize=16.sp,maxLines=1)
            Text(if(settings.autoStash) clipboardStatus else "Off · may include passwords",fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        OriginSwitch(settings.autoStash)
    }
    if(!setup.complete) NavRow(Icons.Outlined.TaskAlt,MaterialTheme.colorScheme.primaryContainer,accent,"Finish setup","A few one-tap permissions",card=true,onClick=onOpenSettings)
    Row(Modifier.fillMaxWidth().padding(start=4.dp,top=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("Recent",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontSize=18.sp)
        if(items.isNotEmpty()) TextButton(onClick=onOpenShelf) { Text("See all",fontWeight=FontWeight.SemiBold) }
    }
    if(items.isEmpty()) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Origin.Card).padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Nothing saved yet",style=MaterialTheme.typography.titleMedium,fontSize=16.sp)
            Text("Share into StashDrop or add your first note.",fontSize=13.sp,color=Origin.Muted)
        }
        FilledTonalButton(onClick=onAdd) { Text("Add note") }
    }
    else items.take(3).forEach { item ->
        Row(Modifier.fillMaxWidth().heightIn(min=58.dp).clip(RoundedCornerShape(20.dp)).background(Origin.Card).clickable(onClickLabel="Open in shelf") { onOpenItem(item) }
            .padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(item.isMedia()) ShelfMediaThumbnail(item,mediaFile(item),Modifier.size(40.dp))
            else Box(Modifier.size(8.dp).background(categoryTint(item),CircleShape))
            Text(item.text?.replace('\n',' ') ?: item.category().label,Modifier.weight(1f),fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(item.collection,fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=1)
        }
    }
}

@Composable private fun categoryTint(item: StashItemEntity): Color = when(item.category()) {
    ShelfCategory.URLS -> Origin.Orange
    ShelfCategory.OTP -> Origin.Success
    ShelfCategory.TEXT -> MaterialTheme.colorScheme.primary
    else -> Origin.Ink
}

// ---------- Shelf ----------

@Composable private fun SearchField(query: String, onChange: (String)->Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(Origin.Card).padding(start=16.dp,end=4.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.Search,null,Modifier.size(18.dp),tint=Origin.Muted)
        BasicTextField(query,onChange,Modifier.weight(1f).semantics { contentDescription="Search your shelf" },singleLine=true,
            textStyle=TextStyle(fontSize=15.sp,color=Origin.Ink),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox={ inner -> Box(contentAlignment=Alignment.CenterStart) {
                if(query.isEmpty()) Text("Search your shelf",fontSize=15.sp,color=Origin.Muted,maxLines=1)
                inner()
            } })
        if(query.isNotEmpty()) IconButton(onClick={ onChange("") }) { Icon(Icons.Outlined.Close,"Clear search",Modifier.size(18.dp),tint=Origin.Muted) }
    }
}

/** Notes and links span the full width; photos and videos pair up two per row, keeping shelf order. */
@Composable private fun ShelfGrid(visible: List<StashItemEntity>, selectedIds: List<String>, mediaFile: (StashItemEntity)->File?,
    onSelect: (StashItemEntity)->Unit, onCollect: (StashItemEntity)->Unit, onDeleteRequest: (StashItemEntity)->Unit) {
    val rows=remember(visible) {
        val out=mutableListOf<List<StashItemEntity>>(); var pending: StashItemEntity?=null
        visible.forEach { item ->
            if(item.isMedia()) { pending?.let { out+=listOf(it,item); pending=null } ?: run { pending=item } }
            else { pending?.let { out+=listOf(it) }; pending=null; out+=listOf(item) }
        }
        pending?.let { out+=listOf(it) }
        out
    }
    rows.forEach { row ->
        if(row.size==2 || row[0].isMedia()) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            row.forEach { item -> Box(Modifier.weight(1f)) { ShelfCard(item,mediaFile(item),item.id in selectedIds,{onSelect(item)},{onCollect(item)},{onDeleteRequest(item)}) } }
            if(row.size==1) Spacer(Modifier.weight(1f))
        } else ShelfCard(row[0],mediaFile(row[0]),row[0].id in selectedIds,{onSelect(row[0])},{onCollect(row[0])},{onDeleteRequest(row[0])})
    }
}

@Composable private fun ShelfCard(item: StashItemEntity, file: File?, chosen: Boolean, onSelect: ()->Unit, onCollect: ()->Unit, onDeleteRequest: ()->Unit) {
    val accent=MaterialTheme.colorScheme.primary
    val tilt by animateFloatAsState(if(chosen) -1.2f else 0f,label="selected tilt")
    SwipeDeleteItem(onDeleteRequest) {
        Surface(onClick=onSelect,modifier=Modifier.fillMaxWidth().rotate(tilt).semantics { selected=chosen },shape=RoundedCornerShape(26.dp),
            color=Origin.Card,border=if(chosen) BorderStroke(2.5.dp,accent) else null) {
            if(item.isMedia()) Box(Modifier.fillMaxWidth().height(176.dp)) {
                ShelfMediaThumbnail(item,file,Modifier.fillMaxSize())
                CollectionPill(item.collection,onCollect,Modifier.align(Alignment.BottomStart).padding(10.dp),onImage=true)
                if(chosen) CheckBadge(Modifier.align(Alignment.TopEnd).padding(10.dp))
            } else Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                val title=item.text?.replace('\n',' ') ?: "Saved file"
                Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    when(item.category()) {
                        ShelfCategory.URLS -> IconBadge(Icons.Outlined.Link,Origin.OrangeSoft,Origin.Orange,corner=18.dp)
                        ShelfCategory.OTP -> IconBadge(Icons.Outlined.Password,Color(0xFFE3F6EC),Origin.Success,corner=18.dp)
                        ShelfCategory.FILES -> IconBadge(Icons.AutoMirrored.Outlined.InsertDriveFile,Origin.Ground,Origin.Ink,corner=18.dp)
                        else -> Unit
                    }
                    if(title.length<=60) Text(title,Modifier.weight(1f),fontFamily=Outfit,fontWeight=FontWeight.SemiBold,fontSize=18.sp,lineHeight=24.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                    else Text(title,Modifier.weight(1f),fontSize=15.sp,lineHeight=21.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    CollectionPill(item.collection,onCollect)
                    Spacer(Modifier.weight(1f))
                    if(chosen) CheckBadge()
                }
            }
        }
    }
}

@Composable private fun CollectionPill(name: String, onClick: ()->Unit, modifier: Modifier=Modifier, onImage: Boolean=false) {
    val accent=MaterialTheme.colorScheme.primary
    Row(modifier.heightIn(min=32.dp).clip(CircleShape).background(if(onImage) Color.White.copy(alpha=.92f) else MaterialTheme.colorScheme.primaryContainer)
        .clickable(onClickLabel="Move to collection",role=Role.Button,onClick=onClick).padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.FolderOpen,null,Modifier.size(14.dp),tint=if(onImage) Origin.Ink else accent)
        Text(name,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold,color=if(onImage) Origin.Ink else accent,maxLines=1)
    }
}

@Composable private fun CheckBadge(modifier: Modifier=Modifier) {
    Box(modifier.size(26.dp).background(MaterialTheme.colorScheme.primary,CircleShape).border(2.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) {
        Icon(Icons.Outlined.Check,"Selected",Modifier.size(15.dp),tint=Color.White)
    }
}

@Composable private fun SelectionBar(count: Int,onClear: ()->Unit,onShare: ()->Unit,onWhatsApp: ()->Unit,onMove: ()->Unit,onDelete: ()->Unit) {
    var more by remember {mutableStateOf(false)}
    Row(Modifier.padding(horizontal=16.dp).fillMaxWidth().shadow(14.dp,CircleShape).clip(CircleShape).background(Origin.Ink).padding(horizontal=6.dp,vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onClear) {Icon(Icons.Outlined.Close,"Clear selection",tint=Color.White)}
        Text("$count selected",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
        IconButton(onClick=onShare) {Icon(Icons.Outlined.IosShare,"Share selected",tint=Color.White)}
        IconButton(onClick=onWhatsApp) {Icon(Icons.AutoMirrored.Outlined.Send,"Send selected to WhatsApp",tint=Color.White)}
        Box {
            IconButton(onClick={more=true}) {Icon(Icons.Outlined.MoreHoriz,"More selection actions",tint=Color.White)}
            DropdownMenu(more,{more=false},shape=RoundedCornerShape(20.dp)) {
                DropdownMenuItem(text={Text("Move to collection")},onClick={more=false;onMove()},leadingIcon={Icon(Icons.Outlined.Folder,null)})
                DropdownMenuItem(text={Text("Delete selected")},onClick={more=false;onDelete()},leadingIcon={Icon(Icons.Outlined.DeleteOutline,null)})
            }
        }
    }
}

// ---------- Settings ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SettingsTab(shizuku: ShizukuConnectionManager.State, setup: SetupState, actions: DashboardActions,
    assistantExpanded: Boolean, onToggleAssistant: ()->Unit, onOpenInteractions: ()->Unit) {
    val accent=MaterialTheme.colorScheme.primary
    Text("Settings",Modifier.padding(horizontal=4.dp),style=MaterialTheme.typography.headlineLarge,fontSize=30.sp)
    SetupRing(setup,actions)
    if(shizuku.connected && !setup.complete) Button(onClick=actions.setupAll,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=CircleShape) {
        Icon(Icons.Outlined.AutoFixHigh,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Set up with Shizuku")
    }
    GroupLabel("Permissions")
    GroupCard {
        SetupRow(Icons.Outlined.Layers,Origin.Ink,"Edge shelf",setup.overlay,"Allow",actions.allowOverlay)
        GroupDivider()
        SetupRow(Icons.Outlined.Notifications,Origin.Orange,"Notifications",setup.notifications,"Allow",actions.allowNotifications)
        GroupDivider()
        SetupRow(Icons.Outlined.Hub,Color(0xFF6E3FF3),"Shizuku",shizuku.connected,if(shizuku.granted) "Check" else "Connect",actions.connectShizuku,
            statusText=if(shizuku.connected) "Bridge connected" else shizuku.message)
    }
    GroupLabel("Circle to Search")
    GroupCard {
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Outlined.CropFree,accent)
            Column(Modifier.weight(1f)) {
                Text("Google Circle to Search",fontSize=15.sp,fontWeight=FontWeight.Medium,maxLines=1)
                Text(setup.google.label,fontSize=12.sp,lineHeight=16.sp,color=if(setup.google==GoogleState.READY) Origin.Success else Origin.Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
            }
        }
        Text("Every search button and gesture asks Google. On phones Google hasn't enabled, it may open Gemini instead.",
            Modifier.padding(start=66.dp,end=16.dp),fontSize=12.sp,lineHeight=17.sp,color=Origin.Muted)
        FlowRow(Modifier.fillMaxWidth().padding(start=66.dp,end=16.dp,top=4.dp,bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SoftButton("Try Google search",actions.search)
            SoftButton("Choose Google assistant",actions.chooseAssistant)
        }
    }
    if(setup.assistantSupported) {
        GroupLabel("Digital assistant")
        GroupCard {
            NavRow(Icons.Outlined.AssistantPhoto,Origin.Orange,Color.White,"Digital assistant",when {
                setup.assistant && setup.google==GoogleState.READY -> "StashDrop gestures · Google voice active"
                setup.assistant -> "StashDrop selected"
                else -> "System picker and optional Shizuku"
            },expanded=assistantExpanded,onClick=onToggleAssistant)
            if(assistantExpanded) Column(Modifier.padding(start=66.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Assistant gestures and Google's voice service are separate settings. Google decides native search eligibility.",
                    Modifier.padding(end=16.dp),fontSize=12.sp,lineHeight=17.sp,color=Origin.Muted)
                FlowRow(Modifier.fillMaxWidth().padding(end=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    SoftButton("Open assistant picker",actions.chooseAssistant)
                    if(shizuku.connected) {
                        SoftButton(if(setup.assistant) "Restore previous" else "Use StashDrop now",if(setup.assistant) actions.restoreAssistant else actions.makeAssistant)
                        if(setup.assistant && setup.google==GoogleState.NOT_ASSISTANT) SoftButton("Connect Google",actions.linkGoogle)
                    } else SoftButton("Connect Shizuku",actions.connectShizuku)
                }
            }
        }
    }
    GroupLabel("Personalise")
    GroupCard { NavRow(Icons.Outlined.Tune,Color(0xFF6E3FF3),Color.White,"Gestures, clipboard & feel","Customize interactions and motion",onClick=onOpenInteractions) }
}

/** Progress ring over the required setup steps, with a one-tap fix for the first missing one. */
@Composable private fun SetupRing(setup: SetupState, actions: DashboardActions) {
    val steps=listOf(
        Triple(setup.overlay,"Edge shelf permission is off",actions.allowOverlay),
        Triple(setup.notifications,"Notifications are off",actions.allowNotifications),
        Triple(setup.google==GoogleState.READY,"Google Circle to Search isn't ready",actions.chooseAssistant),
    )
    val done=steps.count { it.first }
    val missing=steps.firstOrNull { !it.first }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Origin.Ink).padding(18.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(64.dp).semantics { contentDescription="Setup $done of ${steps.size} done" },contentAlignment=Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke=8.dp.toPx(); val inset=stroke/2
                val arc=androidx.compose.ui.geometry.Size(size.width-stroke,size.height-stroke)
                drawArc(Color.White.copy(alpha=.18f),0f,360f,false,Offset(inset,inset),arc,style=Stroke(stroke))
                drawArc(Origin.Mint,-90f,360f*done/steps.size,false,Offset(inset,inset),arc,style=Stroke(stroke,cap=StrokeCap.Round))
            }
            Text("$done/${steps.size}",color=Color.White,fontFamily=Outfit,fontWeight=FontWeight.Bold,fontSize=16.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(if(missing==null) "All set" else "Almost set up",color=Color.White,style=MaterialTheme.typography.titleMedium,fontSize=18.sp,maxLines=1)
            Text(missing?.second ?: "Everything is ready",color=Color(0xFFC4C8D4),fontSize=12.sp,lineHeight=16.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
        }
        if(missing!=null) Surface(onClick=missing.third,shape=CircleShape,color=Color.White) {
            Text("Fix",Modifier.heightIn(min=40.dp).wrapContentHeight().padding(horizontal=18.dp),color=Origin.Ink,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
        }
    }
}

@Composable private fun SetupRow(icon: ImageVector,badge: Color,title: String,done: Boolean,action: String,onClick: ()->Unit,statusText: String=if(done) "On" else "Not enabled") {
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        IconBadge(icon,badge)
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Text(statusText,fontSize=12.sp,lineHeight=16.sp,color=if(done) Origin.Success else Origin.Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
        }
        if(done) Icon(Icons.Outlined.CheckCircle,"Enabled",tint=Origin.Success)
        else FilledTonalButton(onClick=onClick,shape=CircleShape,contentPadding=PaddingValues(horizontal=16.dp)) {Text(action)}
    }
}

@Composable private fun SoftButton(label: String,onClick: ()->Unit) {
    Surface(onClick=onClick,shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
        Text(label,Modifier.heightIn(min=40.dp).wrapContentHeight().padding(horizontal=16.dp),color=MaterialTheme.colorScheme.primary,
            fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1)
    }
}

/** A row that navigates or expands, ending in a chevron. */
@Composable private fun NavRow(icon: ImageVector,badge: Color,tint: Color,title: String,subtitle: String,card: Boolean=false,expanded: Boolean?=null,onClick: ()->Unit) {
    val turn by animateFloatAsState(if(expanded==true) 90f else 0f,label="chevron")
    Row(Modifier.fillMaxWidth().then(if(card) Modifier.clip(RoundedCornerShape(24.dp)).background(Origin.Card) else Modifier)
        .clickable(onClick=onClick).heightIn(min=64.dp).padding(horizontal=16.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        IconBadge(icon,badge,tint)
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Text(subtitle,fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(20.dp).rotate(turn),tint=Color(0xFF9AA0AE))
    }
}

@Composable private fun SettingSwitch(icon: ImageVector,badge: Color,title: String,subtitle: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked,role=Role.Switch,onValueChange=onChange).heightIn(min=64.dp).padding(horizontal=16.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        IconBadge(icon,badge)
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=15.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Text(subtitle,fontSize=12.sp,lineHeight=16.sp,color=Origin.Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
        }
        OriginSwitch(checked)
    }
}

// ---------- Interactions ----------

@Composable private fun InteractionsTab(settings: StashSettings, clipboardStatus: String, actions: DashboardActions, onBack: ()->Unit, onAutoStash: (Boolean)->Unit) {
    val accent=MaterialTheme.colorScheme.primary
    Row(verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back to Settings") }
        Text("Your way",style=MaterialTheme.typography.headlineLarge,fontSize=30.sp)
    }
    Text("One search strip. One movable edge shelf.",Modifier.padding(horizontal=4.dp),fontSize=14.sp,color=Origin.Muted)
    GroupLabel("Gestures")
    GroupCard {
        SettingSwitch(Icons.Outlined.North,Origin.Ink,"Gesture strip","Swipe up above the bottom navigation bar. No persistent markers.",settings.gesturesEnabled) {actions.preference("bar",it)}
        GroupDivider()
        NavRow(Icons.Outlined.AssistantPhoto,Origin.Orange,Color.White,"System corner swipe","Enable Google's assistant corner gesture in your phone settings",onClick=actions.chooseAssistant)
    }
    Text("Android handles physical corner swipes. Availability and Google search eligibility depend on your phone. Drag the shelf handle to either edge; its position is remembered.",
        Modifier.padding(horizontal=8.dp),fontSize=12.sp,lineHeight=17.sp,color=Origin.Muted)
    GroupLabel("Clipboard")
    GroupCard { SettingSwitch(Icons.Outlined.ContentPaste,Origin.Orange,"Auto-stash copied text","May retain passwords, codes or private text",settings.autoStash,onAutoStash) }
    if(settings.autoStash) Text(clipboardStatus,Modifier.padding(horizontal=8.dp),fontSize=12.sp,lineHeight=16.sp,color=Origin.Success)
    GroupLabel("Feel")
    GroupCard {
        SettingSwitch(Icons.Outlined.GraphicEq,Color(0xFF6E3FF3),"Haptic feedback","Feel drags, stacks and circles",settings.haptics) {actions.preference("haptics",it)}
        GroupDivider()
        Column(Modifier.padding(horizontal=16.dp,vertical=12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Springiness",Modifier.weight(1f),fontSize=15.sp,fontWeight=FontWeight.Medium)
                Text(java.lang.String.format(java.util.Locale.ROOT,"%.1f×",settings.bounce),fontSize=13.sp,color=Origin.Muted)
            }
            Slider(settings.bounce,actions.bounce,valueRange=.5f..1.5f,colors=SliderDefaults.colors(thumbColor=accent,activeTrackColor=accent,inactiveTrackColor=Origin.Hairline))
        }
        GroupDivider()
        Column(Modifier.padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Accent colour",fontSize=15.sp,fontWeight=FontWeight.Medium)
            Row(Modifier.selectableGroup(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                AccentChoices.forEachIndexed { index,(name,color) ->
                    val chosen=settings.accent==index
                    Box(Modifier.size(44.dp).clip(CircleShape).background(color)
                        .then(if(chosen) Modifier.border(3.dp,Color.White,CircleShape) else Modifier)
                        .selectable(chosen,role=Role.RadioButton) { actions.accent(index) }.semantics { contentDescription="$name accent" },
                        contentAlignment=Alignment.Center) {
                        if(chosen) Icon(Icons.Outlined.Check,null,Modifier.size(20.dp),tint=Color.White)
                    }
                }
            }
        }
    }
}

// ---------- Navigation ----------

@Composable private fun PillNavigation(tab: Int, onSelect: (Int)->Unit) {
    Row(Modifier.shadow(18.dp,CircleShape,ambientColor=Origin.Ink.copy(alpha=.12f),spotColor=Origin.Ink.copy(alpha=.2f)).clip(CircleShape)
        .background(Origin.Card).padding(8.dp).selectableGroup(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
        listOf(Triple("Home",Icons.Outlined.SpaceDashboard,0),Triple("Shelf",Icons.Outlined.Layers,1),Triple("Settings",Icons.Outlined.Tune,2)).forEach { (label,icon,index) ->
            val active=tab==index
            Row(Modifier.height(48.dp).clip(CircleShape).background(if(active) Origin.Ink else Color.Transparent)
                .selectable(active,role=Role.Tab) { onSelect(index) }
                .then(if(active) Modifier.padding(horizontal=18.dp) else Modifier.width(48.dp).semantics { contentDescription=label }),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Icon(icon,null,Modifier.size(20.dp),tint=if(active) Color.White else Origin.Muted)
                if(active) { Spacer(Modifier.width(8.dp)); Text(label,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp) }
            }
        }
    }
}
