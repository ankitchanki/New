package com.vibe.stashdrop.core.shizuku

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow

class CaptureAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var current: CaptureAccessibilityService? = null; private set
        val foregroundPackage = MutableStateFlow("")
    }
    override fun onServiceConnected() { current = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) foregroundPackage.value = event.packageName?.toString().orEmpty()
    }
    override fun onInterrupt() { }
    override fun onDestroy() { if (current === this) current = null; super.onDestroy() }
}
