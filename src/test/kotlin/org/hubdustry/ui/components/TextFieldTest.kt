package org.hubdustry.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import arc.graphics.Color
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.view.ComposeView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextFieldTest {

    @Test
    fun testTextFieldRenderBoundsAndPlaceholder() {
        val view = ComposeView()
        try {
            val textState = mutableStateOf("")

            view.setContent {
                val text by remember { textState }
                TextField(
                    value = text,
                    onValueChange = { textState.value = it },
                    placeholder = "Enter text..."
                )
            }

            CompositionManager.frame()
            view.setSize(200f, 50f)
            view.layout()

            val fieldNode = view.rootLayoutNode.children[0]
            assertEquals(200f, fieldNode.width)
            assertEquals(50f, fieldNode.height)
            assertEquals(DefaultTextFieldBgColor, fieldNode.backgroundColor)
            assertEquals(DefaultTextFieldBorderColor, fieldNode.borderColor)

            // Phải có child hiển thị placeholder text
            val placeholderNode = fieldNode.children.firstOrNull { it.text != null }
            assertTrue(placeholderNode != null, "Phải tìm thấy node hiển thị placeholder")
            assertEquals("Enter text...", placeholderNode.text.toString())
            assertEquals(Color.gray, placeholderNode.textColor)
        } finally {
            view.dispose()
        }
    }

    @Test
    fun testTextFieldTapFocusAndTyping() {
        val view = ComposeView()
        try {
            val textState = mutableStateOf("")

            view.setContent {
                val text by remember { textState }
                TextField(
                    value = text,
                    onValueChange = { textState.value = it }
                )
            }

            CompositionManager.frame()
            view.setSize(200f, 50f)
            view.layout()

            // 1. Nhấn chuột vào TextField để kích hoạt focus
            view.sendPointerInput(PointerEventType.Press, 50f, 25f)
            view.sendPointerInput(PointerEventType.Release, 50f, 25f)
            CompositionManager.frame()

            // 2. Gõ ký tự thông qua inputDispatcher
            view.inputDispatcher.keyTyped('H')
            view.inputDispatcher.keyTyped('i')

            assertEquals("Hi", textState.value)
        } finally {
            view.dispose()
        }
    }

    @Test
    fun testTextFieldDisabledBlocksInteraction() {
        val view = ComposeView()
        try {
            val textState = mutableStateOf("Initial")

            view.setContent {
                val text by remember { textState }
                TextField(
                    value = text,
                    onValueChange = { textState.value = it },
                    enabled = false
                )
            }

            CompositionManager.frame()
            view.setSize(200f, 50f)
            view.layout()

            val fieldNode = view.rootLayoutNode.children[0]
            assertEquals(0.5f, fieldNode.alpha)

            // Thử click
            view.sendPointerInput(PointerEventType.Press, 50f, 25f)
            view.sendPointerInput(PointerEventType.Release, 50f, 25f)
            CompositionManager.frame()

            // Gõ phím không được thay đổi text
            view.inputDispatcher.keyTyped('X')
            assertEquals("Initial", textState.value)
        } finally {
            view.dispose()
        }
    }

    @Test
    fun testTextFieldFocusWithActLoop() {
        val view = ComposeView()
        try {
            val textState = mutableStateOf("")

            view.setContent {
                val text by remember { textState }
                TextField(
                    value = text,
                    onValueChange = { textState.value = it }
                )
            }

            view.setSize(200f, 50f)
            view.act(0.016f)

            // 1. Nhấn chuột vào TextField để kích hoạt focus
            view.sendPointerInput(PointerEventType.Press, 50f, 25f)
            view.sendPointerInput(PointerEventType.Release, 50f, 25f)

            // 2. Mô phỏng game loop tick act() như trong runtime thực tế
            view.act(0.016f)

            // 3. Gõ phím
            val typed = view.inputDispatcher.keyTyped('A')
            assertTrue(typed, "Phím phải được tiêu thụ bởi focusedKeyHandler")
            assertEquals("A", textState.value)
        } finally {
            view.dispose()
        }
    }
}

