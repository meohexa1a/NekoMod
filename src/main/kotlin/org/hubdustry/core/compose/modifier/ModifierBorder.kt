package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RectangleShape
import org.hubdustry.core.graphics.RoundedCornerShape
import org.hubdustry.core.graphics.Shape
import org.hubdustry.core.layout.LayoutNode

data class BorderModifier(
    val width: Float,
    val color: Color,
    val shape: Shape? = null
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.borderWidth = width
        node.borderColor = color
        when (shape) {
            is RoundedCornerShape -> {
                node.setCornerRadius(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
            }
            RectangleShape -> {
                node.setCornerRadius(0f)
            }
            null -> {
                // Bảo tồn bo góc đã được thiết lập bởi modifier trước đó
            }
        }
    }
}

/**
 * Đặt viền (Border) cho container với độ dày [width], màu sắc [color] và hình dạng [shape].
 * Nếu [shape] là null (mặc định), viền sẽ tuân theo hình dạng bo góc sẵn có của container.
 */
fun Modifier.border(width: Float, color: Color, shape: Shape? = null): Modifier =
    this.then(BorderModifier(width, color, shape))
