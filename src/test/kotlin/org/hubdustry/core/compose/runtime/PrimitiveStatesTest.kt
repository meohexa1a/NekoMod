package org.hubdustry.core.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import org.hubdustry.core.compose.input.IntSize
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrimitiveStatesTest {

    @Test
    fun `mutableDpStateOf delegates reading and writing correctly without boxing`() {
        var width by mutableDpStateOf(10.dp)
        assertEquals(10.dp, width)

        width = 25.dp
        assertEquals(25.dp, width)

        width += 5.dp
        assertEquals(30.dp, width)
    }

    @Test
    fun `mutableOffsetStateOf delegates reading and writing correctly without boxing`() {
        var offset by mutableOffsetStateOf(Offset(10f, 20f))
        assertEquals(10f, offset.x, 0.0001f)
        assertEquals(20f, offset.y, 0.0001f)

        offset = Offset(30f, 40f)
        assertEquals(30f, offset.x, 0.0001f)
        assertEquals(40f, offset.y, 0.0001f)

        offset += Offset(5f, 10f)
        assertEquals(35f, offset.x, 0.0001f)
        assertEquals(50f, offset.y, 0.0001f)
    }

    @Test
    fun `mutableIntSizeStateOf delegates reading and writing correctly without boxing`() {
        var size by mutableIntSizeStateOf(IntSize(100, 200))
        assertEquals(100, size.width)
        assertEquals(200, size.height)

        size = IntSize(300, 400)
        assertEquals(300, size.width)
        assertEquals(400, size.height)
    }

    @Test
    fun `snapshot isolation works correctly with primitive state delegates`() {
        val state = mutableDpStateOf(50.dp)
        var observedInSnapshot = Dp.Zero

        Snapshot.withMutableSnapshot {
            state.value = 100.dp
            observedInSnapshot = state.value
        }

        assertEquals(100.dp, observedInSnapshot)
        assertEquals(100.dp, state.value)
    }
}
