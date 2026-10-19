package com.vibe.stashdrop.features.circletosearch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.features.floatingdock.OneUiThumbDockService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Entry point when StashDrop is the default digital assistant (long-press home or power). Invisible; opens Circle to Search. */
class AssistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) { finish(); return }
        lifecycleScope.launch {
            delay(300) // Let the assist gesture animation or notification shade settle before capturing.
            runCatching {
                // The floating shelf hides its own overlays before capturing.
                if (OneUiThumbDockService.running.value) startService(Intent(this@AssistActivity, OneUiThumbDockService::class.java).setAction(OneUiThumbDockService.SEARCH))
                else { TactileHapticEngine(this@AssistActivity).confirm(); CircleToSearchOrchestrator(this@AssistActivity).launch() }
            }
                .onFailure { Toast.makeText(this@AssistActivity, it.message ?: "Circle to Search unavailable", Toast.LENGTH_LONG).show() }
            finish()
        }
    }
}
