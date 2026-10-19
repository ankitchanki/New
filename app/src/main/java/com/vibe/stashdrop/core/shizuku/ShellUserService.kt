package com.vibe.stashdrop.core.shizuku

import android.content.ClipData
import android.os.*
import com.vibe.stashdrop.core.hiddenapi.HiddenApiBypassInitializer
import java.io.ByteArrayOutputStream

/** Runs under Shizuku's shell identity. No caller-supplied shell commands are accepted. */
class ShellUserService : IPrivilegedService.Stub() {
    companion object {
        const val OWN_PACKAGE = "com.vibe.stashdrop"
        const val ACCESSIBILITY = "$OWN_PACKAGE/$OWN_PACKAGE.core.shizuku.CaptureAccessibilityService"
        const val ASSIST_ACTIVITY = "$OWN_PACKAGE/$OWN_PACKAGE.features.circletosearch.AssistActivity"
    }
    init { HiddenApiBypassInitializer.initialize() }
    private fun command(vararg args: String): String {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { text.take(400) }
        return text.trim()
    }
    private val assistantRole = "android.app.role.ASSISTANT"
    private val packageName = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    override fun getAssistantRole(userId: Int): String = runCatching {
        command("/system/bin/cmd", "role", "get-role-holders", "--user", userId.toString(), assistantRole).lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    }.getOrElse { "Error: ${it.message}" }
    /** Hands the assistant role to [target] (validated package name), or clears it when empty. */
    override fun setAssistantRole(target: String?, userId: Int): String = runCatching {
        val user = userId.toString()
        if (target.isNullOrEmpty()) command("/system/bin/cmd", "role", "clear-role-holders", "--user", user, assistantRole)
        else {
            require(packageName.matches(target)) { "Invalid package" }
            command("/system/bin/cmd", "role", "add-role-holder", "--user", user, assistantRole, target)
        }
        check(getAssistantRole(userId) == target.orEmpty()) { "The system did not accept the assistant change" }
        "Digital assistant updated"
    }.getOrElse { "Error: ${it.message}" }
    /**
     * Assistant presses (corner swipe, long-press home) open StashDrop's assist screen while Google's voice service stays
     * active, so StashDrop can hand them to Google Circle to Search. Only these two fixed values are accepted.
     */
    override fun routeAssistantToStashDrop(googleVoiceService: String?, userId: Int): String = runCatching {
        val service = googleVoiceService.orEmpty()
        require(service.startsWith("com.google.android.googlequicksearchbox/") && packageName.matches(service.substringAfter('/').removePrefix("."))) { "Not a Google voice service" }
        val user = userId.toString()
        command("/system/bin/settings", "--user", user, "put", "secure", "voice_interaction_service", service)
        command("/system/bin/settings", "--user", user, "put", "secure", "assistant", ASSIST_ACTIVITY)
        "Corner swipe and long-press home now open Google Circle to Search through StashDrop"
    }.getOrElse { "Error: ${it.message}" }
    /** Grants StashDrop's own overlay, notification, and screenshot-accessibility access. */
    override fun grantSetup(userId: Int): String {
        val user = userId.toString()
        val done = mutableListOf<String>()
        runCatching { command("/system/bin/appops", "set", "--user", user, OWN_PACKAGE, "SYSTEM_ALERT_WINDOW", "allow"); done += "floating shelf" }
        if (Build.VERSION.SDK_INT >= 33) runCatching { command("/system/bin/pm", "grant", "--user", user, OWN_PACKAGE, "android.permission.POST_NOTIFICATIONS"); done += "notifications" }
        runCatching {
            val current = command("/system/bin/settings", "--user", user, "get", "secure", "enabled_accessibility_services").takeUnless { it == "null" }.orEmpty()
            val services = current.split(':').filter { it.isNotBlank() }
            if (ACCESSIBILITY !in services) command("/system/bin/settings", "--user", user, "put", "secure", "enabled_accessibility_services", (services + ACCESSIBILITY).joinToString(":"))
            command("/system/bin/settings", "--user", user, "put", "secure", "accessibility_enabled", "1")
            done += "screenshot access"
        }
        return if (done.isEmpty()) "Error: Shizuku could not change any setting" else "Enabled " + done.joinToString(", ")
    }
    private fun clipboard(): Any {
        val manager = Class.forName("android.os.ServiceManager")
        val binder = manager.getMethod("getService", String::class.java).invoke(null, "clipboard") as IBinder
        return Class.forName("android.content.IClipboard\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
    }
    private fun arguments(types: Array<Class<*>>, listener: Any? = null): Array<Any?> {
        var stringIndex = 0; var intIndex = 0
        return types.map { t -> when {
            t == String::class.java -> if (stringIndex++ == 0) "com.android.shell" else null
            t == Int::class.javaPrimitiveType -> if (intIndex++ == 0) 0 else 0
            t == Boolean::class.javaPrimitiveType -> false
            t.name == "android.content.IOnPrimaryClipChangedListener" -> listener
            else -> null
        } }.toTypedArray()
    }
    override fun readClipboard(): String? = runCatching {
        val service = clipboard()
        val method = Class.forName("android.content.IClipboard").methods.first { it.name == "getPrimaryClip" }
        val clip = method.invoke(service, *arguments(method.parameterTypes)) as? ClipData
        clip?.getItemAt(0)?.text?.toString()?.take(100_000)
    }.getOrNull()
    private var remoteListener: Any? = null
    @Volatile private var callback: IClipboardCallback? = null
    override fun startClipboard(callback: IClipboardCallback?) {
        stopClipboard(); this.callback = callback
        val binder = object : Binder() {
            init { attachInterface(null, "android.content.IOnPrimaryClipChangedListener") }
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                if (code == IBinder.FIRST_CALL_TRANSACTION) {
                    data.enforceInterface("android.content.IOnPrimaryClipChangedListener")
                    runCatching { this@ShellUserService.callback?.onChanged(readClipboard()) }
                    reply?.writeNoException(); return true
                }
                return super.onTransact(code, data, reply, flags)
            }
        }
        val stub = Class.forName("android.content.IOnPrimaryClipChangedListener\$Stub")
        val listener = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        val service = clipboard()
        val method = Class.forName("android.content.IClipboard").methods.first { it.name == "addPrimaryClipChangedListener" }
        method.invoke(service, *arguments(method.parameterTypes, listener)); remoteListener = listener
    }
    override fun stopClipboard() {
        remoteListener?.let { listener -> runCatching {
            val service = clipboard(); val method = Class.forName("android.content.IClipboard").methods.first { it.name == "removePrimaryClipChangedListener" }
            method.invoke(service, *arguments(method.parameterTypes, listener))
        } }; remoteListener = null; callback = null
    }
    override fun captureToPipe(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            var process: java.lang.Process? = null
            try {
                val capture = ProcessBuilder("/system/bin/screencap", "-p").start()
                process = capture
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output -> capture.inputStream.use { it.copyTo(output) } }
                capture.waitFor()
            } catch (_: Exception) { runCatching { pipe[1].close() } }
            finally { process?.destroy() }
        }.start()
        return pipe[0]
    }
    override fun destroy() { stopClipboard(); Process.killProcess(Process.myPid()) }
}
