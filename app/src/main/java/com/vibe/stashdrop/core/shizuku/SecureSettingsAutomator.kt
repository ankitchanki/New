package com.vibe.stashdrop.core.shizuku

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureSettingsAutomator(private val context: Context, private val manager: ShizukuConnectionManager) {
    private val backup = context.getSharedPreferences("assistant-backup", Context.MODE_PRIVATE)
    suspend fun enableGoogleAssistant(): String = withContext(Dispatchers.IO) {
        val service = manager.service ?: return@withContext "Shizuku bridge is not connected"
        val google = android.content.ComponentName("com.google.android.googlequicksearchbox", "com.google.android.voiceinteraction.GsaVoiceInteractionService")
        @Suppress("DEPRECATION")
        val installed = runCatching { context.packageManager.getServiceInfo(google, 0) }.isSuccess
        if (!installed) return@withContext "Google voice interaction service is not installed"
        if (!backup.contains("saved")) backup.edit().putBoolean("saved", true)
            .putString("assistant", Settings.Secure.getString(context.contentResolver, "assistant"))
            .putString("voice", Settings.Secure.getString(context.contentResolver, "voice_interaction_service")).commit()
        service.setAssistant("com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService",
            "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService")
    }
    suspend fun restore(): String = withContext(Dispatchers.IO) {
        if (!backup.contains("saved")) return@withContext "No previous assistant settings saved"
        val service = manager.service ?: return@withContext "Shizuku bridge is not connected"
        val result = service.setAssistant(backup.getString("assistant", null), backup.getString("voice", null))
        if (!result.startsWith("Error:")) backup.edit().clear().commit()
        result
    }
}
