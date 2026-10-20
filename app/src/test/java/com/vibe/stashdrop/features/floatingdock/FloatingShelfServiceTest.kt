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
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    /** Guarded failures are recorded instead of crashing; any record is a failed test. */
    private fun assertNoErrors() = org.junit.Assert.assertNull("recorded error", com.vibe.stashdrop.core.diagnostics.CrashReporter.read(context))
    @org.junit.Before fun clearErrors() = com.vibe.stashdrop.core.diagnostics.CrashReporter.clear(context)
    private fun startService() = Robolectric.buildService(OneUiThumbDockService::class.java).create().startCommand(0, 1).also { idle() }
    private fun dock(): View = views().first { (it.layoutParams as WindowManager.LayoutParams).width in 1 until 1000 }

    @Test @Config(sdk = [34, 35]) fun startsShowsOverlaysAndStops() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = startService()
        assertTrue("service should report running", OneUiThumbDockService.running.value)
        assertTrue("dock and aurora overlays attached, got ${views().size}", views().size >= 2)
        drawAll()
        idle(5000) // The idle peek animation runs three seconds after start.
        assertNoErrors()
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
        touch(dock, MotionEvent.ACTION_UP, c - 300f, c + 200f, down); idle(5000)
        drawAll()
        assertNoErrors()
        controller.destroy(); idle()
    }

    @Test @Config(sdk = [35]) fun circleToSearchFromTheBubbleRecordsNoError() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = startService()
        controller.withIntent(android.content.Intent().setAction(OneUiThumbDockService.SEARCH)).startCommand(0, 2)
        idle(4000)
        drawAll()
        assertNoErrors()
        controller.destroy(); idle()
    }

    @Test @Config(sdk = [35]) fun droppingTheBubbleOnTheXHidesIt() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = startService()
        val dock = dock(); val params = dock.layoutParams as WindowManager.LayoutParams
        val target = views().first { (it.layoutParams as WindowManager.LayoutParams).gravity == android.view.Gravity.BOTTOM }
        val down = SystemClock.uptimeMillis(); val c = dock.width / 2f
        touch(dock, MotionEvent.ACTION_DOWN, c, c, down); idle(50)
        // Move in small steps from the bubble's position to the target's centre.
        val location = IntArray(2).also(target::getLocationOnScreen)
        val dx = location[0] + target.width / 2f - (params.x + dock.width / 2f)
        val dy = location[1] + target.height / 2f - (params.y + dock.height / 2f)
        for (step in 1..30) { touch(dock, MotionEvent.ACTION_MOVE, c + dx * step / 30, c + dy * step / 30, down); idle(16) }
        touch(dock, MotionEvent.ACTION_UP, c + dx, c + dy, down); idle()
        assertNoErrors()
        assertTrue("dropping on the X should hide the bubble", org.robolectric.Shadows.shadowOf(controller.get()).isStoppedBySelf)
        controller.destroy(); idle()
    }
    @Test @Config(sdk=[34,35]) fun idleDecorationsDoNotObscureOtherAppsAndSingleHandleOpensCompactPanel() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller=startService()
        fun decorations()=views().filter { (it.layoutParams as WindowManager.LayoutParams).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0 }
        assertTrue("No visible decorative overlay while idle",decorations().none { it.visibility==View.VISIBLE })
        val combined=1f-decorations().fold(1f) { transparent,view -> transparent*(1f-(view.layoutParams as WindowManager.LayoutParams).alpha) }
        assertTrue("Even active decoration overlap must remain below input-blocking opacity",combined<=.8f)
        val handles=views().filter { val p=it.layoutParams as WindowManager.LayoutParams; p.width>0 && p.height>p.width && p.gravity==(android.view.Gravity.TOP or android.view.Gravity.LEFT) }
        org.junit.Assert.assertEquals("Only one screen edge has a handle",1,handles.size)
        for(handle in handles) {
            val before=views().size
            val down=SystemClock.uptimeMillis()
            touch(handle,MotionEvent.ACTION_DOWN,handle.width/2f,handle.height/2f,down);idle(50)
            touch(handle,MotionEvent.ACTION_UP,handle.width/2f,handle.height/2f,down);idle()
            org.junit.Assert.assertEquals(before+1,views().size)
            val panel=views().last()
            val p=panel.layoutParams as WindowManager.LayoutParams
            assertTrue("Panel has bounded width",p.width in 1 until context.resources.displayMetrics.widthPixels)
            assertTrue("Panel has compact height",p.height <= (420*context.resources.displayMetrics.density).toInt())
            assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL != 0)
            touch(panel,MotionEvent.ACTION_OUTSIDE,0f,0f,SystemClock.uptimeMillis());idle()
            org.junit.Assert.assertEquals("Outside touch dismisses panel",before,views().size)
        }
        assertNoErrors();controller.destroy();idle()
    }

    @Test @Config(sdk=[35]) fun stoppingDuringSearchCannotRecreateGestureWindows() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller=startService()
        controller.withIntent(android.content.Intent().setAction(OneUiThumbDockService.SEARCH)).startCommand(0,2)
        idle(100) // Search hand-off is waiting for its animation; cancel it now.
        controller.destroy();idle(3000)
        assertTrue("All overlays must stay removed after stopping",views().isEmpty())
        assertTrue(!OneUiThumbDockService.running.value)
    }

}
