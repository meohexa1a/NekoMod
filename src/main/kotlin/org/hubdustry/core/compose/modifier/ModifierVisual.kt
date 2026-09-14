package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.LayoutNode

// ─── Public Visual Modifiers ──────────────────────────────────────

/**
 * Đặt màu nền và bán kính bo góc [corners] cho widget.
 * Nếu [corners] là null (mặc định), giữ nguyên bo góc sẵn có của widget.
 */
fun Modifier.background(color: Color, corners: RoundedCorners? = null): Modifier =
    this.then(BackgroundModifier(color = color, corners = corners))

/**
 * Đặt độ trong suốt (opacity / alpha) cho widget trong khoảng [0f, 1f].
 */
fun Modifier.alpha(alpha: Float): Modifier =
    this.then(AlphaModifier(alpha = alpha.coerceIn(0f, 1f)))

/**
 * Điều khiển tính hiển thị của widget trên cả cây render và chuỗi hit-test tương tác con trỏ.
 */
fun Modifier.visible(visible: Boolean): Modifier =
    this.then(VisibleModifier(visible = visible))

// ─── Internal Modifier Elements ────────────────────────────────────

internal data class BackgroundModifier(
    val color: Color,
    val corners: RoundedCorners? = null
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.backgroundColor = color
        if (corners != null) {
            node.setCornerRadius(corners)
        }
    }
}

internal data class AlphaModifier(
    val alpha: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.alpha = alpha
    }
}

internal data class VisibleModifier(
    val visible: Boolean
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.visible = visible
    }
}

