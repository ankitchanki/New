package com.vibe.stashdrop.core.haptics

import android.content.Context
import android.os.*

class TactileHapticEngine(context: Context) {
    companion object {
        /** Mirrors the in-app "Haptic feedback" setting. */
        @Volatile var enabled = true
    }
    private val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator else context.getSystemService(Vibrator::class.java)
    private var lastStep = 0
    private var travelled = 0f
    fun reset() { lastStep = 0; travelled = 0f }
    /** Rising ticks while pulling toward a threshold. */
    fun tension(distanceDp: Float) {
        val step = (distanceDp / 12).toInt()
        if (step > lastStep) { tick(); lastStep = step }
    }
    /** Light detent ticks while dragging or drawing, one per [spacingDp] travelled. */
    fun travel(deltaDp: Float, spacingDp: Float = 28f) {
        travelled += kotlin.math.abs(deltaDp)
        if (travelled >= spacingDp) { travelled = 0f; tick(.22f) }
    }
    fun tick(scale: Float = .35f) {
        if (!enabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 31 && vibrator.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK).all { it }) {
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scale).compose())
        } else vibrator.vibrate(VibrationEffect.createOneShot(8, (scale * 100).toInt().coerceIn(1, 255)))
    }
    /** Crisp click for selecting or stacking a card. */
    fun select() {
        if (!enabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        else vibrator.vibrate(VibrationEffect.createOneShot(12, 120))
    }
    fun confirm() {
        if (!enabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 31 && vibrator.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_THUD).all { it }) {
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, .65f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, .7f, 20).compose())
        } else vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 12, 20, 22), intArrayOf(0, 90, 0, 150), -1))
    }
}
