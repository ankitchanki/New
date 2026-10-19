package com.vibe.stashdrop.features.circletosearch

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.vibe.stashdrop.core.shizuku.ProjectionCaptureService

class CaptureConsentActivity : ComponentActivity() {
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) ContextCompat.startForegroundService(this,
            Intent(this, ProjectionCaptureService::class.java).putExtra("data", result.data))
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); if (savedInstanceState == null) consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()) }
}
