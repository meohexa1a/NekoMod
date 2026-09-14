package org.hubdustry.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import arc.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.runtime.CompositionManager
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.input.Interaction
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PressInteraction
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.view.ComposeView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SelectionControlsTest {

    @Test
    fun testCheckboxRenderBoundsAndToggleClick() {
        val view = ComposeView()
        val checkedState = mutableStateOf(false)

        view.setContent {
            val checked by remember { checkedState }
            Checkbox(
                checked = checked,
                onCheckedChange = { checkedState.value = it }
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val checkboxNode = view.rootLayoutNode.children[0]

        // 1. Kiểm tra kích thước và trạng thái khi unchecked (false)
        assertEquals(20f, checkboxNode.width, "Checkbox mặc định phải có chiều rộng 20px")
        assertEquals(20f, checkboxNode.height, "Checkbox mặc định phải có chiều cao 20px")
        assertEquals(Color.darkGray, checkboxNode.backgroundColor, "Màu nền khi unchecked phải là uncheckedColor (darkGray)")
        assertEquals(1.5f, checkboxNode.borderWidth, "Độ dày viền khi unchecked phải là 1.5px")
        assertEquals(Color.lightGray, checkboxNode.borderColor, "Màu viền khi unchecked phải là borderColor (lightGray)")
        assertTrue(checkboxNode.children.none { it.text == "✓" }, "Khi unchecked, không có text checkmark con")

        // 2. Click lần 1 -> chuyển sang checked (true)
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()
        view.layout()

        assertTrue(checkedState.value, "Trạng thái checkedState phải chuyển sang true sau click")
        assertEquals(Color.royal, checkboxNode.backgroundColor, "Màu nền khi checked phải là checkedColor (royal)")
        assertEquals(0f, checkboxNode.borderWidth, "Khi checked, không còn hiển thị viền")

        val checkmarkNode = checkboxNode.children.firstOrNull { it.text == "✓" }
        assertTrue(checkmarkNode != null, "Khi checked, phải có checkmark '✓'")
        assertEquals(Color.white, checkmarkNode.textColor, "Màu ký tự checkmark phải là checkmarkColor (white)")

        // 3. Click lần 2 -> chuyển về unchecked (false)
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()
        view.layout()

        assertFalse(checkedState.value, "Trạng thái checkedState phải toggle trở lại false sau click thứ hai")
        assertEquals(Color.darkGray, checkboxNode.backgroundColor)
        assertEquals(1.5f, checkboxNode.borderWidth)
        assertTrue(checkboxNode.children.none { it.text == "✓" })

        view.dispose()
    }

    @Test
    fun testCheckboxDisabledStateBlocksInteraction() {
        val view = ComposeView()
        var clicked = false

        view.setContent {
            Checkbox(
                checked = false,
                onCheckedChange = { clicked = true },
                enabled = false
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val checkboxNode = view.rootLayoutNode.children[0]
        assertEquals(0.5f, checkboxNode.alpha, "Khi disabled, alpha của Checkbox phải giảm còn 0.5f")

        // Gửi click vào giữa checkbox (10, 10)
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()

        assertFalse(clicked, "Callback onCheckedChange TUYỆT ĐỐI KHÔNG được kích hoạt khi enabled = false")

        view.dispose()
    }

    @Test
    fun testCheckboxEmitsPressInteractionsToCustomSource() = runTest {
        val view = ComposeView()
        val interactionSource = MutableInteractionSource()
        val emittedInteractions = ArrayList<Interaction>()

        val job = launch {
            interactionSource.interactions.collect {
                emittedInteractions.add(it)
            }
        }
        runCurrent()

        view.setContent {
            Checkbox(
                checked = false,
                onCheckedChange = { },
                interactionSource = interactionSource
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        // Click xuống
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        runCurrent()

        // Nhả ra
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        runCurrent()

        val pressInteractions = emittedInteractions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size, "Phải phát 2 PressInteraction: Press và Release")
        assertIs<PressInteraction.Press>(pressInteractions[0])
        assertIs<PressInteraction.Release>(pressInteractions[1])

        job.cancel()
        view.dispose()
    }

    @Test
    fun testCustomDimensionsAndColors() {
        val view = ComposeView()

        view.setContent {
            Checkbox(
                checked = true,
                onCheckedChange = null,
                modifier = Modifier.size(32f),
                checkedColor = Color.forest,
                checkmarkColor = Color.gold
            )
        }

        CompositionManager.frame()
        view.setSize(200f, 200f)
        view.layout()

        val checkboxNode = view.rootLayoutNode.children[0]
        assertEquals(32f, checkboxNode.width, "Checkbox phải tôn trọng size tùy biến 32px")
        assertEquals(32f, checkboxNode.height, "Checkbox phải tôn trọng size tùy biến 32px")
        assertEquals(Color.forest, checkboxNode.backgroundColor, "Checkbox phải hiển thị checkedColor tùy biến")
        assertEquals(Color.gold, checkboxNode.children[0].textColor, "Checkmark phải hiển thị checkmarkColor tùy biến")

        view.dispose()
    }
}
