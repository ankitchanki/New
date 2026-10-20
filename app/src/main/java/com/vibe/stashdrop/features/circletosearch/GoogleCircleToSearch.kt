package com.vibe.stashdrop.features.circletosearch

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import com.vibe.stashdrop.stashApp
import android.util.Log
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
        return if (ComponentName.unflattenFromString(active) == google) Readiness.READY else Readiness.GOOGLE_NOT_ASSISTANT
    }

    /** True when Android accepted the request; Google controls eligibility and the displayed UI. */
    suspend fun launch(context: Context): Boolean {
        if (readiness(context) != Readiness.READY) return false
        return withContext(Dispatchers.IO) {
            // showSessionFromSession is often rejected without an existing session token.
            // Shizuku can use the privileged active-service entry point instead.
            val shell = runCatching { context.stashApp.shizuku.service?.requestGoogleSearch() == true }
                .getOrDefault(false)
            shell || requestSession(privileged = false, attributionTag = if (Build.VERSION.SDK_INT >= 30) context.attributionTag else null)
        }
    }

    /** Fixed Google contextual-search request, shared with the Shizuku shell process. */
    fun requestSession(privileged: Boolean, attributionTag: String? = null): Boolean = runCatching {
        val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as IBinder
        val api = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val service = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        if (privileged) {
            val active = api.getMethod("getActiveServiceComponentName").invoke(service) as? ComponentName
            if (active?.packageName != GOOGLE) return false
        }
        val name = if (privileged) "showSessionForActiveService" else "showSessionFromSession"
        val method = api.methods.first { it.name == name }
        val args = Bundle().apply {
            putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            putInt(ENTRY_POINT_KEY, ENTRY_POINT_LONG_PRESS_NAV_HANDLE)
        }
        val flags = VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT or VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE
        val arguments = method.parameterTypes.map { type -> when (type) {
            Bundle::class.java -> args
            Int::class.javaPrimitiveType -> flags
            String::class.java -> attributionTag
            else -> null // callback / session token / activity token
        } }.toTypedArray()
        method.invoke(service, *arguments) as? Boolean ?: false
    }.onFailure { Log.w("StashDropGoogle", "Google session request rejected (${if (privileged) "shell" else "app"})", it) }.getOrDefault(false)
}
