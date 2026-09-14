package org.hubdustry.core.compose.foundation.text

import arc.input.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextFieldStateTest {

    @Test
    fun testTextRangePrimitivePacking() {
        val range = TextRange(5, 12)
        assertEquals(5, range.start)
        assertEquals(12, range.end)
        assertEquals(5, range.min)
        assertEquals(12, range.max)
        assertEquals(7, range.length)
        assertFalse(range.collapsed)
        assertFalse(range.reversed)

        val reversed = TextRange(15, 3)
        assertEquals(15, reversed.start)
        assertEquals(3, reversed.end)
        assertEquals(3, reversed.min)
        assertEquals(15, reversed.max)
        assertEquals(12, reversed.length)
        assertTrue(reversed.reversed)

        val collapsed = TextRange(8)
        assertTrue(collapsed.collapsed)
        assertEquals(8, collapsed.start)
        assertEquals(8, collapsed.end)

        val clamped = TextRange(-5, 50).coerceIn(0, 20)
        assertEquals(0, clamped.start)
        assertEquals(20, clamped.end)
    }

    @Test
    fun testBasicTypingAndInsertion() {
        var textResult = ""
        val state = TextFieldState("Hello") { textResult = it }
        state.isFocused = true
        assertEquals("Hello", state.text)
        assertEquals(5, state.cursor)

        // Type character at the end
        state.onKeyTyped('!')
        assertEquals("Hello!", state.text)
        assertEquals("Hello!", textResult)
        assertEquals(6, state.cursor)

        // Move cursor and insert
        state.setCursor(0)
        state.insertText(">> ")
        assertEquals(">> Hello!", state.text)
        assertEquals(3, state.cursor)
    }

    @Test
    fun testBackspaceAndDelete() {
        val state = TextFieldState("Kotlin")
        state.isFocused = true
        state.setCursor(6)

        // Backspace
        state.onKeyDown(KeyCode.backspace)
        assertEquals("Kotli", state.text)
        assertEquals(5, state.cursor)

        // Move to start and forward Delete
        state.setCursor(0)
        state.onKeyDown(KeyCode.del)
        assertEquals("otli", state.text)
        assertEquals(0, state.cursor)

        // Delete selection
        state.setSelection(1, 3) // "tl"
        state.onKeyDown(KeyCode.backspace)
        assertEquals("oi", state.text)
        assertEquals(1, state.cursor)
    }

    @Test
    fun testCursorNavigationAndSelection() {
        val state = TextFieldState("One Two Three")
        state.isFocused = true
        state.setCursor(0)

        // Move right
        state.onKeyDown(KeyCode.right)
        assertEquals(1, state.cursor)

        // Move to End
        state.onKeyDown(KeyCode.end)
        assertEquals(13, state.cursor)

        // Move to Home
        state.onKeyDown(KeyCode.home)
        assertEquals(0, state.cursor)

        // Select All
        state.selectAll()
        assertEquals(0, state.selection.min)
        assertEquals(13, state.selection.max)
        assertTrue(state.hasSelection())

        // Select Word
        state.selectWordAt(5) // inside "Two"
        assertEquals(4, state.selection.min)
        assertEquals(7, state.selection.max)
    }

    @Test
    fun testImeCompositionLifecycle() {
        val state = TextFieldState("Xin chao ")
        state.isFocused = true
        state.setCursor(9)

        // Bắt đầu gõ tiếng Việt Telex: candidate "vieetj"
        state.setComposition("vieetj")
        assertTrue(state.hasComposition())
        assertEquals("Xin chao vieetj", state.getDisplayText())
        assertEquals(15, state.getEffectiveCursor())

        val range = state.getCompositionRange()
        assertEquals(TextRange(9, 15), range)

        // Cập nhật candidate tiếp theo: "việt"
        state.setComposition("việt")
        assertEquals("Xin chao việt", state.getDisplayText())
        assertEquals(13, state.getEffectiveCursor())

        // Chốt (commit) chuỗi vào văn bản chính
        state.commitComposition()
        assertFalse(state.hasComposition())
        assertNull(state.getCompositionRange())
        assertEquals("Xin chao việt", state.text)
        assertEquals(13, state.cursor)

        // Thử hủy composition (clear)
        state.setComposition("test")
        assertTrue(state.hasComposition())
        assertEquals("Xin chao việttest", state.getDisplayText())

        state.clearComposition()
        assertFalse(state.hasComposition())
        assertEquals("Xin chao việt", state.getDisplayText())
        assertEquals("Xin chao việt", state.text)
    }

    @Test
    fun testBlinkTimerReset() {
        val state = TextFieldState("Test")
        state.isFocused = true
        assertTrue(state.cursorVisible)

        // Giả lập trôi qua 0.6 giây (> blinkInterval 0.5s)
        state.updateBlink(0.6f)
        assertFalse(state.cursorVisible)

        // Khi người dùng gõ phím, con trỏ lập tức hiện lại
        state.onKeyTyped('a')
        assertTrue(state.cursorVisible)
    }
}
