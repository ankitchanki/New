package com.vibe.stashdrop.features.circletosearch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vibe.stashdrop.stashApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class GoogleOnlySearchTest {
    @Test fun missingGoogleReportsSetupWithoutStartingAnySearchOrCaptureActivity() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertEquals(GoogleCircleToSearch.Readiness.GOOGLE_MISSING,GoogleCircleToSearch.readiness(context))
        assertFalse(CircleToSearchOrchestrator(context).launch())
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
        assertTrue(ShadowToast.getTextOfLatestToast().contains("Google app"))
    }
    @Test fun oldBuiltInPreferenceCannotRestoreRemovedSearch() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.stashApp.preferences.set("google_cts",false)
        assertFalse(CircleToSearchOrchestrator(context).launch())
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
        assertTrue(ShadowToast.getTextOfLatestToast().contains("Google app"))
        assertFalse(context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_ACTIVITIES).activities.orEmpty().any {
            it.name.endsWith("CircleSearchActivity") || it.name.endsWith("CaptureConsentActivity")
        })
    }
    private fun installGoogleService(context: Context): android.content.ComponentName {
        val name=android.content.ComponentName(GoogleCircleToSearch.GOOGLE,"${GoogleCircleToSearch.GOOGLE}.GoogleVoiceService")
        val info=android.content.pm.ResolveInfo().apply {
            serviceInfo=android.content.pm.ServiceInfo().apply {
                packageName=name.packageName; this.name=name.className
                permission=android.Manifest.permission.BIND_VOICE_INTERACTION
                metaData=android.os.Bundle().apply { putInt(android.service.voice.VoiceInteractionService.SERVICE_META_DATA,1) }
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(android.content.Intent(android.service.voice.VoiceInteractionService.SERVICE_INTERFACE).setPackage(name.packageName),info)
        return name
    }
    @Test fun inactiveGoogleServiceRequestsAssistantSetupWithoutOpeningAnActivity() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        installGoogleService(context)
        android.provider.Settings.Secure.putString(context.contentResolver,"voice_interaction_service","another.assistant/another.assistant.Service")
        assertEquals(GoogleCircleToSearch.Readiness.GOOGLE_NOT_ASSISTANT,GoogleCircleToSearch.readiness(context))
        assertFalse(CircleToSearchOrchestrator(context).launch())
        assertTrue(ShadowToast.getTextOfLatestToast().contains("digital assistant"))
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
    }
    @Test fun rejectedSessionStaysGoogleOnlyAndReportsBridgeGuidance() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val google=installGoogleService(context)
        android.provider.Settings.Secure.putString(context.contentResolver,"voice_interaction_service",google.flattenToShortString())
        assertEquals(GoogleCircleToSearch.Readiness.READY,GoogleCircleToSearch.readiness(context))
        // Robolectric has no voice-interaction Binder; rejection must never restore an internal route.
        assertFalse(CircleToSearchOrchestrator(context).launch())
        assertTrue(ShadowToast.getTextOfLatestToast().contains("Shizuku"))
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
    }

}
