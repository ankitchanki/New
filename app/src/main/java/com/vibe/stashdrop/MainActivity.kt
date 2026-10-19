package com.vibe.stashdrop

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
        val accessibility=getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName==packageName }
        setup=SetupState(overlay=Settings.canDrawOverlays(this),
            notifications=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED,
            accessibility=accessibility,assistant=assistant.isDefault(),assistantSupported=assistant.supported)
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
    private val defaultApps get()=arrayOf(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),Intent(Settings.ACTION_SETTINGS))
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
            delete={item->lifecycleScope.launch { stashApp.repository.delete(item) }},
            share={selected,whatsapp->runCatching { VisualLensBridge.share(this,stashApp.repository,selected,whatsapp) }.onFailure { message=it.message.orEmpty() }},
            collect={item,collection->lifecycleScope.launch { stashApp.repository.move(item,collection) }},
            allowOverlay={ shizukuOr({ assistant.grantSetup() },overlaySettings) },
            allowNotifications={
                if(stashApp.shizuku.service!=null) shizukuOr({ assistant.grantSetup() })
                else if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else open(notificationSettings)
            },
            enableAccessibility={ shizukuOr({ assistant.grantSetup() },Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            makeAssistant={ shizukuOr({ assistant.makeDefault() },*defaultApps) },
            restoreAssistant={ shizukuOr({ assistant.restore() },*defaultApps) },
            setupAll={ shizukuOr({
                val granted=assistant.grantSetup()
                val role=if(assistant.supported) assistant.makeDefault() else null
                listOfNotNull(granted,role).joinToString(" · ")
            }) },
            messageShown={ message="" },
        )
        setContent { StashDropTheme {
            val items by stashApp.repository.items.collectAsStateWithLifecycle(emptyList())
            val settings by stashApp.preferences.settings.collectAsStateWithLifecycle(com.vibe.stashdrop.features.stashshelf.StashSettings())
            val shizuku by stashApp.shizuku.state.collectAsStateWithLifecycle()
            val running by OneUiThumbDockService.running.collectAsStateWithLifecycle()
            val clipboardStatus by OneUiThumbDockService.clipboardStatus.collectAsStateWithLifecycle()
            DashboardScreen(items,settings,shizuku,running,setup,message,clipboardStatus,actions)
        } }
    }
}
