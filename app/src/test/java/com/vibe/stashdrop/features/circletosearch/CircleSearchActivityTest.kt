package com.vibe.stashdrop.features.circletosearch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import com.vibe.stashdrop.core.shizuku.CapturedFrame
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CircleSearchActivityTest {
    private fun idle(ms: Long = 1500) = repeat((ms / 100).toInt()) { ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS) }
    private fun draw(activity: CircleSearchActivity) {
        val root = activity.window.decorView
        val bitmap = Bitmap.createBitmap(maxOf(root.width, 1), maxOf(root.height, 1), Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
    }

    /** Circling, resetting and circling again must keep drawing without touching recycled bitmaps. */
    @Test fun circleResetAndCircleAgain() {
        val frame = Bitmap.createBitmap(540, 1170, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(220, 226, 240)) }
        CapturedFrame.put(frame)
        val controller = Robolectric.buildActivity(CircleSearchActivity::class.java).setup()
        val activity = controller.get()
        idle(); draw(activity)
        val model = activity.viewModelStore.let { androidx.lifecycle.ViewModelProvider(activity)[CircleSearchModel::class.java] }
        val ring = (0..40).map { i -> val a = i / 40f * 6.283f; Offset(270f + 150f * kotlin.math.cos(a), 585f + 200f * kotlin.math.sin(a)) }
        repeat(3) {
            model.select(ring, IntSize(540, 1170)); idle(3000); draw(activity)
            model.reset(); idle(); draw(activity)
        }
        assertEquals(null, model.crop)
        // A new capture while the screen is open must not invalidate the frame it is drawing.
        CapturedFrame.put(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)); idle(); draw(activity)
        controller.pause().stop().destroy()
    }

    @Test fun googleRouteNeedsTheGoogleApp() {
        assertEquals(GoogleCircleToSearch.Readiness.GOOGLE_MISSING, GoogleCircleToSearch.readiness(ApplicationProvider.getApplicationContext()))
    }
}
