package com.vibe.stashdrop.core.shizuku

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.vibe.stashdrop.features.circletosearch.CircleSearchActivity

/** One consent token, one virtual display, one screenshot. Never retains a capture session. */
class ProjectionCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finishCapture(null) }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("capture", "Screen capture", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, "capture").setContentTitle("Capturing for Circle to Search")
            .setSmallIcon(android.R.drawable.ic_menu_camera).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(2, notification)
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("data", Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra<Intent>("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(Activity.RESULT_OK, data)
            projection!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { release(); stopSelf() } }, handler)
            val metrics = resources.displayMetrics
            reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
            reader!!.setOnImageAvailableListener({ images ->
                val image = images.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = try {
                    val plane = image.planes[0]
                    val paddedWidth = image.width + (plane.rowStride - plane.pixelStride * image.width) / plane.pixelStride
                    val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(plane.buffer)
                    Bitmap.createBitmap(padded, 0, 0, image.width, image.height).also { if (it !== padded) padded.recycle() }
                } finally { image.close() }
                finishCapture(bitmap)
            }, handler)
            display = projection!!.createVirtualDisplay("StashDrop one-shot", metrics.widthPixels, metrics.heightPixels,
                metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
            handler.postDelayed(timeout, 5000)
        } catch (e: Exception) { android.widget.Toast.makeText(this, "Capture failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show(); finishCapture(null) }
        return START_NOT_STICKY
    }
    private fun finishCapture(bitmap: Bitmap?) {
        handler.removeCallbacks(timeout)
        release()
        if (bitmap != null) {
            CapturedFrame.put(bitmap)
            startActivity(Intent(this, CircleSearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        stopSelf()
    }
    private fun release() {
        reader?.setOnImageAvailableListener(null, null); display?.release(); display = null; reader?.close(); reader = null
        val p = projection; projection = null; p?.stop()
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); release(); super.onDestroy() }
}
