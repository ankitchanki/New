package com.vibe.stashdrop.features.circletosearch

import android.content.*
import com.vibe.stashdrop.core.shizuku.*
import com.vibe.stashdrop.stashApp
import kotlinx.coroutines.*

class CircleToSearchOrchestrator(private val context: Context) {
    suspend fun launch(builtIn: Boolean = false) {
        if (!builtIn && context.stashApp.shizuku.state.value.granted) {
            val result = withContext(Dispatchers.IO) { PrivilegedSystemBridge().launchNative() }
            if (result.isSuccess) return
        }
        val captured = runCatching { SurfaceScreenCapturer(context).capture() }
        if (captured.isSuccess) {
            CapturedFrame.put(captured.getOrThrow())
            context.startActivity(Intent(context, CircleStudioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(context, CaptureConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
