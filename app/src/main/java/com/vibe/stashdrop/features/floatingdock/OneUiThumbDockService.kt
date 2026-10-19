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
        const val SEARCH="search"
    }
    private val saved=SavedStateRegistryController.create(this)
    override val savedStateRegistry get()=saved.savedStateRegistry
    override val viewModelStore=ViewModelStore()
    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private val density get()=resources.displayMetrics.density
    private val haptics by lazy { com.vibe.stashdrop.core.haptics.TactileHapticEngine(this) }
    /** Every overlay callback runs guarded: a failure is recorded and shown instead of closing the app. */
    private fun safely(where: String, block: ()->Unit) { runCatching(block).onFailure { report(where,it) } }
    private fun report(where: String, error: Throwable) {
        com.vibe.stashdrop.core.diagnostics.CrashReporter.record(this,error,"Floating bubble: $where")
        toast("$where failed: ${error.message ?: error.javaClass.simpleName}")
    }
    private val guard=CoroutineExceptionHandler { _,error -> report("Background task",error) }
    /** Compose animations need a frame clock; AndroidUiDispatcher supplies one tied to the Choreographer. */
    private val ui get()=androidx.compose.ui.platform.AndroidUiDispatcher.Main+guard
    private lateinit var dock: ComposeView
    private var sheet: View?=null
    private var dismiss: ComposeView?=null
    private var dismissShown by mutableStateOf(false)
    private var dismissHot by mutableStateOf(false)
    private var freeX=0f
    private var freeY=0f
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
        override fun onReceive(context: Context,intent: Intent) = safely("Screen change") {
            if(intent.action==Intent.ACTION_SCREEN_OFF) {
                hideSheet(); bar.close(); dock.visibility=View.INVISIBLE; wave.visibility=View.INVISIBLE
            } else if(intent.action==Intent.ACTION_USER_PRESENT ||
                (intent.action==Intent.ACTION_SCREEN_ON && !getSystemService(KeyguardManager::class.java).isKeyguardLocked)) {
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
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
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
        // An activity target collapses the notification shade before the screenshot.
        val search=PendingIntent.getActivity(this,2,Intent(this,AssistActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(this,"dock").setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("StashDrop shelf is active").setContentText("Hold the bubble or swipe up to Circle to Search")
            .setContentIntent(open).addAction(android.R.drawable.ic_menu_search,"Circle to Search",search)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",stop).setOngoing(true).build()
        val started=runCatching {
            androidx.core.app.ServiceCompat.startForeground(this,1,notification,
                if(Build.VERSION.SDK_INT>=34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        }
        if(started.isFailure) { report("Starting the bubble",started.exceptionOrNull()!!); stopSelf(); return }
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
                setContent { StashDropTheme { ThumbDockIconComposable(stash.size,absorbing,{ safely("Opening the shelf") { haptics.tick(); showSheet() } },{ safely("Circle to Search",::launchSearch) },
                    { amount -> safely("Moving the bubble") { dragDock(amount) } },{ safely("Moving the bubble",::endDockDrag) }) } }
                setOnDragListener { _,event ->
                    when(event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> { absorbing=true; true }
                        DragEvent.ACTION_DRAG_ENTERED -> { absorbing=true; haptics.tick(.5f); true }
                        DragEvent.ACTION_DRAG_EXITED -> { absorbing=false; true }
                        DragEvent.ACTION_DROP -> {
                            val clip=event.clipData
                            if ((0 until clip.itemCount).any { clip.getItemAt(it).uri != null }) {
                                toast("Share files to StashDrop to grant access; overlay drops support text")
                                return@setOnDragListener false
                            }
                            haptics.confirm()
                            lifecycleScope.launch(ui) {
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
            // Added before the dock so the bubble stays above it.
            dismiss=compose().apply { setContent { DismissTarget(dismissShown,dismissHot) } }
            windows.addView(dismiss,params(-1,(170*density).toInt(),false).apply { gravity=Gravity.BOTTOM })
            windows.addView(dock,dockParams)
            bar=BottomBarGestureOverlay(this,windows,{p,x->
                waveX=x
                val velocity=waveProgress.velocity
                waveJob?.cancel()
                waveJob=lifecycleScope.launch(ui) { waveProgress.animateTo(p,IosSpringSpecs.follow(),initialVelocity=velocity) }
            },{ safely("Circle to Search",::launchSearch) })
            clipboard=ShizukuClipboardMonitor(this,stashApp.shizuku,stashApp.repository,lifecycleScope)
            lifecycleScope.launch(ui) { clipboard.status.collect { clipboardStatus.value=it } }
            lifecycleScope.launch(ui) { stashApp.repository.items.collect { stash=it } }
            lifecycleScope.launch(ui) { stashApp.preferences.settings.collect { value -> settings=value; configureBar(); if(value.autoStash) clipboard.start() else clipboard.stop() } }
            androidx.core.content.ContextCompat.registerReceiver(this,screenState,
                IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) },
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered=true
            running.value=true; schedulePeek()
        } catch(e: Exception) { toast(e.message ?: "Overlay unavailable"); stopSelf() }
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        super.onStartCommand(intent,flags,startId)
        if(intent?.action==STOP) stopSelf()
        else if(intent?.action==SEARCH && ::dock.isInitialized) safely("Circle to Search",::launchSearch)
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
    private fun launchSearch() {
        if(searchJob?.isActive==true) return
        haptics.confirm(); hideSheet(); idleJob?.cancel()
        searchJob=lifecycleScope.launch(ui) {
            var google=false
            try {
                val velocity=waveProgress.velocity
                waveJob?.cancel()
                waveJob=launch { waveProgress.animateTo(1f,IosSpringSpecs.follow(),initialVelocity=velocity) }
                delay(220)
                waveJob?.cancel(); waveProgress.snapTo(0f)
                dock.visibility=View.INVISIBLE; wave.visibility=View.INVISIBLE
                delay(80) // Let SurfaceFlinger remove our overlay before screenshot acquisition.
                google=CircleToSearchOrchestrator(this@OneUiThumbDockService).launch()
                if(google) delay(900) // Keep the bubble out of Google's screenshot.
            } catch(e: CancellationException) { throw e
            } catch(e: Exception) { report("Circle to Search",e) }
            finally { dock.visibility=View.VISIBLE; wave.visibility=View.VISIBLE; schedulePeek() }
        }
    }
    private fun dragDock(amount: Offset) {
        idleJob?.cancel(); snapJob?.cancel(); peeked=false
        val now=SystemClock.uptimeMillis()
        if(lastDragTime>0) snapVelocity=(amount.x*1000f/(now-lastDragTime).coerceAtLeast(1)).coerceIn(-5000f,5000f)
        lastDragTime=now
        if(dragged==Offset.Zero) { haptics.reset(); freeX=dockParams.x.toFloat(); freeY=dockParams.y.toFloat(); dismissShown=true }
        haptics.travel(amount.getDistance()/density)
        dragged+=amount; freeX+=amount.x; freeY+=amount.y
        // Magnet: near the dismiss target the bubble snaps onto it, like a chat head.
        val target=dismissCenter()
        val hot=target!=null && kotlin.math.hypot(freeX+dockParams.width/2-target.x,freeY+dockParams.height/2-target.y)<84*density
        if(hot!=dismissHot) { dismissHot=hot; if(hot) haptics.select() else haptics.tick() }
        if(hot && target!=null) { dockParams.x=(target.x-dockParams.width/2).toInt(); dockParams.y=(target.y-dockParams.height/2).toInt() }
        else { dockParams.x=freeX.toInt(); dockParams.y=freeY.toInt() }
        runCatching { windows.updateViewLayout(dock,dockParams) }.onFailure { stopSelf() }
    }
    /** Repositioning only: the dock snaps to the nearest edge and never launches anything on release. */
    private fun dismissCenter(): Offset? {
        val view=dismiss?.takeIf { it.isAttachedToWindow && it.height>0 } ?: return null
        val location=IntArray(2).also(view::getLocationOnScreen)
        return Offset(location[0]+view.width/2f,location[1]+view.height/2f)
    }
    private fun endDockDrag() {
        dismissShown=false
        if(dismissHot) {
            dismissHot=false; haptics.confirm()
            toast("Bubble hidden · turn it on again in StashDrop")
            stopSelf(); return
        }
        val moved=dragged!=Offset.Zero
        dragged=Offset.Zero; lastDragTime=0L
        left=dockParams.x+ dockParams.width/2 < resources.displayMetrics.widthPixels/2
        val metrics=resources.displayMetrics
        dockParams.y=dockParams.y.coerceIn((metrics.heightPixels*.12f).toInt(),(metrics.heightPixels*.85f).toInt())
        springX(if(left) 0 else metrics.widthPixels-dockParams.width)
        if(moved) haptics.tick(.6f)
        schedulePeek()
    }
    private fun springX(target: Int) {
        val initialVelocity=snapVelocity
        snapJob?.cancel(); snapJob=lifecycleScope.launch(ui) {
            Animatable(dockParams.x.toFloat()).animateTo(target.toFloat(),IosSpringSpecs.follow(),initialVelocity=initialVelocity) {
                snapVelocity=velocity
                dockParams.x=value.toInt(); if(dock.isAttachedToWindow) windows.updateViewLayout(dock,dockParams)
            }
        }
    }
    private fun schedulePeek() {
        idleJob?.cancel(); idleJob=lifecycleScope.launch(ui) { delay(3000); if(sheet==null) {
            peeked=true
            springX(if(left) (-dockParams.width*.6f).toInt() else (resources.displayMetrics.widthPixels-dockParams.width*.4f).toInt())
        } }
    }
    private fun showSheet() {
        if(peeked) { peeked=false; springX(if(left) 0 else resources.displayMetrics.widthPixels-dockParams.width) }
        idleJob?.cancel(); if(sheet!=null) { hideSheet(); return }
        val root=compose()
        // Focusable host so Back closes the shelf, like a normal sheet.
        val host=object: android.widget.FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if(event.keyCode==KeyEvent.KEYCODE_BACK) { if(event.action==KeyEvent.ACTION_UP) hideSheet(); return true }
                return super.dispatchKeyEvent(event)
            }
        }
        // Compose resolves its lifecycle from the window's root view.
        host.setViewTreeLifecycleOwner(this); host.setViewTreeSavedStateRegistryOwner(this); host.setViewTreeViewModelStoreOwner(this)
        host.addView(root)
        root.setContent { StashDropTheme { IosFanOutStashSheet(stash,stashApp.repository,::hideSheet,{ safely("Circle to Search",::launchSearch) },
            {items,whatsapp->runCatching { VisualLensBridge.share(this,stashApp.repository,items,whatsapp); hideSheet() }.onFailure { toast(it.message.orEmpty()) }},
            {item->lifecycleScope.launch(ui) { stashApp.repository.delete(item) }},
            {view,items->safely("Dragging cards") { startGlobalDrag(host,view,items) }}) } }
        val layout=params(-1,-1).apply { flags=flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() }
        windows.addView(host,layout); sheet=host
    }
    private fun startGlobalDrag(root: View,source: View,items: List<StashItemEntity>) {
        root.setOnDragListener { _,event ->
            if(event.action==DragEvent.ACTION_DRAG_ENDED) {
                if(event.result) haptics.confirm() else toast("Drop was not accepted. Your items remain on the shelf.")
                hideSheet()
            }
            true
        }
        val success=runCatching { source.startDragAndDrop(stashApp.repository.clip(items),com.vibe.stashdrop.features.dragdrop.IosCardStackDragShadow(source,items,stashApp.repository),null,
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ) }.getOrDefault(false)
        if(success) {
            root.alpha=0f
            val layout=root.layoutParams as WindowManager.LayoutParams
            layout.flags=layout.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
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
        dismiss?.let { runCatching { windows.removeViewImmediate(it) } }
        viewModelStore.clear(); super.onDestroy()
    }
}
