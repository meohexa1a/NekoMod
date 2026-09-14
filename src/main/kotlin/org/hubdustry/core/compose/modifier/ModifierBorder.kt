package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.LayoutNode

// ─── Public Border Modifiers ──────────────────────────────────────

/**
 * Đặt viền (Border) cho container với độ dày [width], màu sắc [color] và bán kính bo góc [corners].
 * Nếu [corners] là null (mặc định), viền sẽ tuân theo hình dạng bo góc sẵn có của container.
 */
fun Modifier.border(width: Float, color: Color, corners: RoundedCorners? = null): Modifier =
    this.then(
        BorderModifier(
            width = width.coerceAtLeast(0f),
            color = color,
            corners = corners
        )
    )

fun Modifier.border(width: Dp, color: Color, corners: RoundedCorners? = null): Modifier =
    this.border(width = width.toPx, color = color, corners = corners)

// ─── Internal Modifier Elements ────────────────────────────────────

internal data class BorderModifier(
    val width: Float,
    val color: Color,
    val corners: RoundedCorners? = null
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.borderWidth = width
        node.borderColor = color
        if (corners != null) {
            node.setCornerRadius(corners)
        }
    }
}

