package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.clickable
import org.hubdustry.core.compose.input.collectIsHoveredAsState
import org.hubdustry.core.compose.input.collectIsPressedAsState
import org.hubdustry.core.compose.input.hoverable
import org.hubdustry.core.compose.modifier.BoxScope
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.graphics.RoundedCorners

internal val DefaultButtonCorners = RoundedCorners(6f)
internal val DefaultButtonPressedColor: Color by lazy { Color.royal.cpy().mul(0.75f) }

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
    corners: RoundedCorners = DefaultButtonCorners,
    backgroundColor: Color = Color.royal,
    pressedColor: Color = DefaultButtonPressedColor,
    hoverColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()

    val effectiveHoverColor = hoverColor ?: remember(backgroundColor) { backgroundColor.cpy().mul(1.2f) }
    val currentColor = when {
        isPressed -> pressedColor
        isHovered -> effectiveHoverColor
        else -> backgroundColor
    }

    Box(
        modifier = modifier
            .background(currentColor, corners)
            .hoverable(interactionSource = source, enabled = enabled)
            .clickable(interactionSource = source, enabled = enabled, onClick = onClick),
        content = content
    )
}
