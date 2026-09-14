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

// ─── Checkbox Defaults ────────────────────────────────────────────

object CheckboxDefaults {
    const val SIZE: Float = 20f
    const val BORDER_WIDTH: Float = 1.5f
    const val DISABLED_ALPHA: Float = 0.5f

    val corners: RoundedCorners = RoundedCorners(4f)
    val checkedColor: Color get() = Color.royal
    val uncheckedColor: Color get() = Color.darkGray
    val checkmarkColor: Color get() = Color.white
    val borderColor: Color get() = Color.lightGray
}

// ─── Checkbox Component ───────────────────────────────────────────

/**
 * Checkbox chuẩn Jetpack Compose cho NekoMod UI:
 * - Kích thước mặc định 20x20 px với bo góc SDF 4px ([CheckboxDefaults.corners]).
 * - Khi [checked] = true: Nền [checkedColor], hiển thị ký tự "✓" màu [checkmarkColor] căn chính giữa.
 * - Khi [checked] = false: Nền [uncheckedColor], viền [borderColor] dày 1.5px.
 * - Tự động phát sinh các tương tác [PressInteraction] và [HoverInteraction] lên [interactionSource].
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    corners: RoundedCorners = CheckboxDefaults.corners,
    checkedColor: Color = CheckboxDefaults.checkedColor,
    uncheckedColor: Color = CheckboxDefaults.uncheckedColor,
    checkmarkColor: Color = CheckboxDefaults.checkmarkColor,
    borderColor: Color = CheckboxDefaults.borderColor
) {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val visualModifier = if (checked) {
        Modifier
            .background(color = checkedColor, corners = corners)
            .border(width = 0f, color = Color.clear, corners = corners)
    } else {
        Modifier
            .background(color = uncheckedColor, corners = corners)
            .border(width = CheckboxDefaults.BORDER_WIDTH, color = borderColor, corners = corners)
    }

    val interactiveModifier = if (enabled && onCheckedChange != null) {
        Modifier
            .hoverable(interactionSource = currentInteractionSource, enabled = enabled)
            .clickable(interactionSource = currentInteractionSource, enabled = enabled) {
                onCheckedChange(!checked)
            }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(CheckboxDefaults.SIZE)
            .then(modifier)
            .then(if (!enabled) Modifier.alpha(CheckboxDefaults.DISABLED_ALPHA) else Modifier)
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

