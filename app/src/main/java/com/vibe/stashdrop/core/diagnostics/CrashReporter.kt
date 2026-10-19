package com.vibe.stashdrop.core.diagnostics

import android.content.Context
import android.os.Build
import java.io.File

/** Keeps the last crash (or caught failure) on the device so it can be shown and copied from the app. */
object CrashReporter {
    private fun file(context: Context) = File(context.applicationContext.filesDir, "last-crash.txt")
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, "Crash on thread ${thread.name}", error) }
            previous?.uncaughtException(thread, error)
        }
    }
    /** Records a failure that was caught and survived, so it is still visible in the app. */
    fun record(context: Context, error: Throwable, where: String = "Handled error") { runCatching { write(context, where, error) } }
    private fun write(context: Context, title: String, error: Throwable) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        file(context).writeText(buildString {
            appendLine("$title · StashDrop ${info.versionName}")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            append(error.stackTraceToString().take(12_000))
        })
    }
    fun read(context: Context): String? = file(context).takeIf { it.isFile }?.readText()
    fun clear(context: Context) { file(context).delete() }
}
