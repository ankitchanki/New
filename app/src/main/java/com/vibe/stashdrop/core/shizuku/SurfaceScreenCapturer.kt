package com.vibe.stashdrop.core.shizuku

import android.graphics.*
import android.os.*
import android.accessibilityservice.AccessibilityService
import android.view.Display
import com.vibe.stashdrop.stashApp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CapturedFrame {
    private val bitmap = AtomicReference<Bitmap?>()
    // Never recycle here: an open Circle to Search screen may still be drawing the previous frame.
    fun put(value: Bitmap) { bitmap.set(value) }
    fun take(): Bitmap? = bitmap.getAndSet(null)
}
class SurfaceScreenCapturer(private val context: android.content.Context) {
    suspend fun capture(): Bitmap = withContext(Dispatchers.IO) {
        val shell = context.stashApp.shizuku.service
        if (shell != null) {
            val result = runCatching {
                val bytes = withTimeout(4000) {
                    suspendCancellableCoroutine<ByteArray> { continuation ->
                        val input = ParcelFileDescriptor.AutoCloseInputStream(shell.captureToPipe())
                        continuation.invokeOnCancellation { runCatching { input.close() } }
                        Thread {
                            val read = runCatching {
                                input.use {
                                    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(65536)
                                    while (true) {
                                        val n = it.read(buffer); if (n < 0) break
                                        check(output.size() + n <= 32 * 1024 * 1024) { "Screenshot too large" }
                                        output.write(buffer, 0, n)
                                    }
                                    output.toByteArray()
                                }
                            }
                            if (continuation.isActive) continuation.resumeWith(read)
                        }.apply { isDaemon = true; name = "screenshot-reader" }.start()
                    }
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Shell screenshot unavailable")
            }
            if (result.isSuccess) return@withContext result.getOrThrow()
        }
        val accessibility = CaptureAccessibilityService.current
        if (Build.VERSION.SDK_INT >= 30 && accessibility != null) return@withContext suspendCancellableCoroutine { continuation ->
            accessibility.takeScreenshot(Display.DEFAULT_DISPLAY, context.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val hardware = result.hardwareBuffer
                    val bitmap = try { Bitmap.wrapHardwareBuffer(hardware, result.colorSpace)?.let { source ->
                        source.copy(Bitmap.Config.ARGB_8888, false).also { source.recycle() }
                    } } finally { hardware.close() }
                    if (continuation.isActive) {
                        if (bitmap != null) continuation.resume(bitmap) else continuation.resumeWithException(IllegalStateException("Screenshot buffer unavailable"))
                    } else bitmap?.recycle()
                }
                override fun onFailure(errorCode: Int) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Screenshot denied ($errorCode); protected windows cannot be captured")) }
            })
        }
        error("Screen capture needs Shizuku, accessibility screenshot access, or screen-sharing consent")
    }
}
