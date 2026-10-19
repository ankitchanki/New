package com.vibe.stashdrop

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibe.stashdrop.core.shizuku.SecureSettingsAutomator
import com.vibe.stashdrop.features.circletosearch.*
import com.vibe.stashdrop.features.floatingdock.OneUiThumbDockService
import com.vibe.stashdrop.ui.dashboard.DashboardScreen
import com.vibe.stashdrop.ui.theme.StashDropTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var overlayAllowed by mutableStateOf(false)
    override fun onResume() { super.onResume(); overlayAllowed=Settings.canDrawOverlays(this); stashApp.shizuku.refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { StashDropTheme {
            val items by stashApp.repository.items.collectAsStateWithLifecycle(emptyList())
            val settings by stashApp.preferences.settings.collectAsStateWithLifecycle(com.vibe.stashdrop.features.stashshelf.StashSettings())
            val shizuku by stashApp.shizuku.state.collectAsStateWithLifecycle()
            val running by OneUiThumbDockService.running.collectAsStateWithLifecycle()
            val clipboardStatus by OneUiThumbDockService.clipboardStatus.collectAsStateWithLifecycle()
            var message by remember { mutableStateOf("") }
            DashboardScreen(items,settings,shizuku,running,overlayAllowed,message,clipboardStatus,
                onToggleDock={
                    if(running) startService(Intent(this,OneUiThumbDockService::class.java).setAction(OneUiThumbDockService.STOP))
                    else if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
                    else {
                        if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        runCatching { ContextCompat.startForegroundService(this,Intent(this,OneUiThumbDockService::class.java)) }.onFailure { message=it.message.orEmpty() }
                    }
                },
                onShizuku=stashApp.shizuku::requestPermission,
                onAccessibility={startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},
                onSearch={builtIn->lifecycleScope.launch { moveTaskToBack(true); delay(250); runCatching { CircleToSearchOrchestrator(this@MainActivity).launch(builtIn) }.onFailure { message=it.message.orEmpty() } }},
                onPreference={key,value->lifecycleScope.launch { stashApp.preferences.set(key,value) }},
                onBounce={value->lifecycleScope.launch { stashApp.preferences.bounce(value) }},
                onAssistant={enable->lifecycleScope.launch {
                    val automator=SecureSettingsAutomator(this@MainActivity,stashApp.shizuku)
                    message=runCatching { if(enable) automator.enableGoogleAssistant() else automator.restore() }.getOrElse { it.message.orEmpty() }
                }},
                onAddText={text->lifecycleScope.launch { runCatching { stashApp.repository.addText(text) }.onFailure { message=it.message.orEmpty() } }},
                onDelete={item->lifecycleScope.launch { stashApp.repository.delete(item) }},
                onShare={selected,whatsapp->runCatching { VisualLensBridge.share(this,stashApp.repository,selected,whatsapp) }.onFailure { message=it.message.orEmpty() }},
                onCollect={item,collection->lifecycleScope.launch { stashApp.repository.move(item,collection) }})
        } }
    }
}
