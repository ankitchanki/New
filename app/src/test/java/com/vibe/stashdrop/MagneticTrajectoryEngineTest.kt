package com.vibe.stashdrop

import com.vibe.stashdrop.core.physics.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class MagneticTrajectoryEngineTest {
    @Test fun flightMeetsEndpointsAndClamps() {
        val a=Vector2(5f,30f); val b=Vector2(90f,120f)
        val flight=MagneticTrajectoryEngine.flight(a,b,Vector2(500f,-900f))
        assertEquals(a,flight.position(0f)); assertEquals(b,flight.position(1f))
        assertEquals(a,flight.position(-1f)); assertEquals(b,flight.position(2f))
    }
    @Test fun preservesInitialVelocityDirection() {
        val velocity=Vector2(700f,-300f)
        val tangent=MagneticTrajectoryEngine.flight(Vector2(0f,0f),Vector2(100f,200f),velocity).tangent(0f)
        assertEquals(velocity.x/velocity.y,tangent.x/tangent.y,.0001f)
    }
    @Test fun veryHighVelocityIsBounded() {
        val flight=MagneticTrajectoryEngine.flight(Vector2(0f,0f),Vector2(100f,0f),Vector2(1e8f,0f))
        assertTrue(hypot(flight.control1.x,flight.control1.y)<=55.001f)
    }
    @Test fun zeroLengthFlightIsStable() {
        val p=Vector2(50f,50f); val flight=MagneticTrajectoryEngine.flight(p,p,Vector2(0f,0f))
        for(i in 0..100) { val point=flight.position(i/100f); assertEquals(p.x,point.x,.0001f); assertEquals(p.y,point.y,.0001f) }
    }
    @Test fun tiltHasPhysicalBounds() { assertEquals(14f,MagneticTrajectoryEngine.tilt(1e7f),0f); assertEquals(-14f,MagneticTrajectoryEngine.tilt(-1e7f),0f) }
}
