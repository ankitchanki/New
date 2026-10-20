package com.vibe.stashdrop.features.circletosearch

import android.app.Application
import android.app.SearchManager
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.core.shizuku.CapturedFrame
import com.vibe.stashdrop.features.stashshelf.StashItemEntity
import com.vibe.stashdrop.features.stashshelf.UrlPrivacySanitizer
import com.vibe.stashdrop.stashApp
import com.vibe.stashdrop.ui.theme.StashDropTheme
import kotlinx.coroutines.launch

class CircleSearchModel(app: Application): AndroidViewModel(app) {
    val source=CapturedFrame.take()
    var crop by mutableStateOf<Bitmap?>(null); private set
    var result by mutableStateOf<OcrAndBarcodeProcessor.Result?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf("")
    private val processor by lazy { OcrAndBarcodeProcessor() }
    private var processorCreated=false
    private var saved: StashItemEntity?=null
    val extracted get()=result?.let { (listOf(it.text)+it.codes).filter(String::isNotBlank).distinct().joinToString("\n") }.orEmpty()
    /** Stash, Lens and Share reuse one saved copy of the current cutout. */
    suspend fun saveCrop()=saved ?: getApplication<Application>().stashApp.repository.addBitmap(crop!!).also { saved=it }
    fun select(points: List<androidx.compose.ui.geometry.Offset>, size: androidx.compose.ui.unit.IntSize) {
        if(busy) return
        val bitmap=source ?: return
        busy=true
        viewModelScope.launch {
            try {
                val selected=cropLasso(bitmap,points,size)
                crop=selected; saved=null; result=null; message=""
                val reader=processor; processorCreated=true
                result=reader.process(selected)
            } catch(e: Exception) { message=e.message ?: "Selection failed" } finally { busy=false }
        }
    }
    // Bitmaps are left to the garbage collector: recycling one the render thread still draws crashes the app.
    fun reset() { if(!busy) { crop=null; saved=null; result=null; message="" } }
    override fun onCleared() { if(processorCreated) runCatching { processor.close() } }
}

class CircleSearchActivity: ComponentActivity() {
    private val model by viewModels<CircleSearchModel>()
    private val haptics by lazy { TactileHapticEngine(this) }
    private fun webSearch(query: String) {
        val text=UrlPrivacySanitizer.cleanText(query).take(500)
        val search=Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY,text)
        runCatching { startActivity(search) }.recoverCatching {
            startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(text))))
        }.onFailure { model.message="No browser available" }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StashDropTheme {
            val scope=rememberCoroutineScope()
            var saving by remember { mutableStateOf(false) }
            val source=model.source
            var flight by remember { mutableStateOf<com.vibe.stashdrop.core.physics.MagneticFlight?>(null) }
            fun withCutout(action: suspend (StashItemEntity)->Unit) {
                saving=true
                scope.launch { runCatching { action(model.saveCrop()) }.onFailure { model.message=it.message.orEmpty() }; saving=false }
            }
            BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val resultPanelHeight=maxHeight*.5f
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={ finish() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Close Circle to Search") }
                        Text("BUILT-IN CIRCLE",style=MaterialTheme.typography.labelSmall,letterSpacing=2.sp)
                        Spacer(Modifier.weight(1f))
                        if(model.crop!=null) TextButton(enabled=!model.busy && !saving,onClick={ haptics.tick(); model.reset() }) { Text("Again",maxLines=1) }
                    }
                    Text("Circle something useful.",Modifier.padding(horizontal=22.dp,vertical=8.dp),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                    when {
                        source==null -> Text("Capture expired. Close and try again.",Modifier.padding(24.dp))
                        model.crop==null -> {
                            Text("Draw a loop around the detail you want to keep.",Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=8.dp),
                                style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,
                                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=22.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
                                AuroraCircleOverlayCanvas(source,Modifier.aspectRatio(source.width.toFloat()/source.height).clip(RoundedCornerShape(24.dp)),model::select)
                            }
                            if(model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        else -> {
                            Image(model.crop!!.asImageBitmap(),"Selected area",Modifier.weight(1f).fillMaxWidth().padding(24.dp),contentScale=ContentScale.Fit)
                            Surface(modifier=Modifier.heightIn(max=resultPanelHeight),shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),color=MaterialTheme.colorScheme.surface) {
                                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                    Text("Selection ready",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
                                    val extracted=model.extracted
                                    when {
                                        model.busy -> Row(verticalAlignment=Alignment.CenterVertically) {
                                            CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp); Spacer(Modifier.width(10.dp)); Text("Reading selection…",maxLines=1)
                                        }
                                        extracted.isNotEmpty() -> Text(extracted,maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                                        else -> Text("No text found. Search the image with Lens.",maxLines=1,overflow=TextOverflow.Ellipsis,
                                            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
                                    }
                                    Text("Lens and Share also save this cutout to your shelf.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    val enabled=!model.busy && !saving
                                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Button(enabled=enabled,onClick={
                                            haptics.select()
                                            if(extracted.isNotEmpty()) webSearch(extracted)
                                            else withCutout { VisualLensBridge.lens(this@CircleSearchActivity,stashApp.repository.uri(it)!!) }
                                        }) { Icon(Icons.Outlined.Search,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Search",maxLines=1) }
                                        Action("Lens",Icons.Outlined.CenterFocusWeak,enabled) { haptics.select(); withCutout { VisualLensBridge.lens(this@CircleSearchActivity,stashApp.repository.uri(it)!!) } }
                                        Action("Copy",Icons.Outlined.ContentCopy,enabled && extracted.isNotEmpty()) {
                                            haptics.select()
                                            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Circle text",UrlPrivacySanitizer.cleanText(extracted)))
                                            model.message="Copied"
                                        }
                                        Action("Stash",Icons.Outlined.Inventory2,enabled) {
                                            withCutout {
                                                haptics.confirm()
                                                val metrics=resources.displayMetrics
                                                flight=com.vibe.stashdrop.core.physics.MagneticTrajectoryEngine.flight(
                                                    com.vibe.stashdrop.core.physics.Vector2(metrics.widthPixels*.5f,metrics.heightPixels*.45f),
                                                    com.vibe.stashdrop.core.physics.Vector2(metrics.widthPixels*.9f,metrics.heightPixels*.7f),
                                                    com.vibe.stashdrop.core.physics.Vector2(200f,-600f))
                                                model.message="Saved to shelf"
                                            }
                                        }
                                        Action("Share",Icons.Outlined.IosShare,enabled) { haptics.select(); withCutout { VisualLensBridge.share(this@CircleSearchActivity,stashApp.repository,listOf(it)) } }
                                    }
                                    if(model.message.isNotEmpty()) Text(model.message,color=MaterialTheme.colorScheme.primary,maxLines=1,style=MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    if(model.crop==null && model.message.isNotEmpty()) Text(model.message,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.primary,maxLines=2)
                }
                com.vibe.stashdrop.features.dragdrop.CrossAppDropFlightOverlay(flight,Modifier.fillMaxSize()) {flight=null}
            }
        } }
    }
}

@Composable private fun Action(label: String,icon: ImageVector,enabled: Boolean,onClick: ()->Unit) {
    OutlinedButton(onClick=onClick,enabled=enabled,contentPadding=PaddingValues(horizontal=14.dp)) {
        Icon(icon,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(label,maxLines=1)
    }
}
