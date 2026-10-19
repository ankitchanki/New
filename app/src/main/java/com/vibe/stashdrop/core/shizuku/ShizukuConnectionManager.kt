package com.vibe.stashdrop.core.shizuku

import android.content.*
import android.content.pm.PackageManager
import android.os.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuConnectionManager(private val context: Context) {
    data class State(val running: Boolean = false, val granted: Boolean = false, val connected: Boolean = false, val message: String = "Shizuku is not running")
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    @Volatile var service: IPrivilegedService? = null; private set
    private var binding = false
    private val args = Shizuku.UserServiceArgs(ComponentName(context, ShellUserService::class.java))
        .daemon(false).processNameSuffix("privileged").debuggable(false).version(2)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IPrivilegedService.Stub.asInterface(binder); binding = false
            mutable.value = State(true, true, true, "Shizuku shell bridge connected")
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null; binding = false; refresh() }
    }
    private val received = Shizuku.OnBinderReceivedListener { refresh() }
    private val dead = Shizuku.OnBinderDeadListener { service = null; binding = false; mutable.value = State() }
    private val permission = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }
    fun start() {
        Shizuku.addBinderReceivedListenerSticky(received); Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission); refresh()
    }
    fun refresh() {
        runCatching {
            val running = Shizuku.pingBinder()
            val granted = running && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            mutable.value = State(running, granted, service != null, when { !running -> "Start Shizuku first"; !granted -> "Shizuku permission required"; service != null -> "Shizuku shell bridge connected"; else -> "Connecting shell bridge" })
            if (granted && service == null && !binding) { binding = true; Shizuku.bindUserService(args, connection) }
        }.onFailure { binding = false; mutable.value = State(message = it.message ?: "Shizuku unavailable") }
    }
    fun requestPermission() { runCatching { if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(101) else refresh() }.onFailure { mutable.value = State(message = it.message.orEmpty()) } }
}
