package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import arc.graphics.Color
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.alpha
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.border
import org.hubdustry.core.compose.modifier.clickable
import org.hubdustry.core.compose.modifier.hoverable
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment

internal val DefaultCheckboxCorners = RoundedCorners(4f)

/**
 * Checkbox chuẩn Jetpack Compose cho NekoMod UI:
 * - Kích thước mặc định 20x20 px với bo góc SDF 4px ([DefaultCheckboxCorners]).
 * - Khi [checked] = true: Nền [checkedColor], hiển thị ký tự "✓" màu [checkmarkColor] căn chính giữa.
 * - Khi [checked] = false: Nền [uncheckedColor] với viền độ dày 1.5px màu [borderColor].
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 * - Tự động giảm độ mờ (alpha 0.5f) và vô hiệu hóa tương tác khi [enabled] = false.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    checkedColor: Color = Color.royal,
    uncheckedColor: Color = Color.darkGray,
    checkmarkColor: Color = Color.white,
    borderColor: Color = Color.lightGray
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }

    val visualModifier = if (checked) {
        Modifier.background(checkedColor, DefaultCheckboxCorners)
    } else {
        Modifier
            .background(uncheckedColor, DefaultCheckboxCorners)
            .border(1.5f, borderColor, DefaultCheckboxCorners)
    }

    val interactiveModifier = if (enabled && onCheckedChange != null) {
        Modifier
            .hoverable(interactionSource = source, enabled = enabled)
            .clickable(interactionSource = source, enabled = enabled) {
                onCheckedChange(!checked)
            }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(20f)
            .then(modifier)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .then(visualModifier)
            .then(interactiveModifier)
    ) {
        if (checked) {
            Text(
                text = "✓",
                textColor = checkmarkColor,
                modifier = Modifier.align(Alignment.CENTER)
            )
        }
    }
}
