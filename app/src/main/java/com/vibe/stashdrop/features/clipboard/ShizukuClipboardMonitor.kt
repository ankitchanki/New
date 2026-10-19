package com.vibe.stashdrop.features.clipboard

import android.content.*
import com.vibe.stashdrop.core.shizuku.*
import com.vibe.stashdrop.features.stashshelf.StashRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Disabled by default: clipboard content may contain passwords or one-time codes. */
class ShizukuClipboardMonitor(context: Context, private val manager: ShizukuConnectionManager, private val repository: StashRepository,
    private val scope: CoroutineScope) {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    @Volatile private var enabled = false
    private var previous: String? = null
    private val mutable = MutableStateFlow("Auto-stash is off")
    val status = mutable.asStateFlow()
    private val local = ClipboardManager.OnPrimaryClipChangedListener {
        runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull()?.let(::receive)
    }
    private val remote = object : IClipboardCallback.Stub() { override fun onChanged(text: String?) { text?.let(::receive) } }
    @Synchronized private fun receive(text: String) {
        if (!enabled || text.isBlank() || text == previous) return
        previous = text
        scope.launch { runCatching { repository.addText(text, true) }.onFailure { mutable.value = it.message.orEmpty() } }
    }
    private var remoteService: IPrivilegedService? = null
    private var connectionJob: Job? = null
    fun start() {
        if (enabled) return
        enabled = true; clipboard.addPrimaryClipChangedListener(local)
        connectionJob = scope.launch {
            manager.state.collectLatest { state ->
                withContext(Dispatchers.IO) {
                    runCatching { remoteService?.stopClipboard() }; remoteService = null
                    if (state.connected) runCatching {
                        manager.service?.let { it.startClipboard(remote); remoteService = it }
                        mutable.value = "Shell clipboard listener registered; OEM access rules still apply"
                    }.onFailure { mutable.value = "Background listener denied; foreground copy remains available" }
                    else mutable.value = "Foreground clipboard only; connect Shizuku for background listening"
                }
            }
        }
    }
    fun stop() {
        enabled = false; connectionJob?.cancel(); connectionJob = null
        clipboard.removePrimaryClipChangedListener(local)
        val service = remoteService; remoteService = null
        // Cleanup must outlive the LifecycleService's canceled scope.
        if(service != null) Thread { runCatching { service.stopClipboard() } }.start()
        previous = null; mutable.value = "Auto-stash is off"
    }
}
