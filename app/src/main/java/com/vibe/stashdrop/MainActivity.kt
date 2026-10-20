package com.vibe.stashdrop

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
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
import com.vibe.stashdrop.core.shizuku.AssistantController
import com.vibe.stashdrop.features.circletosearch.*
import com.vibe.stashdrop.features.floatingdock.OneUiThumbDockService
import com.vibe.stashdrop.ui.dashboard.*
import com.vibe.stashdrop.ui.theme.StashDropTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Android stops showing the prompt after repeated denials; send the person to the app's notification settings instead.
        if(!granted && Build.VERSION.SDK_INT>=33 && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) open(notificationSettings)
        refreshSetup()
    }
    private val assistant by lazy { AssistantController(this,stashApp.shizuku) }
    private var setup by mutableStateOf(SetupState())
    private var message by mutableStateOf("")
    override fun onResume() { super.onResume(); stashApp.shizuku.refresh(); refreshSetup() }
    private fun refreshSetup() {
        setup=SetupState(overlay=Settings.canDrawOverlays(this),
            notifications=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED,
            assistant=assistant.isDefault(),assistantSupported=assistant.supported,
            google=when(GoogleCircleToSearch.readiness(this)) {
                GoogleCircleToSearch.Readiness.READY -> GoogleState.READY
                GoogleCircleToSearch.Readiness.GOOGLE_NOT_ASSISTANT -> GoogleState.NOT_ASSISTANT
                GoogleCircleToSearch.Readiness.GOOGLE_MISSING -> GoogleState.MISSING
            },
            crash=com.vibe.stashdrop.core.diagnostics.CrashReporter.read(this))
    }
    private fun open(vararg intents: Intent) {
        if(intents.none { runCatching { startActivity(it) }.isSuccess }) message="That setting is not available on this phone"
    }
    /** Runs a Shizuku action when connected; otherwise opens the matching system screen. */
    private fun shizukuOr(action: suspend ()->String?,vararg fallback: Intent) { lifecycleScope.launch {
        val result=runCatching { action() }.getOrElse { "Error: ${it.message}" }
        if(result==null) open(*fallback) else message=result.removePrefix("Error: ")
        refreshSetup()
    } }
    private val notificationSettings get()=Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName)
    private val overlaySettings get()=Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))
    /** The system screen where the digital assistant app is picked by hand. */
    private val defaultApps get()=arrayOf(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),Intent(Settings.ACTION_SETTINGS))
    private fun startSearch() { lifecycleScope.launch {
        moveTaskToBack(true); delay(250)
        runCatching {
            if(OneUiThumbDockService.running.value) startService(Intent(this@MainActivity,OneUiThumbDockService::class.java).setAction(OneUiThumbDockService.SEARCH))
            else CircleToSearchOrchestrator(this@MainActivity).launch()
        }.onFailure { message=it.message.orEmpty() }
    } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        val actions=DashboardActions(
            toggleDock={
                if(OneUiThumbDockService.running.value) startService(Intent(this,OneUiThumbDockService::class.java).setAction(OneUiThumbDockService.STOP))
                else if(!Settings.canDrawOverlays(this)) shizukuOr({ assistant.grantSetup() },overlaySettings)
                else {
                    if(!setup.notifications) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    runCatching { ContextCompat.startForegroundService(this,Intent(this,OneUiThumbDockService::class.java)) }.onFailure { message=it.message.orEmpty() }
                }
            },
            connectShizuku=stashApp.shizuku::requestPermission,
            search=::startSearch,
            preference={key,value->lifecycleScope.launch { stashApp.preferences.set(key,value) }},
            bounce={value->lifecycleScope.launch { stashApp.preferences.bounce(value) }},
            addText={text->lifecycleScope.launch { runCatching { stashApp.repository.addText(text) }.onFailure { message=it.message.orEmpty() } }},
            delete={item->lifecycleScope.launch { runCatching { stashApp.repository.delete(item) }.onFailure { message=it.message ?: "Could not delete item" } }},
            share={selected,whatsapp->runCatching { VisualLensBridge.share(this,stashApp.repository,selected,whatsapp) }.onFailure { message=it.message.orEmpty() }},
            collect={item,collection->lifecycleScope.launch { runCatching { stashApp.repository.move(item,collection) }.onFailure { message=it.message ?: "Could not move item" } }},
            deleteMany={selected->lifecycleScope.launch { runCatching { selected.forEach { stashApp.repository.delete(it) } }.onFailure { message=it.message ?: "Could not delete selection" } }},
            collectMany={selected,collection->lifecycleScope.launch { runCatching { selected.forEach { stashApp.repository.move(it,collection) } }.onFailure { message=it.message ?: "Could not move selection" } }},
            allowOverlay={ shizukuOr({ assistant.grantSetup() },overlaySettings) },
            allowNotifications={
                if(stashApp.shizuku.service!=null) shizukuOr({ assistant.grantSetup() })
                else if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else open(notificationSettings)
            },
            makeAssistant={ shizukuOr({ assistant.makeDefault() },*defaultApps) },
            restoreAssistant={ shizukuOr({ assistant.restore() },*defaultApps) },
            setupAll={ shizukuOr({
                assistant.grantSetup()
            }) },
            chooseAssistant={ open(*defaultApps) },
            linkGoogle={ shizukuOr({ assistant.linkGoogle() }) },
            copyCrash={ report ->
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("StashDrop crash report",report))
                message="Crash report copied"
            },
            dismissCrash={ com.vibe.stashdrop.core.diagnostics.CrashReporter.clear(this); refreshSetup() },
            messageShown={ message="" },
        )
        setContent { StashDropTheme {
            val items by stashApp.repository.items.collectAsStateWithLifecycle(emptyList())
            val settings by stashApp.preferences.settings.collectAsStateWithLifecycle(com.vibe.stashdrop.features.stashshelf.StashSettings())
            val shizuku by stashApp.shizuku.state.collectAsStateWithLifecycle()
            val running by OneUiThumbDockService.running.collectAsStateWithLifecycle()
            val clipboardStatus by OneUiThumbDockService.clipboardStatus.collectAsStateWithLifecycle()
            DashboardScreen(items,settings,shizuku,running,setup,message,clipboardStatus,actions,mediaFile=stashApp.repository::file)
        } }
    }
}
