package com.vibe.stashdrop.features.circletosearch

import android.app.Application
import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.stashdrop.core.shizuku.CapturedFrame
import com.vibe.stashdrop.features.stashshelf.UrlPrivacySanitizer
import com.vibe.stashdrop.stashApp
import com.vibe.stashdrop.ui.theme.StashDropTheme
import kotlinx.coroutines.launch

class StudioModel(app: Application): AndroidViewModel(app) {
    val source=CapturedFrame.take()
    var crop by mutableStateOf<Bitmap?>(null); private set
    var result by mutableStateOf<OcrAndBarcodeProcessor.Result?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf("")
    private val processor=OcrAndBarcodeProcessor()
    private var saved: com.vibe.stashdrop.features.stashshelf.StashItemEntity?=null
    /** Stash, Lens and Share reuse one saved copy of the current cutout. */
    suspend fun saveCrop()=saved ?: getApplication<Application>().stashApp.repository.addBitmap(crop!!).also { saved=it }
    fun select(points: List<androidx.compose.ui.geometry.Offset>, size: androidx.compose.ui.unit.IntSize) {
        if(busy) return
        val bitmap=source ?: return
        busy=true
        viewModelScope.launch {
            try {
                val selected=cropLasso(bitmap,points,size)
                crop?.recycle(); crop=selected; saved=null; result=null
                result=processor.process(selected)
            } catch(e: Exception) { message=e.message ?: "Selection failed" } finally { busy=false }
        }
    }
    fun reset() { if(!busy) { crop?.recycle(); crop=null; saved=null; result=null } }
    override fun onCleared() { processor.close(); source?.recycle(); crop?.recycle() }
}
class CircleStudioActivity: ComponentActivity() {
    private val model by viewModels<StudioModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StashDropTheme {
            val scope=rememberCoroutineScope()
            var saving by remember { mutableStateOf(false) }
            val source=model.source
            var flight by remember { mutableStateOf<com.vibe.stashdrop.core.physics.MagneticFlight?>(null) }
            Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Circle Studio",style=MaterialTheme.typography.titleLarge)
                        TextButton(onClick={ finish() }) { Text("Close") }
                    }
                    if(source==null) Text("Capture expired. Close and capture again.",Modifier.padding(24.dp))
                    else if(model.crop==null) {
                        Text("Circle text, a code, or an object",Modifier.padding(horizontal=16.dp))
                        AuroraCircleOverlayCanvas(source,Modifier.fillMaxWidth().weight(1f),model::select)
                    } else {
                        Image(model.crop!!.asImageBitmap(),"Selected cutout",Modifier.weight(1f).fillMaxWidth().padding(24.dp))
                        Column(Modifier.fillMaxWidth().heightIn(max=300.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                            if(model.busy) CircularProgressIndicator()
                            val extracted=model.result?.let { (listOf(it.text)+it.codes).filter(String::isNotBlank).distinct().joinToString("\n") }.orEmpty()
                            if(extracted.isNotEmpty()) Text(extracted,Modifier.padding(bottom=12.dp))
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Button(enabled=!model.busy && !saving,onClick={ saving=true; scope.launch {
                                    runCatching { model.saveCrop() }.onSuccess {
                                        val metrics=resources.displayMetrics
                                        flight=com.vibe.stashdrop.core.physics.MagneticTrajectoryEngine.flight(
                                            com.vibe.stashdrop.core.physics.Vector2(metrics.widthPixels*.5f,metrics.heightPixels*.45f),
                                            com.vibe.stashdrop.core.physics.Vector2(metrics.widthPixels*.9f,metrics.heightPixels*.7f),
                                            com.vibe.stashdrop.core.physics.Vector2(200f,-600f))
                                        model.message="Saved to shelf"
                                    }.onFailure { model.message=it.message.orEmpty() }
                                    saving=false
                                } }) { Text("Stash") }
                                OutlinedButton(enabled=extracted.isNotEmpty(),onClick={
                                    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Circle text",UrlPrivacySanitizer.cleanText(extracted)))
                                    model.message="Copied"
                                }) { Text("Copy text") }
                                TextButton(enabled=!model.busy && !saving,onClick=model::reset) { Text("Again") }
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(enabled=!model.busy && !saving,onClick={ saving=true; scope.launch {
                                    runCatching { val item=model.saveCrop(); VisualLensBridge.lens(this@CircleStudioActivity,stashApp.repository.uri(item)!!) }
                                        .onFailure { model.message=it.message.orEmpty() }
                                    saving=false
                                } }) { Text("Lens") }
                                OutlinedButton(enabled=!model.busy && !saving,onClick={ saving=true; scope.launch {
                                    runCatching { val item=model.saveCrop(); VisualLensBridge.share(this@CircleStudioActivity,stashApp.repository,listOf(item)) }
                                        .onFailure { model.message=it.message.orEmpty() }
                                    saving=false
                                } }) { Text("Share cutout") }
                            }
                        }
                    }
                    if(model.busy && model.crop==null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(model.message.isNotEmpty()) Text(model.message,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.primary)
                }
            }
            com.vibe.stashdrop.features.dragdrop.CrossAppDropFlightOverlay(flight,Modifier.fillMaxSize()) {flight=null}
            }
        } }
    }
}
