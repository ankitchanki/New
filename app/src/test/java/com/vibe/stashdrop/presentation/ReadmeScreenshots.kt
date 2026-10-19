package com.vibe.stashdrop.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibe.stashdrop.core.shaders.AuroraWave
import com.vibe.stashdrop.core.shizuku.ShizukuConnectionManager
import com.vibe.stashdrop.features.floatingdock.IosFanOutStashSheet
import com.vibe.stashdrop.features.stashshelf.*
import com.vibe.stashdrop.ui.dashboard.*
import com.vibe.stashdrop.ui.theme.StashDropTheme
import org.junit.Rule
import org.junit.Test

/** Documentation renders of production composables with clearly identified sample data. */
class ReadmeScreenshots {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
        theme = "android:style/Theme.Material.NoActionBar",
        useDeviceResolution = true,
    )
    private val samples = listOf(
        StashItemEntity("note", "Good ideas deserve a place to land.", null, "text/plain", "Ideas", 1L),
        StashItemEntity("link", "https://developer.android.com/jetpack/compose", null, "text/plain", "Reading", 2L),
        StashItemEntity("packing", "Weekend essentials\nCamera · notebook · a little curiosity", null, "text/plain", "Inbox", 3L),
    )
    @Test fun overview() {
        paparazzi.snapshot(name = "overview") { dashboard() }
    }
    @Test fun shelf() { paparazzi.snapshot(name = "shelf") { dashboard(1) } }
    @Test fun controls() { paparazzi.snapshot(name = "controls") { dashboard(2) } }
    @Composable private fun dashboard(initialTab: Int = 0) {
        StashDropTheme {
            DashboardScreen(
                items = samples, settings = StashSettings(), shizuku = ShizukuConnectionManager.State(),
                running = false, setup = SetupState(overlay = true), message = "", clipboardStatus = "Auto-stash is off",
                actions = DashboardActions(), initialTab = initialTab,
            )
        }
    }
    @Test fun floatingShelf() {
        val repository = StashRepository(paparazzi.context, StashDatabase.create(paparazzi.context).stash())
        paparazzi.snapshot(name = "floating-shelf") {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                StashDropTheme {
                    Box(Modifier.fillMaxSize().background(Color(0xFF0C0E15))) {
                        IosFanOutStashSheet(samples, repository, {}, {}, {_,_->}, {}, {_,_->})
                    }
                }
            }
        }
    }
    @Test fun controlsFull() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 5200, softButtons = false))
        paparazzi.snapshot(name = "controls-full") { dashboard(2) }
    }
    @Test fun circleCanvas() {
        val bitmap = android.graphics.Bitmap.createBitmap(1080, 2340, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(242, 241, 237))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.rgb(37, 45, 58); paint.textSize = 30f
        canvas.drawText("SAMPLE CONTENT · CIRCLE TO SEARCH PREVIEW", 72f, 160f, paint)
        paint.textSize = 94f; paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        canvas.drawText("Keep the", 72f, 350f, paint)
        canvas.drawText("good parts.", 72f, 465f, paint)
        paint.color = android.graphics.Color.rgb(173, 196, 245)
        canvas.drawRoundRect(android.graphics.RectF(72f, 590f, 1008f, 1510f), 48f, 48f, paint)
        paint.color = android.graphics.Color.rgb(132, 182, 176)
        canvas.drawCircle(650f, 1050f, 300f, paint)
        paint.color = android.graphics.Color.rgb(228, 227, 220)
        canvas.drawRoundRect(android.graphics.RectF(230f, 900f, 780f, 1280f), 70f, 70f, paint)
        paint.color = android.graphics.Color.rgb(37, 45, 58); paint.textSize = 55f
        canvas.drawText("A note worth keeping.", 72f, 1690f, paint)
        paint.textSize = 38f; paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Circle it. Search it. Stash it.", 72f, 1800f, paint)
        val points = (0..80).map { index ->
            val angle = index / 80f * kotlin.math.PI.toFloat() * 2f
            androidx.compose.ui.geometry.Offset(540f + 440f * kotlin.math.cos(angle), 1050f + 480f * kotlin.math.sin(angle))
        }
        paparazzi.snapshot(name = "circle-canvas") {
            StashDropTheme {
                com.vibe.stashdrop.features.circletosearch.AuroraCircleOverlayCanvas(
                    bitmap, Modifier.fillMaxSize(), {_,_->}, initialSelection = points,
                )
            }
        }
        bitmap.recycle()
    }
    @Test fun auroraWave() {
        paparazzi.snapshot(name = "aurora-wave") {
            StashDropTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AuroraWave(.75f, androidx.compose.ui.geometry.Offset(540f, 1500f), Modifier.fillMaxSize())
                }
            }
        }
    }
}
