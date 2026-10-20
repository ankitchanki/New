package com.vibe.stashdrop.features.floatingdock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.features.stashshelf.GestureSettings
import kotlin.math.*

/** Visible opt-in markers above Android navigation; these do not intercept system navigation gestures. */
class BottomBarGestureOverlay(private val context: Context, private val windows: WindowManager,
    private val onProgress: (Float, Float)->Unit, private val onTrigger: ()->Unit) {
    private val views = mutableListOf<View>()
    private val density = context.resources.displayMetrics.density
    private var attachedSettings: GestureSettings? = null
    private var configuration: String? = null
    fun attach(settings: GestureSettings) {
        val currentConfiguration = context.resources.configuration.toString()
        if (attachedSettings == settings && configuration == currentConfiguration) return
        close()
        attachedSettings = settings; configuration = currentConfiguration
        val zones = mutableListOf<Int>()
        if (settings.swipe || settings.hold || settings.doubleTap) zones += Gravity.CENTER_HORIZONTAL
        if (settings.corners) zones += listOf(Gravity.LEFT, Gravity.RIGHT)
        zones.forEach { gravity ->
            val view = GestureStrip(context, settings, gravity, onProgress, onTrigger)
            val params = WindowManager.LayoutParams((88*density).toInt(), (40*density).toInt(),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
                this.gravity = Gravity.BOTTOM or gravity
                // Let the platform fit navigation insets exactly once. No manual y offset as well.
                if (android.os.Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(WindowInsetsCompat.Type.systemBars())
            }
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                // A gesture navigation bar can report invisible while the screen is usable.
                // Keep markers available then; hide only while a keyboard occupies the bottom.
                view.visibility = if (insets.isVisible(WindowInsetsCompat.Type.ime())) View.GONE else View.VISIBLE
                insets
            }
            windows.addView(view, params); views += view
            ViewCompat.requestApplyInsets(view)
        }
    }
    fun close() {
        views.forEach { runCatching { windows.removeViewImmediate(it) } }; views.clear()
        attachedSettings = null; configuration = null
    }
}

/** Kept separate so real MotionEvents and timer cancellation can be regression tested. */
internal class GestureStrip(context: Context, private val settings: GestureSettings, private val gravity: Int,
    private val onProgress: (Float, Float)->Unit, private val onTrigger: ()->Unit): View(context) {
    private val density = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val haptics = TactileHapticEngine(context)
    private val config = ViewConfiguration.get(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99B4C9FF.toInt() }
    private var downX = 0f; private var downY = 0f
    private var active = false; private var fired = false; private var moved = false
    private var lastTap: Long? = null; private var tapX = 0f; private var tapY = 0f
    private var secondTap = false
    private var velocity: VelocityTracker? = null
    private val center get() = gravity == Gravity.CENTER_HORIZONTAL
    private val hold = Runnable { if (active && !moved && !fired && center && settings.hold) trigger() }
    init { contentDescription = if (center) "Google search gesture strip" else "Google search corner swipe" }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = min(width*.5f, 36*density)
        canvas.drawRoundRect((width-w)/2, height/2f-2*density, (width+w)/2, height/2f+2*density, 2*density, 2*density, paint)
    }
    private fun trigger() {
        if (fired || !active) return
        fired = true; lastTap = null; handler.removeCallbacks(hold)
        haptics.confirm(); onProgress(1f, downX); onTrigger()
    }
    private fun updateSwipe(event: MotionEvent) {
        val up = downY-event.rawY
        val horizontal = event.rawX-downX
        if (hypot(horizontal, up) > config.scaledTouchSlop) {
            moved = true; lastTap = null; handler.removeCallbacks(hold)
        }
        if (fired) return
        val inward = if (gravity == Gravity.LEFT) horizontal else -horizontal
        val allowed = if (center) settings.swipe && up > 0 && up >= abs(horizontal)*.5f
            else settings.corners && up > 12*density && inward > 12*density
        if (allowed) {
            val pull = if (center) up else hypot(up, inward)
            haptics.tension(pull/density)
            onProgress((pull/(72*density)).coerceIn(0f, 1f), event.rawX)
            if (pull >= 64*density) trigger()
        }
    }
    private fun cancel() {
        active = false; lastTap = null; secondTap = false
        handler.removeCallbacks(hold); velocity?.recycle(); velocity = null
        onProgress(0f, downX)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                active = true; fired = false; moved = false; haptics.reset()
                secondTap = lastTap?.let { event.eventTime-it in 1..ViewConfiguration.getDoubleTapTimeout().toLong() &&
                    hypot(downX-tapX, downY-tapY) <= config.scaledDoubleTapSlop } == true
                lastTap = null
                velocity?.recycle(); velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                if (center && settings.hold) handler.postDelayed(hold, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (active) { velocity?.addMovement(event); updateSwipe(event) }
            MotionEvent.ACTION_UP -> if (active) {
                handler.removeCallbacks(hold); velocity?.addMovement(event); velocity?.computeCurrentVelocity(1000)
                updateSwipe(event)
                val up = downY-event.rawY
                if (!fired && center && settings.swipe && up > 12*density && up >= abs(event.rawX-downX)*.5f &&
                    (velocity?.yVelocity ?: 0f) < -1200*density) trigger()
                if (!fired && !moved && center && settings.doubleTap && event.eventTime-event.downTime < ViewConfiguration.getLongPressTimeout()) {
                    if (secondTap) trigger() else { lastTap = event.eventTime; tapX = downX; tapY = downY }
                }
                if (!fired) onProgress(0f, event.rawX)
                active = false; secondTap = false
                velocity?.recycle(); velocity = null; performClick()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> cancel()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDetachedFromWindow() { cancel(); super.onDetachedFromWindow() }
}
