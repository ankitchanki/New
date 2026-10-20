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
import com.vibe.stashdrop.features.floatingdock.ThumbDockIconComposable
import com.vibe.stashdrop.ui.shelf.ShelfThumbnails
import java.io.File
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
    private val sampleFiles by lazy {
        val dir=File(paparazzi.context.filesDir,"stash").apply { mkdirs() }
        listOf("sample-landscape.png","sample-sticker.png").mapIndexed { index,name ->
            val file=File(dir,name)
            val bitmap=android.graphics.Bitmap.createBitmap(480,320,android.graphics.Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bitmap)
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(if(index==0) android.graphics.Color.rgb(166,208,239) else android.graphics.Color.rgb(48,42,74))
            paint.color=if(index==0) android.graphics.Color.rgb(253,216,133) else android.graphics.Color.rgb(237,179,222)
            canvas.drawCircle(if(index==0) 365f else 240f,if(index==0) 80f else 160f,if(index==0) 40f else 112f,paint)
            if(index==0) {
                paint.color=android.graphics.Color.rgb(67,116,111)
                val path=android.graphics.Path().apply { moveTo(0f,320f); lineTo(170f,115f); lineTo(340f,320f); close() }
                canvas.drawPath(path,paint)
                paint.color=android.graphics.Color.rgb(91,145,118);canvas.drawOval(android.graphics.RectF(190f,210f,610f,470f),paint)
            } else {
                paint.color=android.graphics.Color.rgb(48,42,74);canvas.drawCircle(202f,140f,12f,paint);canvas.drawCircle(278f,140f,12f,paint)
                paint.style=android.graphics.Paint.Style.STROKE;paint.strokeWidth=9f
                canvas.drawArc(android.graphics.RectF(195f,165f,285f,220f),0f,180f,false,paint)
            }
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            ShelfThumbnails.load(file,"image/png")
            name to file
        }.toMap()
    }
    private val samples = listOf(
        StashItemEntity("landscape",null,"sample-landscape.png","image/png","Photos",4L),
        StashItemEntity("sticker",null,"sample-sticker.png","image/png","Stickers",5L),
        StashItemEntity("note", "Good ideas deserve a place to land.", null, "text/plain", "Ideas", 1L),
        StashItemEntity("link", "https://developer.android.com/jetpack/compose", null, "text/plain", "Reading", 2L),
        StashItemEntity("packing", "Weekend essentials\nCamera · notebook · a little curiosity", null, "text/plain", "Inbox", 3L),
    )
    @Test fun overview() {
        paparazzi.snapshot(name = "overview") { dashboard() }
    }
    @Test fun shelf() { paparazzi.snapshot(name = "shelf") { dashboard(1) } }
    @Test fun controls() { paparazzi.snapshot(name = "controls") { dashboard(2) } }
    @Composable private fun dashboard(initialTab: Int = 0, interactions: Boolean = false) {
        StashDropTheme {
            DashboardScreen(
                items = samples, settings = StashSettings(), shizuku = ShizukuConnectionManager.State(),
                running = false, setup = SetupState(overlay = true), message = "", clipboardStatus = "Auto-stash is off",
                actions = DashboardActions(), initialTab = initialTab, initialInteractions = interactions,
                mediaFile={sampleFiles[it.fileName]},
            )
        }
    }
    @Test fun floatingShelf() {
        sampleFiles // Generate and decode the actual local sample media before rendering.
        val repository = StashRepository(paparazzi.context, StashDatabase.create(paparazzi.context).stash())
        paparazzi.snapshot(name = "floating-shelf") {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                StashDropTheme {
                    Box(Modifier.fillMaxSize().background(Color(0xFF0C0E15))) {
                        Box(Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(12.dp).width(296.dp).height(420.dp)) {
                            IosFanOutStashSheet(samples, repository, {}, {}, {_,_->}, {}, {_,_->},compact=true)
                        }
                    }
                }
            }
        }
    }
    @Test fun interactions() { paparazzi.snapshot(name = "interactions") { dashboard(2, true) } }
    @Test fun controlsFull() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 5200, softButtons = false))
        paparazzi.snapshot(name = "controls-full") { dashboard(2) }
    }
    @Test fun edgeHandles() {
        paparazzi.snapshot(name="edge-handles") {
            StashDropTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    listOf(androidx.compose.ui.Alignment.CenterEnd).forEach { edge ->
                        ThumbDockIconComposable(5,false,{}, {},{}, {},Modifier.align(edge),edgeHandle=true)
                    }
                }
            }
        }
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
