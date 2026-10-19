package com.vibe.stashdrop.features.floatingdock

import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings
import java.util.concurrent.TimeUnit

/** Starting the floating shelf must not crash once overlay access is granted. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class FloatingShelfServiceTest {
    private fun idle() = repeat(30) { ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS) }
    @Test @Config(sdk = [34, 35]) fun startsShowsOverlaysAndStops() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(OneUiThumbDockService::class.java).create().startCommand(0, 1)
        idle()
        assertTrue("service should report running", OneUiThumbDockService.running.value)
        val windows = ApplicationProvider.getApplicationContext<android.content.Context>().getSystemService(WindowManager::class.java)
        val views = org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowWindowManagerImpl>(windows).views
        assertTrue("dock and aurora overlays attached, got ${views.size}", views.size >= 2)
        // Service overlays are not hardware accelerated: every overlay must draw on a software canvas.
        views.forEach { view ->
            val bitmap = android.graphics.Bitmap.createBitmap(maxOf(view.width, 1), maxOf(view.height, 1), android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
        }
        controller.destroy()
        idle()
    }
}
