package com.vibe.stashdrop.features.circletosearch

import android.content.*
import com.vibe.stashdrop.core.shizuku.*

/** Captures the screen and opens StashDrop's Circle to Search; never hands off to Gemini or the system assistant. */
class CircleToSearchOrchestrator(private val context: Context) {
    suspend fun launch() {
        val captured = runCatching { SurfaceScreenCapturer(context).capture() }
        if (captured.isSuccess) {
            CapturedFrame.put(captured.getOrThrow())
            context.startActivity(Intent(context, CircleSearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(context, CaptureConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
