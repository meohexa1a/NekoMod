package org.hubdustry.core.compose.input.gestures

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VelocityTrackerTest {

    @Test
    fun testInitialVelocityIsZero() {
        val tracker = VelocityTracker1D()
        assertEquals(0f, tracker.calculateVelocity())

        // 1 sample is not enough to determine velocity
        tracker.addPosition(1000L, 50f)
        assertEquals(0f, tracker.calculateVelocity())
    }

    @Test
    fun testConstantVelocityCalculation() {
        val tracker = VelocityTracker1D(sampleWindowMillis = 200L)

        // Di chuyển 100px trong 100ms -> Vận tốc lý thuyết: 1000 px/s
        val t0 = 1000L
        for (i in 0..5) {
            val time = t0 + i * 20L // 0, 20, 40, 60, 80, 100 ms
            val pos = i * 20f       // 0, 20, 40, 60, 80, 100 px
            tracker.addPosition(time, pos)
        }

        val velocity = tracker.calculateVelocity()
        assertTrue(abs(velocity - 1000f) < 50f, "Vận tốc tính được phải xấp xỉ 1000 px/s (thực tế: $velocity)")
    }

    @Test
    fun testNegativeVelocityCalculation() {
        val tracker = VelocityTracker1D(sampleWindowMillis = 200L)

        // Di chuyển ngược chiều: -150px trong 100ms -> -1500 px/s
        val t0 = 1000L
        for (i in 0..5) {
            val time = t0 + i * 20L
            val pos = 300f - i * 30f
            tracker.addPosition(time, pos)
        }

        val velocity = tracker.calculateVelocity()
        assertTrue(abs(velocity - (-1500f)) < 50f, "Vận tốc phải xấp xỉ -1500 px/s (thực tế: $velocity)")
    }

    @Test
    fun testNeverThrowWithNaNAndInfinities() {
        val tracker = VelocityTracker1D()

        tracker.addPosition(1000L, Float.NaN)
        tracker.addPosition(1020L, Float.POSITIVE_INFINITY)
        tracker.addPosition(1040L, Float.NEGATIVE_INFINITY)
        assertEquals(0f, tracker.calculateVelocity())

        tracker.addPosition(1060L, 100f)
        tracker.addPosition(1080L, 120f)
        val v = tracker.calculateVelocity()
        assertTrue(!v.isNaN() && !v.isInfinite())
    }

    @Test
    fun testResetClearsHistory() {
        val tracker = VelocityTracker1D()
        tracker.addPosition(1000L, 0f)
        tracker.addPosition(1050L, 50f)
        assertTrue(tracker.calculateVelocity() > 0f)

        tracker.reset()
        assertEquals(0f, tracker.calculateVelocity(), "Sau reset, vận tốc phải về 0f")
    }
}
