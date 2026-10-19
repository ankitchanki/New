package com.vibe.stashdrop.core.physics

import kotlin.math.*

data class Vector2(val x: Float, val y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(value: Float) = Vector2(x * value, y * value)
}
data class MagneticFlight(val start: Vector2, val control1: Vector2, val control2: Vector2, val end: Vector2) {
    fun position(progress: Float): Vector2 {
        val t = progress.coerceIn(0f, 1f); val s = 1 - t
        return start * (s*s*s) + control1 * (3*s*s*t) + control2 * (3*s*t*t) + end * (t*t*t)
    }
    fun tangent(progress: Float): Vector2 {
        val t = progress.coerceIn(0f, 1f); val s = 1-t
        return (control1-start)*(3*s*s) + (control2-control1)*(6*s*t) + (end-control2)*(3*t*t)
    }
}
object MagneticTrajectoryEngine {
    fun flight(start: Vector2, target: Vector2, velocity: Vector2): MagneticFlight {
        val distance = hypot(target.x-start.x, target.y-start.y)
        val speed = hypot(velocity.x, velocity.y)
        val projected = if (speed > .01f) velocity * (min(distance*.55f, speed*.12f)/speed) else Vector2(0f, -distance*.3f)
        return MagneticFlight(start, start+projected, target+Vector2(0f, -distance*.25f), target)
    }
    fun tilt(velocity: Float): Float = (velocity / 150f).coerceIn(-14f, 14f)
}
