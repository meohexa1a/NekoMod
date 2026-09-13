package org.hubdustry.core.compose.modifier

import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RectangleShape
import org.hubdustry.core.graphics.RoundedCornerShape
import org.hubdustry.core.graphics.Shape
import org.hubdustry.core.layout.LayoutNode

data class BackgroundModifier(
    val color: Color,
    val shape: Shape? = null
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.backgroundColor = color
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

data class AlphaModifier(
    val alpha: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.alpha = alpha
    }
}

/**
 * Đặt màu nền và hình dạng bo góc cho widget.
 * Nếu [shape] là null (mặc định), giữ nguyên bo góc sẵn có của widget.
 */
fun Modifier.background(color: Color, shape: Shape? = null): Modifier =
    this.then(BackgroundModifier(color, shape))

/**
 * Đặt độ trong suốt (opacity / alpha) cho widget trong khoảng [0f, 1f].
 */
fun Modifier.alpha(alpha: Float): Modifier = this.then(AlphaModifier(alpha))

data class VisibleModifier(
    val visible: Boolean
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.visible = visible
    }
}

/**
 * Điều khiển tính hiển thị của widget trên cả cây render và chuỗi hit-test tương tác con trỏ.
 */
fun Modifier.visible(visible: Boolean): Modifier = this.then(VisibleModifier(visible))

