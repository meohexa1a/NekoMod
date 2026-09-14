package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.LayoutNode

data class BorderModifier(
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

/**
 * Đặt viền (Border) cho container với độ dày [width], màu sắc [color] và bán kính bo góc [corners].
 * Nếu [corners] là null (mặc định), viền sẽ tuân theo hình dạng bo góc sẵn có của container.
 */
fun Modifier.border(width: Float, color: Color, corners: RoundedCorners? = null): Modifier =
    this.then(BorderModifier(width, color, corners))
