package com.vibe.stashdrop.core.physics

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.sqrt

object IosSpringSpecs {
    fun <T> follow() = spring<T>(dampingRatio = .78f, stiffness = 650f)
    fun <T> liquid(intensity: Float = 1f) = spring<T>(dampingRatio = (.68f / intensity).coerceIn(.45f, .95f), stiffness = 380f)
    fun <T> landing() = spring<T>(dampingRatio = .62f, stiffness = 420f)
    /** CASpringAnimation damping coefficient c = 2 ζ sqrt(k m), mass normalized to 1. */
    fun damping(stiffness: Float, dampingRatio: Float): Float = 2 * dampingRatio * sqrt(stiffness)
}
