package com.vibe.stashdrop.ui.shelf

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[28,35])
class ShelfThumbnailTest {
    private val dir get()=ApplicationProvider.getApplicationContext<Context>().cacheDir
    @Test fun decodesRealLargeImageToBoundedPreviewAndCachesIt() {
        val file=File.createTempFile("thumbnail-",".png",dir)
        val original=Bitmap.createBitmap(1600,800,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
        file.outputStream().use { original.compress(Bitmap.CompressFormat.PNG,100,it) };original.recycle()
        val frame=ShelfThumbnails.load(file,"image/png")
        assertNotNull(org.robolectric.shadows.ShadowLog.getLogsForTag("StashDropMedia").joinToString { it.throwable?.stackTraceToString().orEmpty() },frame);assertEquals(320,frame!!.width);assertEquals(160,frame.height)
        assertEquals(android.graphics.Color.GREEN,frame.getPixel(10,10))
        assertSame(frame,ShelfThumbnails.load(file,"image/png"))
        file.delete()
    }
    @Test fun missingOrCorruptMediaReturnsUnavailableWithoutThrowing() {
        assertNull(ShelfThumbnails.load(File(dir,"absent.png"),"image/png"))
        val broken=File.createTempFile("broken-",".bin",dir).apply { writeText("invalid media") }
        assertNull(ShelfThumbnails.load(broken,"image/png"))
        assertNull(ShelfThumbnails.load(broken,"video/mp4"))
        broken.delete()
    }
    @Test fun jpegExifRotationIsHonoredByPreview() {
        val file=File.createTempFile("oriented-",".jpg",dir)
        val pixels=Bitmap.createBitmap(800,400,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val bytes=java.io.ByteArrayOutputStream().also { pixels.compress(Bitmap.CompressFormat.JPEG,95,it) }.toByteArray();pixels.recycle()
        // Minimal TIFF IFD with EXIF orientation 6 (90 degrees clockwise).
        val exif=byteArrayOf(69,120,105,102,0,0,73,73,42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,6,0,0,0,0,0,0,0)
        file.outputStream().use { out ->
            out.write(bytes,0,2);out.write(255);out.write(225);out.write(0);out.write(exif.size+2);out.write(exif);out.write(bytes,2,bytes.size-2)
        }
        val frame=ShelfThumbnails.load(file,"image/jpeg")
        assertNotNull(frame);assertTrue("Portrait orientation must survive decoding",frame!!.height>frame.width)
        assertTrue(maxOf(frame.width,frame.height)<=320)
        file.delete()
    }

}
