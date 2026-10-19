package com.vibe.stashdrop.core.shizuku

import android.content.ClipData
import android.os.*
import com.vibe.stashdrop.core.hiddenapi.HiddenApiBypassInitializer
import java.io.ByteArrayOutputStream

/** Runs under Shizuku's shell identity. No caller-supplied shell commands are accepted. */
class ShellUserService : IPrivilegedService.Stub() {
    init { HiddenApiBypassInitializer.initialize() }
    private fun command(vararg args: String): String {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { text.take(400) }
        return text.trim()
    }
    override fun setAssistant(assistant: String?, voice: String?): String = runCatching {
        val keys=listOf("assistant", "voice_interaction_service")
        val previous=keys.map { command("/system/bin/settings", "get", "secure", it).takeUnless { value -> value == "null" } }
        fun write(key: String,value: String?) {
            if(value.isNullOrEmpty()) command("/system/bin/settings", "delete", "secure", key)
            else command("/system/bin/settings", "put", "secure", key, value)
        }
        try {
            keys.zip(listOf(assistant,voice)).forEach { (key,value) -> write(key,value) }
            val actual=keys.map { command("/system/bin/settings", "get", "secure", it).takeUnless { value -> value == "null" } }
            check(actual == listOf(assistant?.ifEmpty { null },voice?.ifEmpty { null })) { "System did not retain assistant settings" }
        } catch(e: Exception) {
            keys.zip(previous).forEach { (key,value) -> runCatching { write(key,value) } }
            throw e
        }
        "Assistant settings saved"
    }.getOrElse { "Error: ${it.message}" }
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
