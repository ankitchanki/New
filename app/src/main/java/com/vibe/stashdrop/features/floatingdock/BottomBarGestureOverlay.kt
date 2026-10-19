package com.vibe.stashdrop.features.floatingdock

import android.content.Context
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vibe.stashdrop.core.haptics.TactileHapticEngine
import com.vibe.stashdrop.features.stashshelf.GestureSettings
import kotlin.math.*

/** Opt-in touch strips above the system navigation region; Android does not allow touch replay. */
class BottomBarGestureOverlay(private val context: Context, private val windows: WindowManager,
    private val onProgress: (Float,Float)->Unit, private val onTrigger: ()->Unit) {
    private val views=mutableListOf<View>()
    private val density=context.resources.displayMetrics.density
    fun attach(settings: GestureSettings) {
        close()
        val zones=mutableListOf<Int>()
        if(settings.swipe || settings.hold || settings.doubleTap) zones+=Gravity.CENTER_HORIZONTAL
        if(settings.corners) zones+=listOf(Gravity.LEFT,Gravity.RIGHT)
        zones.forEach { gravity ->
            val view=GestureStrip(context,settings,gravity)
            val params=WindowManager.LayoutParams((88*density).toInt(),(24*density).toInt(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT)
            params.gravity=Gravity.BOTTOM or gravity
            ViewCompat.setOnApplyWindowInsetsListener(view) { _,insets ->
                val bottom=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                if(params.y!=bottom) { params.y=bottom; runCatching { windows.updateViewLayout(view,params) } }
                view.visibility=if(insets.isVisible(WindowInsetsCompat.Type.navigationBars())) View.VISIBLE else View.GONE
                insets
            }
            windows.addView(view,params); views+=view; ViewCompat.requestApplyInsets(view)
        }
    }
    fun close() { views.forEach { runCatching { windows.removeViewImmediate(it) } }; views.clear() }
    private inner class GestureStrip(context: Context,private val settings: GestureSettings,private val gravity: Int): View(context) {
        private val handler=Handler(Looper.getMainLooper())
        private val haptics=TactileHapticEngine(context)
        private var downX=0f; private var downY=0f; private var fired=false; private var moved=false
        private var lastTap=0L; private var velocity: VelocityTracker?=null
        private val hold=Runnable { if(!moved && !fired && settings.hold) trigger() }
        private fun trigger() { fired=true; handler.removeCallbacks(hold); haptics.confirm(); onProgress(1f,downX); onTrigger() }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX=event.rawX; downY=event.rawY; fired=false; moved=false; haptics.reset()
                    velocity?.recycle(); velocity=VelocityTracker.obtain().also { it.addMovement(event) }
                    if(settings.hold) handler.postDelayed(hold,400)
                }
                MotionEvent.ACTION_MOVE -> {
                    velocity?.addMovement(event)
                    val up=max(0f,downY-event.rawY); val horizontal=event.rawX-downX
                    if(hypot(horizontal,event.rawY-downY)>8*density) { moved=true; handler.removeCallbacks(hold) }
                    val allowed=if(gravity==Gravity.CENTER_HORIZONTAL) settings.swipe else settings.corners && (if(gravity==Gravity.LEFT) horizontal>=0 else horizontal<=0)
                    if(allowed && !fired) {
                        val pull=if(gravity==Gravity.CENTER_HORIZONTAL) up else hypot(up,horizontal)
                        haptics.tension(pull/density); onProgress((pull/(72*density)).coerceIn(0f,1f),event.rawX)
                        if(pull>=64*density) trigger()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(hold); velocity?.addMovement(event); velocity?.computeCurrentVelocity(1000)
                    val up=downY-event.rawY
                    if(!fired && settings.swipe && gravity==Gravity.CENTER_HORIZONTAL && up>12*density && (velocity?.yVelocity ?: 0f)<-1200f) trigger()
                    if(!fired && !moved && settings.doubleTap) {
                        if(event.eventTime-lastTap in 1..300) { trigger(); lastTap=0 } else lastTap=event.eventTime
                    }
                    if(!fired) onProgress(0f,event.rawX)
                    velocity?.recycle(); velocity=null; performClick()
                }
                MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(hold); velocity?.recycle(); velocity=null; onProgress(0f,downX) }
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
        override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); velocity?.recycle(); velocity=null; super.onDetachedFromWindow() }
    }
}
