package com.vibe.stashdrop.core.hiddenapi

import org.lsposed.hiddenapibypass.HiddenApiBypass
object HiddenApiBypassInitializer {
    fun initialize(): Boolean = runCatching { HiddenApiBypass.addHiddenApiExemptions("Landroid/", "Lcom/android/internal/") }.getOrDefault(false)
}
