package org.hubdustry.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hubdustry.core.compose.CompositionManager
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.input.Interaction
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PressInteraction
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.ui.Checkbox
import org.hubdustry.ui.Switch
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SelectionControlsTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CHECKBOX TESTS
    // ─────────────────────────────────────────────────────────────────────────

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
        assertEquals(0, checkboxNode.children.size, "Khi unchecked, không có text checkmark con")

        // 2. Click lần 1 -> chuyển sang checked (true)
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()
        view.layout()

        assertTrue(checkedState.value, "Trạng thái checkedState phải chuyển sang true sau click")
        assertEquals(Color.royal, checkboxNode.backgroundColor, "Màu nền khi checked phải là checkedColor (royal)")
        assertEquals(0f, checkboxNode.borderWidth, "Khi checked, không còn hiển thị viền")
        assertEquals(1, checkboxNode.children.size, "Khi checked, phải hiển thị 1 node con chứa dấu check")

        val checkmarkNode = checkboxNode.children[0]
        assertEquals("✓", checkmarkNode.text, "Ký tự checkmark phải là '✓'")
        assertEquals(Color.white, checkmarkNode.textColor, "Màu ký tự checkmark phải là checkmarkColor (white)")

        // 3. Click lần 2 -> chuyển về unchecked (false)
        view.sendPointerInput(PointerEventType.Press, 10f, 10f)
        view.sendPointerInput(PointerEventType.Release, 10f, 10f)
        CompositionManager.frame()
        view.layout()

        assertFalse(checkedState.value, "Trạng thái checkedState phải toggle trở lại false sau click thứ hai")
        assertEquals(Color.darkGray, checkboxNode.backgroundColor)
        assertEquals(1.5f, checkboxNode.borderWidth)
        assertEquals(0, checkboxNode.children.size)

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

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SWITCH TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testSwitchRenderBoundsAndThumbPosition() {
        val view = ComposeView()
        val switchState = mutableStateOf(false)

        view.setContent {
            val checked by remember { switchState }
            Switch(
                checked = checked,
                onCheckedChange = { switchState.value = it }
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val trackNode = view.rootLayoutNode.children[0]

        // 1. Kiểm tra kích thước khung ray (Track) khi unchecked
        assertEquals(40f, trackNode.width, "Track của Switch phải có chiều rộng chuẩn 40px")
        assertEquals(22f, trackNode.height, "Track của Switch phải có chiều cao chuẩn 22px")
        assertEquals(Color.darkGray, trackNode.backgroundColor, "Màu nền của Track khi unchecked phải là trackUncheckedColor")

        // 2. Kiểm tra kích thước và vị trí nút trượt (Thumb) khi unchecked
        assertEquals(1, trackNode.children.size, "Track phải chứa đúng 1 node con là Thumb")
        val thumbNode = trackNode.children[0]
        assertEquals(16f, thumbNode.width, "Thumb phải có chiều rộng chuẩn 16px")
        assertEquals(16f, thumbNode.height, "Thumb phải có chiều cao chuẩn 16px")
        assertEquals(Color.white, thumbNode.backgroundColor, "Màu của Thumb phải là thumbColor (white)")

        // Khi unchecked: Thumb ở START (padding 3px -> x = 3px, y = 3px)
        assertEquals(3f, thumbNode.x, "Thumb phải nằm sát mép trái (x = 3px) khi unchecked")
        assertEquals(3f, thumbNode.y, "Thumb phải được căn giữa dọc (y = 3px)")

        // 3. Click chuyển trạng thái sang checked (true)
        view.sendPointerInput(PointerEventType.Press, 20f, 11f)
        view.sendPointerInput(PointerEventType.Release, 20f, 11f)
        CompositionManager.frame()
        view.layout()

        assertTrue(switchState.value, "Trạng thái switchState phải chuyển sang true sau click")
        assertEquals(Color.royal, trackNode.backgroundColor, "Màu Track khi checked phải chuyển sang trackCheckedColor (royal)")

        // Khi checked: Thumb ở END (x = 21px, y = 3px -> mép phải cách biên đúng 3px: 40 - (21 + 16) = 3px)
        assertEquals(21f, thumbNode.x, "Thumb phải nằm sát mép phải (x = 21px) khi checked")
        assertEquals(3f, thumbNode.y, "Thumb vẫn phải duy trì căn giữa dọc (y = 3px)")

        // 4. Click chuyển ngược lại về unchecked (false)
        view.sendPointerInput(PointerEventType.Press, 20f, 11f)
        view.sendPointerInput(PointerEventType.Release, 20f, 11f)
        CompositionManager.frame()
        view.layout()

        assertFalse(switchState.value, "Trạng thái switchState phải toggle về false sau click thứ hai")
        assertEquals(Color.darkGray, trackNode.backgroundColor)
        assertEquals(3f, thumbNode.x, "Thumb phải quay trở lại mép trái (x = 3px)")

        view.dispose()
    }

    @Test
    fun testSwitchDisabledStateBlocksInteraction() {
        val view = ComposeView()
        var toggled = false

        view.setContent {
            Switch(
                checked = false,
                onCheckedChange = { toggled = true },
                enabled = false
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        val trackNode = view.rootLayoutNode.children[0]
        assertEquals(0.5f, trackNode.alpha, "Khi disabled, alpha của Switch phải giảm còn 0.5f")

        // Gửi click vào giữa Switch (20, 11)
        view.sendPointerInput(PointerEventType.Press, 20f, 11f)
        view.sendPointerInput(PointerEventType.Release, 20f, 11f)
        CompositionManager.frame()

        assertFalse(toggled, "Callback onCheckedChange của Switch TUYỆT ĐỐI KHÔNG được kích hoạt khi enabled = false")

        view.dispose()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. INTERACTION SOURCE & CUSTOM CONFIGURATION TESTS
    // ─────────────────────────────────────────────────────────────────────────

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
    fun testSwitchEmitsPressInteractionsToCustomSource() = runTest {
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
            Switch(
                checked = false,
                onCheckedChange = { },
                interactionSource = interactionSource
            )
        }

        CompositionManager.frame()
        view.setSize(100f, 100f)
        view.layout()

        // Click xuống
        view.sendPointerInput(PointerEventType.Press, 20f, 11f)
        runCurrent()

        // Nhả ra
        view.sendPointerInput(PointerEventType.Release, 20f, 11f)
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
            Switch(
                checked = true,
                onCheckedChange = null,
                modifier = Modifier.size(60f, 30f),
                trackCheckedColor = Color.coral,
                thumbColor = Color.yellow
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

        val switchNode = view.rootLayoutNode.children[1]
        assertEquals(60f, switchNode.width, "Switch phải tôn trọng size tùy biến 60px")
        assertEquals(30f, switchNode.height, "Switch phải tôn trọng size tùy biến 30px")
        assertEquals(Color.coral, switchNode.backgroundColor, "Track phải hiển thị trackCheckedColor tùy biến")
        assertEquals(Color.yellow, switchNode.children[0].backgroundColor, "Thumb phải hiển thị thumbColor tùy biến")

        view.dispose()
    }
}
