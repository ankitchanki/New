package com.vibe.stashdrop.features.floatingdock

import android.content.Context
import android.os.SystemClock
import android.view.*
import androidx.test.core.app.ApplicationProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vibe.stashdrop.features.stashshelf.GestureSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class BottomBarGestureTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val d get() = context.resources.displayMetrics.density
    private var triggers = 0
    private fun strip(settings: GestureSettings, gravity: Int = Gravity.CENTER_HORIZONTAL) = GestureStrip(context, settings, gravity, {_,_->}, {triggers++})
    private fun touch(view: View, action: Int, x: Float = 30f, y: Float = 180f, down: Long = SystemClock.uptimeMillis()) {
        MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x*d, y*d, 0).also { view.onTouchEvent(it); it.recycle() }
    }
    private fun idle(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
    private fun tap(view: View) {
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down=down); idle(25)
        touch(view, MotionEvent.ACTION_UP, down=down)
    }
    @Test fun holdFiresOnceAndReleaseDoesNotFireAgain() {
        val view = strip(GestureSettings(swipe=false, hold=true, doubleTap=true))
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down=down)
        idle(ViewConfiguration.getLongPressTimeout().toLong()+10)
        assertEquals(1, triggers)
        touch(view, MotionEvent.ACTION_UP, down=down); idle(1000)
        assertEquals(1, triggers)
    }
    @Test fun movementAndCancellationStopHold() {
        val view = strip(GestureSettings(swipe=false, hold=true))
        touch(view, MotionEvent.ACTION_DOWN)
        touch(view, MotionEvent.ACTION_MOVE, x=80f)
        idle(1000); assertEquals(0, triggers)
        touch(view, MotionEvent.ACTION_CANCEL)
        touch(view, MotionEvent.ACTION_DOWN)
        touch(view, MotionEvent.ACTION_POINTER_DOWN)
        idle(1000); assertEquals(0, triggers)
    }
    @Test fun doubleTapFiresOnlyOnSecondTap() {
        val view = strip(GestureSettings(swipe=false, doubleTap=true))
        tap(view); assertEquals(0, triggers)
        idle(80); tap(view); assertEquals(1, triggers)
        idle(80); tap(view); assertEquals(1, triggers)
    }
    @Test fun expiredCancelledAndDistantTapsDoNotPair() {
        val view = strip(GestureSettings(swipe=false, doubleTap=true))
        tap(view); idle(ViewConfiguration.getDoubleTapTimeout().toLong()+20); tap(view)
        assertEquals(0, triggers)
        touch(view, MotionEvent.ACTION_CANCEL); idle(50); tap(view)
        assertEquals(0, triggers)
        idle(50); touch(view, MotionEvent.ACTION_DOWN, x=600f); touch(view, MotionEvent.ACTION_UP, x=600f)
        assertEquals(0, triggers)
    }
    @Test fun cornersRequireUpAndInwardInBothDirections() {
        for (gravity in listOf(Gravity.LEFT, Gravity.RIGHT)) {
            triggers=0
            val view=strip(GestureSettings(swipe=false,corners=true), gravity)
            val inward=if(gravity==Gravity.LEFT) 1 else -1
            touch(view, MotionEvent.ACTION_DOWN, x=150f)
            touch(view, MotionEvent.ACTION_MOVE, x=150f+90*inward) // horizontal only
            assertEquals(0,triggers)
            touch(view, MotionEvent.ACTION_MOVE, x=150f-90*inward,y=80f) // outward
            assertEquals(0,triggers)
            touch(view, MotionEvent.ACTION_MOVE, x=150f+55*inward,y=125f)
            assertEquals(1,triggers)
            touch(view, MotionEvent.ACTION_UP,x=150f+70*inward,y=100f)
            assertEquals(1,triggers)
        }
    }
    @Test fun swipeUpFiresOnceAndDisabledGesturesNeverFire() {
        val view=strip(GestureSettings())
        touch(view,MotionEvent.ACTION_DOWN)
        touch(view,MotionEvent.ACTION_MOVE,y=100f)
        touch(view,MotionEvent.ACTION_UP,y=90f)
        assertEquals(1,triggers)
        triggers=0
        val off=strip(GestureSettings(swipe=false))
        tap(off); idle(30); tap(off)
        touch(off,MotionEvent.ACTION_DOWN); idle(1000)
        touch(off,MotionEvent.ACTION_MOVE,y=10f); touch(off,MotionEvent.ACTION_UP,y=10f)
        assertEquals(0,triggers)
    }
    @Test fun unchangedSettingsPreserveFirstTapAndHiddenNavigationKeepsMarkersVisible() {
        val windows=context.getSystemService(WindowManager::class.java)
        val overlay=BottomBarGestureOverlay(context,windows,{_,_->},{triggers++})
        val settings=GestureSettings(swipe=false,doubleTap=true,corners=true)
        overlay.attach(settings)
        val views=Shadow.extract<ShadowWindowManagerImpl>(windows).views
        val center=views.first { (it.layoutParams as WindowManager.LayoutParams).gravity == (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL) }
        ViewCompat.dispatchApplyWindowInsets(center,WindowInsetsCompat.Builder().setVisible(WindowInsetsCompat.Type.navigationBars(),false).build())
        assertEquals(View.VISIBLE,center.visibility)
        val params=center.layoutParams as WindowManager.LayoutParams
        assertTrue("Markers stay above the mandatory system gesture region",params.y>=32*d)
        views.filter { it!==center }.forEach { side ->
            assertTrue("Corners are inset from the system Back edge",(side.layoutParams as WindowManager.LayoutParams).x>=24*d)
        }
        tap(center); idle(80); overlay.attach(settings)
        assertTrue(Shadow.extract<ShadowWindowManagerImpl>(windows).views.any { it === center })
        tap(center); assertEquals(1,triggers)
        overlay.close()
        assertTrue(Shadow.extract<ShadowWindowManagerImpl>(windows).views.isEmpty())
    }
}
