package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import arc.input.KeyCode
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.view.ComposeView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicTextFieldTest {

    @Test
    fun testBasicTextFieldTypingAndFocus() {
        val view = ComposeView()
        val textState = mutableStateOf("")

        view.setContent {
            val text by remember { textState }
            BasicTextField(
                value = text,
                onValueChange = { textState.value = it }
            )
        }

        view.setSize(200f, 40f)
        view.act(0.016f)

        // 1. Click vào BasicTextField để kích hoạt focus
        view.sendPointerInput(PointerEventType.Press, 20f, 20f)
        view.sendPointerInput(PointerEventType.Release, 20f, 20f)
        view.act(0.016f)

        // 2. Gõ các ký tự
        view.inputDispatcher.keyTyped('N')
        view.inputDispatcher.keyTyped('e')
        view.inputDispatcher.keyTyped('k')
        view.inputDispatcher.keyTyped('o')

        assertEquals("Neko", textState.value)
    }

    @Test
    fun testBasicTextFieldCustomDecorationBoxSlot() {
        val view = ComposeView()
        val textState = mutableStateOf("")
        var decorationBoxInvoked = false

        view.setContent {
            val text by remember { textState }
            BasicTextField(
                value = text,
                onValueChange = { textState.value = it },
                decorationBox = { innerTextField ->
                    decorationBoxInvoked = true
                    innerTextField()
                }
            )
        }

        view.setSize(200f, 40f)
        view.act(0.016f)

        assertTrue(decorationBoxInvoked, "decorationBox slot phải được gọi để trang trí nội dung")

        // Tương tác vẫn hoạt động xuyên suốt decorationBox
        view.sendPointerInput(PointerEventType.Press, 20f, 20f)
        view.sendPointerInput(PointerEventType.Release, 20f, 20f)
        view.act(0.016f)

        view.inputDispatcher.keyTyped('O')
        view.inputDispatcher.keyTyped('K')
        assertEquals("OK", textState.value)
    }

    @Test
    fun testBasicTextFieldEscapeClearsFocus() {
        val view = ComposeView()
        val textState = mutableStateOf("")

        view.setContent {
            val text by remember { textState }
            BasicTextField(
                value = text,
                onValueChange = { textState.value = it }
            )
        }

        view.setSize(200f, 40f)
        view.act(0.016f)

        // Focus
        view.sendPointerInput(PointerEventType.Press, 20f, 20f)
        view.sendPointerInput(PointerEventType.Release, 20f, 20f)
        view.act(0.016f)

        view.inputDispatcher.keyTyped('A')
        assertEquals("A", textState.value)

        // Nhấn phím Escape -> Mất focus
        val handled = view.inputDispatcher.keyDown(KeyCode.escape)
        assertTrue(handled, "Escape phải được tiêu thụ để giải phóng focus")

        // Sau khi mất focus, gõ phím không còn được tiếp nhận
        val typedAfterEscape = view.inputDispatcher.keyTyped('B')
        assertFalse(typedAfterEscape, "Không được nhận phím khi đã mất focus")
        assertEquals("A", textState.value)
    }

    @Test
    fun testBasicTextFieldDisabledBlocksTyping() {
        val view = ComposeView()
        val textState = mutableStateOf("Locked")

        view.setContent {
            val text by remember { textState }
            BasicTextField(
                value = text,
                onValueChange = { textState.value = it },
                enabled = false
            )
        }

        view.setSize(200f, 40f)
        view.act(0.016f)

        // Thử click khi disabled
        view.sendPointerInput(PointerEventType.Press, 20f, 20f)
        view.sendPointerInput(PointerEventType.Release, 20f, 20f)
        view.act(0.016f)

        val typed = view.inputDispatcher.keyTyped('X')
        assertFalse(typed, "Disabled text field không được nhận phím")
        assertEquals("Locked", textState.value)
    }
}
