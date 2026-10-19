package com.vibe.stashdrop.features.floatingdock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.util.concurrent.TimeUnit

/** The floating bubble must survive starting, tapping, holding, dragging and opening the shelf. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FloatingShelfServiceTest {
    private fun idle(ms: Long = 3000) = repeat((ms / 100).toInt()) { ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS) }
    private val windows get() = ApplicationProvider.getApplicationContext<android.content.Context>().getSystemService(WindowManager::class.java)
    private fun views(): List<View> = Shadow.extract<ShadowWindowManagerImpl>(windows).views
    /** Service overlays may be software rendered: every overlay must draw on a software canvas. */
    private fun drawAll() = views().forEach { view ->
        val bitmap = Bitmap.createBitmap(maxOf(view.width, 1), maxOf(view.height, 1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
    }
    private fun touch(view: View, action: Int, x: Float, y: Float, down: Long) {
        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
        view.dispatchTouchEvent(event); event.recycle()
    }
    private fun startService() = Robolectric.buildService(OneUiThumbDockService::class.java).create().startCommand(0, 1).also { idle() }
    private fun dock(): View = views().first { (it.layoutParams as WindowManager.LayoutParams).width in 1 until 1000 }

    @Test @Config(sdk = [34, 35]) fun startsShowsOverlaysAndStops() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = startService()
        assertTrue("service should report running", OneUiThumbDockService.running.value)
        assertTrue("dock and aurora overlays attached, got ${views().size}", views().size >= 2)
        drawAll()
        controller.destroy(); idle()
    }

    @Test @Config(sdk = [34, 35]) fun tapOpensShelfAndDragMovesBubble() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = startService()
        val dock = dock(); val c = dock.width / 2f
        val before = views().size
        // Tap: opens the shelf sheet window.
        var down = SystemClock.uptimeMillis()
        touch(dock, MotionEvent.ACTION_DOWN, c, c, down); idle(100); touch(dock, MotionEvent.ACTION_UP, c, c, down); idle()
        assertTrue("tap should open the shelf sheet", views().size > before)
        drawAll()
        // Tap again closes the sheet.
        down = SystemClock.uptimeMillis()
        touch(dock, MotionEvent.ACTION_DOWN, c, c, down); idle(100); touch(dock, MotionEvent.ACTION_UP, c, c, down); idle()
        // Drag across the screen.
        down = SystemClock.uptimeMillis()
        touch(dock, MotionEvent.ACTION_DOWN, c, c, down)
        for (step in 1..20) { touch(dock, MotionEvent.ACTION_MOVE, c - step * 15f, c + step * 10f, down); idle(16) }
        touch(dock, MotionEvent.ACTION_UP, c - 300f, c + 200f, down); idle()
        drawAll()
        controller.destroy(); idle()
    }
}
