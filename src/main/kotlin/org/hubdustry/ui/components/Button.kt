package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import arc.graphics.Color
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.collectIsHoveredAsState
import org.hubdustry.core.compose.input.collectIsPressedAsState
import org.hubdustry.core.compose.modifier.BoxScope
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.clickable
import org.hubdustry.core.compose.modifier.hoverable
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.graphics.RoundedCorners

// ─── Button Defaults ──────────────────────────────────────────────
 
object ButtonDefaults {
    const val HOVER_BRIGHTNESS_FACTOR: Float = 1.2f
    val corners: RoundedCorners = RoundedCorners(6f)
    val backgroundColor: Color get() = Color.royal
    val pressedColor: Color by lazy { Color.royal.cpy().mul(0.75f) }
}

// ─── Button Component ─────────────────────────────────────────────

/**
 * Nút bấm chuẩn của NekoMod UI:
 * - Tự động đổi màu khi bị nhấn (pressed) và khi rê chuột qua (hovered) dựa trên [MutableInteractionSource].
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 * - Hỗ trợ hình dạng bo góc SDF tự nhiên qua [corners].
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    corners: RoundedCorners = ButtonDefaults.corners,
    backgroundColor: Color = ButtonDefaults.backgroundColor,
    pressedColor: Color = ButtonDefaults.pressedColor,
    hoverColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by currentInteractionSource.collectIsPressedAsState()
    val isHovered by currentInteractionSource.collectIsHoveredAsState()

    val effectiveHoverColor = hoverColor ?: remember(backgroundColor) { backgroundColor.cpy().mul(ButtonDefaults.HOVER_BRIGHTNESS_FACTOR) }
    val currentColor = when {
        isPressed -> pressedColor
        isHovered -> effectiveHoverColor
        else -> backgroundColor
    }

    Box(
        modifier = modifier
            .background(currentColor, corners)
            .hoverable(interactionSource = currentInteractionSource, enabled = enabled)
            .clickable(interactionSource = currentInteractionSource, enabled = enabled, onClick = onClick),
        content = content
    )
}

