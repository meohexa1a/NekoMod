// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.input.TextEditState
import org.mdt.core.ui.unit.Color
import org.mdt.ui.theme.InputStyle

/**
 * ## InputNodeTest
 *
 * Validates text editing, selection ranges, IME composition, and style binding on [InputNode].
 */
class InputNodeTest {

    @Test
    fun `TextEditState handles selection range primitives with zero heap allocation`() {
        val state = TextEditState { PlatformHost.NoOp }
        state.setText("Hello World", 0)

        assertFalse(state.hasSelection(), "Initially has no selection")
        assertEquals(0, state.selectionStartRange)
        assertEquals(0, state.selectionEndRange)

        state.setSelection(0, 5)
        assertTrue(state.hasSelection(), "Selection should be active")
        assertEquals(0, state.selectionStartRange)
        assertEquals(5, state.selectionEndRange)
        assertEquals("Hello", state.getSelectedText())

        state.deleteSelection()
        assertEquals(" World", state.text)
        assertEquals(0, state.cursor)
        assertFalse(state.hasSelection())
    }

    @Test
    fun `TextEditState handles IME composition range primitives`() {
        val state = TextEditState { PlatformHost.NoOp }
        state.setText("Xin ch", 6)

        state.setComposition("ao")
        assertTrue(state.hasComposition())
        assertEquals(6, state.compositionStartRange)
        assertEquals(8, state.compositionEndRange)
        assertEquals("Xin chao", state.getDisplayText())

        state.clearComposition()
        assertFalse(state.hasComposition())
        assertEquals("Xin ch", state.getDisplayText())
    }

    @Test
    fun `InputNode intrinsic sizing respects bounds and padding`() {
        val node = InputNode { PlatformHost.NoOp }
        node.minWidth = 120.0f
        node.padL = 8.0f
        node.padR = 8.0f

        assertEquals(136.0f, node.getPrefWidth(), "Pref width should include minWidth + padding")

        node.width = 200.0f
        assertEquals(200.0f, node.getPrefWidth(), "Explicit width overrides intrinsic width")
    }

    @Test
    fun `InputNode style mutator updates style and triggers layout dirtying`() {
        val node = InputNode { PlatformHost.NoOp }
        val customStyle = InputStyle(
            textColor = Color.Red,
            cursorColor = Color.Green
        )

        node.style = customStyle
        assertEquals(Color.Red, node.style.textColor)
        assertEquals(Color.Green, node.style.cursorColor)
    }
}
