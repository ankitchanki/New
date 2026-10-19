package com.vibe.stashdrop.features.circletosearch

import android.content.*
import android.widget.Toast
import com.vibe.stashdrop.core.shizuku.*
import com.vibe.stashdrop.stashApp
import kotlinx.coroutines.flow.first

/** Opens Google Circle to Search when enabled and reachable; otherwise StashDrop's built-in circle screen. */
class CircleToSearchOrchestrator(private val context: Context) {
    /** @return true when Google's Circle to Search was requested (the caller should not draw over it). */
    suspend fun launch(): Boolean {
        if (context.stashApp.preferences.settings.first().googleCircle) {
            if (GoogleCircleToSearch.launch(context)) return true
            val reason = when (GoogleCircleToSearch.readiness(context)) {
                GoogleCircleToSearch.Readiness.GOOGLE_MISSING -> "Google app not found"
                GoogleCircleToSearch.Readiness.GOOGLE_NOT_ASSISTANT -> "Connect Google in StashDrop Settings"
                GoogleCircleToSearch.Readiness.READY -> "Google did not respond"
            }
            Toast.makeText(context, "$reason · using StashDrop circle", Toast.LENGTH_SHORT).show()
        }
        launchBuiltIn()
        return false
    }
    suspend fun launchBuiltIn() {
        val captured = runCatching { SurfaceScreenCapturer(context).capture() }
        if (captured.isSuccess) {
            CapturedFrame.put(captured.getOrThrow())
            context.startActivity(Intent(context, CircleSearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(context, CaptureConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
