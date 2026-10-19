package com.vibe.stashdrop.features.floatingdock

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.vibe.stashdrop.MainActivity
import com.vibe.stashdrop.core.physics.IosSpringSpecs
import com.vibe.stashdrop.core.shaders.AuroraWave
import com.vibe.stashdrop.features.circletosearch.*
import com.vibe.stashdrop.features.clipboard.ShizukuClipboardMonitor
import com.vibe.stashdrop.features.stashshelf.*
import com.vibe.stashdrop.stashApp
import com.vibe.stashdrop.ui.theme.StashDropTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class OneUiThumbDockService: LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    companion object {
        val running=MutableStateFlow(false)
        val clipboardStatus=MutableStateFlow("Start the floating shelf to monitor the clipboard")
        const val STOP="stop"
    }
    private val saved=SavedStateRegistryController.create(this)
    override val savedStateRegistry get()=saved.savedStateRegistry
    override val viewModelStore=ViewModelStore()
    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private val density get()=resources.displayMetrics.density
    private lateinit var dock: ComposeView
    private var sheet: ComposeView?=null
    private lateinit var wave: ComposeView
    private lateinit var bar: BottomBarGestureOverlay
    private lateinit var clipboard: ShizukuClipboardMonitor
    private var settings=StashSettings()
    private var stash by mutableStateOf(emptyList<StashItemEntity>())
    private var absorbing by mutableStateOf(false)
    private val waveProgress=Animatable(0f)
    private var waveX by mutableFloatStateOf(0f)
    private var waveJob: Job?=null
    private var searchJob: Job?=null
    private var idleJob: Job?=null
    private var snapJob: Job?=null
    private var receiverRegistered=false
    private val screenState=object: BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            if(intent.action==Intent.ACTION_SCREEN_OFF) {
                hideSheet(); bar.close(); dock.visibility=View.INVISIBLE; wave.visibility=View.INVISIBLE
            } else if(intent.action==Intent.ACTION_USER_PRESENT) {
                dock.visibility=View.VISIBLE; wave.visibility=View.VISIBLE; configureBar(); schedulePeek()
            }
        }
    }
    private var peeked=false
    private var left=false
    private var dragged=Offset.Zero
    private var lastDragTime=0L
    private var snapVelocity=0f
    private lateinit var dockParams: WindowManager.LayoutParams
    private fun params(width: Int,height: Int,touchable: Boolean=true)=WindowManager.LayoutParams(width,height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            (if(touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),PixelFormat.TRANSLUCENT).apply {
            // Android 12+ blocks touches through non-touchable overlays above the obscuring-opacity limit.
            alpha=if(touchable) 1f else .7f
            preferredRefreshRate=120f
        }
    private fun compose()=ComposeView(this).also { it.setViewTreeLifecycleOwner(this); it.setViewTreeSavedStateRegistryOwner(this); it.setViewTreeViewModelStoreOwner(this) }
    override fun onCreate() {
        saved.performAttach(); saved.performRestore(null); super.onCreate()
        val notifications=getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("dock","Floating shelf",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,javaClass).setAction(STOP),PendingIntent.FLAG_IMMUTABLE)
        startForeground(1,NotificationCompat.Builder(this,"dock").setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("StashDrop shelf is active").setContentText("Tap to configure · Stop to remove overlays")
            .setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",stop).setOngoing(true).build())
        if(!Settings.canDrawOverlays(this)) { stopSelf(); return }
        try {
            wave=compose().apply { setContent { AuroraWave(waveProgress.value,Offset(waveX,resources.displayMetrics.heightPixels.toFloat()),Modifier.fillMaxSize()) } }
            windows.addView(wave,params(-1,-1,false))
            dockParams=params((88*density).toInt(),(88*density).toInt()).apply {
                gravity=Gravity.TOP or Gravity.LEFT
                x=resources.displayMetrics.widthPixels-width
                y=(resources.displayMetrics.heightPixels*.65f).toInt()
                flags=flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            dock=compose().apply {
                setContent { StashDropTheme { ThumbDockIconComposable(stash.size,absorbing,::showSheet,{launchSearch(false)},::dragDock,::endDockDrag) } }
                setOnDragListener { _,event ->
                    when(event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> { absorbing=true; true }
                        DragEvent.ACTION_DRAG_ENTERED -> { absorbing=true; true }
                        DragEvent.ACTION_DRAG_EXITED -> { absorbing=false; true }
                        DragEvent.ACTION_DROP -> {
                            val clip=event.clipData
                            if ((0 until clip.itemCount).any { clip.getItemAt(it).uri != null }) {
                                toast("Share files to StashDrop to grant access; overlay drops support text")
                                return@setOnDragListener false
                            }
                            lifecycleScope.launch {
                                runCatching {
                                    for(i in 0 until minOf(clip.itemCount,30)) {
                                        val item=clip.getItemAt(i)
                                        item.text?.toString()?.let { stashApp.repository.addText(it) }
                                        // Service overlays cannot request activity drag URI grants; inaccessible URIs fail visibly.
                                        item.uri?.let { stashApp.repository.importUri(it) }
                                    }
                                }.onFailure { toast(it.message ?: "Could not import drop; share to StashDrop instead") }
                            }; true
                        }
                        DragEvent.ACTION_DRAG_ENDED -> { absorbing=false; true }
                        else -> true
                    }
                }
            }
            windows.addView(dock,dockParams)
            bar=BottomBarGestureOverlay(this,windows,{p,x->
                waveX=x
                val velocity=waveProgress.velocity
                waveJob?.cancel()
                waveJob=lifecycleScope.launch { waveProgress.animateTo(p,IosSpringSpecs.follow(),initialVelocity=velocity) }
            },{launchSearch(false)})
            clipboard=ShizukuClipboardMonitor(this,stashApp.shizuku,stashApp.repository,lifecycleScope)
            lifecycleScope.launch { clipboard.status.collect { clipboardStatus.value=it } }
            lifecycleScope.launch { stashApp.repository.items.collect { stash=it } }
            lifecycleScope.launch { stashApp.preferences.settings.collect { value -> settings=value; configureBar(); if(value.autoStash) clipboard.start() else clipboard.stop() } }
            androidx.core.content.ContextCompat.registerReceiver(this,screenState,
                IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) },
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered=true
            running.value=true; schedulePeek()
        } catch(e: Exception) { toast(e.message ?: "Overlay unavailable"); stopSelf() }
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        super.onStartCommand(intent,flags,startId)
        if(intent?.action==STOP) stopSelf()
        return Service.START_NOT_STICKY
    }
    private fun configureBar() {
        if(!::bar.isInitialized) return
        if(!Settings.canDrawOverlays(this)) { bar.close(); stopSelf(); return }
        val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if(settings.gesturesEnabled && resources.configuration.orientation!=Configuration.ORIENTATION_LANDSCAPE && !locked) bar.attach(settings.gestures) else bar.close()
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); configureBar(); if(::dock.isInitialized) endDockDrag() }
    private fun toast(text: String) { Toast.makeText(this,text,Toast.LENGTH_LONG).show() }
    private fun launchSearch(builtIn: Boolean) {
        if(searchJob?.isActive==true) return
        hideSheet(); idleJob?.cancel()
        searchJob=lifecycleScope.launch {
            try {
                val velocity=waveProgress.velocity
                waveJob?.cancel()
                waveJob=launch { waveProgress.animateTo(1f,IosSpringSpecs.follow(),initialVelocity=velocity) }
                delay(220)
                waveJob?.cancel(); waveProgress.snapTo(0f)
                dock.visibility=View.INVISIBLE; wave.visibility=View.INVISIBLE
                delay(80) // Let SurfaceFlinger remove our overlay before screenshot acquisition.
                CircleToSearchOrchestrator(this@OneUiThumbDockService).launch(builtIn)
            } catch(e: Exception) { toast(e.message ?: "Search unavailable") }
            finally { dock.visibility=View.VISIBLE; wave.visibility=View.VISIBLE; schedulePeek() }
        }
    }
    private fun dragDock(amount: Offset) {
        idleJob?.cancel(); snapJob?.cancel(); peeked=false
        val now=SystemClock.uptimeMillis()
        if(lastDragTime>0) snapVelocity=(amount.x*1000f/(now-lastDragTime).coerceAtLeast(1)).coerceIn(-5000f,5000f)
        lastDragTime=now
        dragged+=amount; dockParams.x+=amount.x.toInt(); dockParams.y+=amount.y.toInt()
        runCatching { windows.updateViewLayout(dock,dockParams) }.onFailure { stopSelf() }
    }
    private fun endDockDrag() {
        val inward=if(left) dragged.x else -dragged.x
        val downward=dragged.y
        dragged=Offset.Zero; lastDragTime=0L
        left=dockParams.x+ dockParams.width/2 < resources.displayMetrics.widthPixels/2
        dockParams.y=dockParams.y.coerceIn((resources.displayMetrics.heightPixels*.45f).toInt(),(resources.displayMetrics.heightPixels*.75f).toInt())
        springX(if(left) 0 else resources.displayMetrics.widthPixels-dockParams.width)
        if(inward>64*density) launchSearch(false) else if(downward>72*density) launchSearch(true)
        schedulePeek()
    }
    private fun springX(target: Int) {
        val initialVelocity=snapVelocity
        snapJob?.cancel(); snapJob=lifecycleScope.launch {
            Animatable(dockParams.x.toFloat()).animateTo(target.toFloat(),IosSpringSpecs.follow(),initialVelocity=initialVelocity) {
                snapVelocity=velocity
                dockParams.x=value.toInt(); if(dock.isAttachedToWindow) windows.updateViewLayout(dock,dockParams)
            }
        }
    }
    private fun schedulePeek() {
        idleJob?.cancel(); idleJob=lifecycleScope.launch { delay(3000); if(sheet==null) {
            peeked=true
            springX(if(left) (-dockParams.width*.6f).toInt() else (resources.displayMetrics.widthPixels-dockParams.width*.4f).toInt())
        } }
    }
    private fun showSheet() {
        if(peeked) { peeked=false; springX(if(left) 0 else resources.displayMetrics.widthPixels-dockParams.width) }
        idleJob?.cancel(); if(sheet!=null) { hideSheet(); return }
        val root=compose()
        root.setContent { StashDropTheme { IosFanOutStashSheet(stash,stashApp.repository,::hideSheet,
            {items,whatsapp->runCatching { VisualLensBridge.share(this,stashApp.repository,items,whatsapp); hideSheet() }.onFailure { toast(it.message.orEmpty()) }},
            {item->lifecycleScope.launch { stashApp.repository.delete(item) }},
            {view,items->startGlobalDrag(root,view,items)}) } }
        windows.addView(root,params(-1,-1)); sheet=root
    }
    private fun startGlobalDrag(root: ComposeView,source: View,items: List<StashItemEntity>) {
        root.setOnDragListener { _,event ->
            if(event.action==DragEvent.ACTION_DRAG_ENDED) {
                if(!event.result) toast("Drop was not accepted. Your items remain on the shelf.")
                hideSheet()
            }
            true
        }
        val success=runCatching { source.startDragAndDrop(stashApp.repository.clip(items),com.vibe.stashdrop.features.dragdrop.IosCardStackDragShadow(source,items,stashApp.repository),null,
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ) }.getOrDefault(false)
        if(success) {
            root.alpha=0f
            val layout=root.layoutParams as WindowManager.LayoutParams
            layout.flags=layout.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            layout.alpha=0f
            windows.updateViewLayout(root,layout)
        } else { root.setOnDragListener(null); toast("Drag could not start. Use Share instead.") }
    }
    private fun hideSheet() { sheet?.let { runCatching { windows.removeViewImmediate(it) } }; sheet=null; schedulePeek() }
    override fun onDestroy() {
        running.value=false
        clipboardStatus.value="Floating shelf is paused; clipboard monitoring is stopped"
        if(receiverRegistered) unregisterReceiver(screenState)
        if(::clipboard.isInitialized) clipboard.stop()
        if(::bar.isInitialized) bar.close()
        sheet?.let { runCatching { windows.removeViewImmediate(it) } }
        if(::dock.isInitialized) runCatching { windows.removeViewImmediate(dock) }
        if(::wave.isInitialized) runCatching { windows.removeViewImmediate(wave) }
        viewModelStore.clear(); super.onDestroy()
    }
}
