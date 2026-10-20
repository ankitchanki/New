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

    /** A local Binder endpoint verifies the actual reflected API and payload on both OS versions. */
    private fun voiceBinder(active: android.content.ComponentName, calls: MutableList<Pair<android.os.Bundle,Int>>): android.os.IBinder {
        val api=Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        lateinit var binder: android.os.Binder
        val service=java.lang.reflect.Proxy.newProxyInstance(api.classLoader,arrayOf(api)) { _,method,args ->
            when(method.name) {
                "getActiveServiceComponentName" -> active
                "showSessionFromSession" -> {
                    assertNull(args!![0]) // AOSP permits a null session token for this invocation.
                    calls += (args[1] as android.os.Bundle) to (args[2] as Int)
                    true
                }
                "showSessionForActiveService" -> error("Must preserve contextual-search handling")
                "asBinder" -> binder
                else -> null
            }
        } as android.os.IInterface
        binder=android.os.Binder().apply { attachInterface(service,api.name) }
        return binder
    }
    @Test @Config(sdk=[28,35]) fun bothIdentitiesUseTheContextualSessionApiAndGoogleEntryPoint() {
        val calls=mutableListOf<Pair<android.os.Bundle,Int>>()
        val binder=voiceBinder(android.content.ComponentName(GoogleCircleToSearch.GOOGLE,"GoogleService"),calls)
        val accepted=GoogleCircleToSearch.requestSession(fromShell=false,voiceBinder=binder)
        assertTrue(org.robolectric.shadows.ShadowLog.getLogsForTag("StashDropGoogle").joinToString { "${it.msg}: ${it.throwable?.stackTraceToString()}" },accepted)
        assertTrue(GoogleCircleToSearch.requestSession(fromShell=true,voiceBinder=binder))
        assertEquals(2,calls.size)
        calls.forEach { (args,flags) ->
            assertEquals(1,args.getInt("omni.entry_point"))
            assertTrue(args.containsKey("invocation_time_ms"))
            assertEquals(android.service.voice.VoiceInteractionSession.SHOW_WITH_ASSIST or
                android.service.voice.VoiceInteractionSession.SHOW_WITH_SCREENSHOT or
                android.service.voice.VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE,flags)
        }
    }
    @Test @Config(sdk=[28,35]) fun changedActiveServiceCannotLaunchAnotherAssistant() {
        val calls=mutableListOf<Pair<android.os.Bundle,Int>>()
        val binder=voiceBinder(android.content.ComponentName("another.assistant","AnotherService"),calls)
        assertFalse(GoogleCircleToSearch.requestSession(fromShell=false,voiceBinder=binder))
        assertFalse(GoogleCircleToSearch.requestSession(fromShell=true,voiceBinder=binder))
        assertTrue(calls.isEmpty())
    }

}
