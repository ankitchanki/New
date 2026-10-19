package com.vibe.stashdrop.features.circletosearch

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens Google's own Circle to Search.
 *
 * The Google app shows Circle to Search when the system voice-interaction service (which must be Google's, i.e.
 * Google is the digital assistant) receives a session request carrying the contextual-search entry point, the same
 * request SystemUI sends on a long-press of the navigation handle. Google decides device eligibility; on phones where
 * it has not enabled Circle to Search it may open its assistant instead.
 */
object GoogleCircleToSearch {
    const val GOOGLE = "com.google.android.googlequicksearchbox"
    private const val VOICE_SERVICE = "voice_interaction_service"
    private const val ENTRY_POINT_KEY = "omni.entry_point"
    private const val ENTRY_POINT_LONG_PRESS_NAV_HANDLE = 1

    enum class Readiness { READY, GOOGLE_NOT_ASSISTANT, GOOGLE_MISSING }

    /** Google's assistant-capable voice-interaction service, if the Google app is installed. */
    fun googleService(context: Context): ComponentName? = runCatching {
        context.packageManager.queryIntentServices(Intent(VoiceInteractionService.SERVICE_INTERFACE).setPackage(GOOGLE), PackageManager.GET_META_DATA)
            .map { it.serviceInfo }
            .firstOrNull { it.permission == Manifest.permission.BIND_VOICE_INTERACTION && it.metaData?.containsKey(VoiceInteractionService.SERVICE_META_DATA) == true }
            ?.let { ComponentName(it.packageName, it.name) }
    }.getOrNull()

    private fun activeService(context: Context): String? = runCatching { Settings.Secure.getString(context.contentResolver, VOICE_SERVICE) }.getOrNull()
    fun readiness(context: Context): Readiness {
        val google = googleService(context) ?: return Readiness.GOOGLE_MISSING
        // When the active service cannot be read, assume Google and let the request decide.
        val active = activeService(context) ?: return Readiness.READY
        return if (active == google.flattenToString()) Readiness.READY else Readiness.GOOGLE_NOT_ASSISTANT
    }

    /** True when the system accepted the request; Google then shows Circle to Search on eligible phones. */
    suspend fun launch(context: Context): Boolean =
        readiness(context) == Readiness.READY && withContext(Dispatchers.IO) { showSession(context) }

    private fun showSession(context: Context): Boolean = runCatching {
        val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as IBinder
        val service = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        val method = Class.forName("com.android.internal.app.IVoiceInteractionManagerService").methods.first { it.name == "showSessionFromSession" }
        val args = Bundle().apply {
            putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            putInt(ENTRY_POINT_KEY, ENTRY_POINT_LONG_PRESS_NAV_HANDLE)
        }
        val flags = VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT or VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE
        val result = if (method.parameterTypes.size >= 4) method.invoke(service, null, args, flags, if (Build.VERSION.SDK_INT >= 30) context.attributionTag else null)
            else method.invoke(service, null, args, flags)
        result as? Boolean ?: false
    }.getOrDefault(false)
}
