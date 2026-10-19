package com.vibe.stashdrop.core.haptics

import android.content.Context
import android.os.*

class TactileHapticEngine(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator else context.getSystemService(Vibrator::class.java)
    private var lastStep = 0
    fun reset() { lastStep = 0 }
    fun tension(distanceDp: Float) {
        val step = (distanceDp / 12).toInt()
        if (step > lastStep) { tick(); lastStep = step }
    }
    fun tick() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 31 && vibrator.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK).all { it }) {
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, .35f).compose())
        } else vibrator.vibrate(VibrationEffect.createOneShot(8, 35))
    }
    fun confirm() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 31 && vibrator.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_THUD).all { it }) {
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, .65f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, .7f, 20).compose())
        } else vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 12, 20, 22), intArrayOf(0, 90, 0, 150), -1))
    }
}
