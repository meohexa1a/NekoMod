package org.hubdustry.core.compose.unit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DpTest {

    @Test
    fun `arithmetic operators calculate correctly`() {
        assertEquals(15.dp, 10.dp + 5.dp)
        assertEquals(15.dp, 20.dp - 5.dp)
        assertEquals(20.dp, 10.dp * 2f)
        assertEquals(20.dp, 10.dp * 2)
        assertEquals(20.dp, 2f * 10.dp)
        assertEquals(20.dp, 2 * 10.dp)
        assertEquals(5.dp, 10.dp / 2f)
        assertEquals(5.dp, 10.dp / 2)
        assertEquals(5f, 10.dp / 2.dp, 0.0001f)
        assertEquals(Dp(-10f), -10.dp)
    }

    @Test
    fun `comparison and clamping functions behave correctly`() {
        assertTrue(10.dp < 20.dp)
        assertTrue(20.dp > 10.dp)
        assertEquals(10.dp, 15.dp.coerceIn(0.dp, 10.dp))
        assertEquals(0.dp, (-5).dp.coerceIn(0.dp, 10.dp))
        assertEquals(10.dp, 5.dp.coerceAtLeast(10.dp))
        assertEquals(10.dp, 15.dp.coerceAtMost(10.dp))
        assertEquals(15.dp, maxOf(5.dp, 15.dp))
        assertEquals(5.dp, minOf(5.dp, 15.dp))
    }

    @Test
    fun `gateway sanitization handles NaN and max values safely while preserving negative vectors`() {
        assertEquals(0f, Dp(Float.NaN).toPx, 0.0001f)
        assertEquals(-10f, Dp(-10f).toPx, 0.0001f)
        assertEquals(Float.MAX_VALUE, Dp(Float.MAX_VALUE).toPx)

        assertEquals(Dp(0f), Dp.Hairline)
        assertEquals(Dp(Float.MAX_VALUE), Dp.Infinity)

        assertTrue(Dp.Unspecified.isUnspecified)
        assertFalse(Dp.Unspecified.isSpecified)
        assertTrue(10.dp.isSpecified)
        assertFalse(10.dp.isUnspecified)
    }

    @Test
    fun `convenience extension properties produce correct Dp`() {
        assertEquals(10f, 10.dp.value)
        assertEquals(10.5f, 10.5f.dp.value)
        assertEquals(12f, 12.dp.value)
        assertEquals(15f, 15.0.dp.value)
    }

    @Test
    fun `toString formats specified and unspecified properly`() {
        assertEquals("10.0.dp", 10.dp.toString())
        assertEquals("Dp.Unspecified", Dp.Unspecified.toString())
    }
}
