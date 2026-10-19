package com.vibe.stashdrop.features.stashshelf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vibe.stashdrop.stashApp
import kotlinx.coroutines.launch

class ReceiveShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            runCatching {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let { stashApp.repository.addText(it) }
                val uris = mutableListOf<Uri>()
                if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    @Suppress("DEPRECATION") val values = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    uris.addAll(values.orEmpty())
                } else {
                    @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    uri?.let(uris::add)
                }
                uris.distinct().take(30).forEach { stashApp.repository.importUri(it) }
            }.onSuccess { Toast.makeText(this@ReceiveShareActivity, "Saved to your shelf", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this@ReceiveShareActivity, it.message, Toast.LENGTH_LONG).show() }
            finish()
        }
    }
}
