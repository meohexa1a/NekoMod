package org.hubdustry.libs.compose.modifier

import arc.graphics.Color
import org.hubdustry.libs.compose.Modifier
import org.hubdustry.libs.layout.LayoutNode

private data class BackgroundModifier(
    val color: Color
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.backgroundColor = color
    }
}

private data class AlphaModifier(
    val alpha: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.alpha = alpha
    }
}

/**
 * Đặt màu nền cho widget.
 */
fun Modifier.background(color: Color): Modifier = this.then(BackgroundModifier(color))

/**
 * Đặt độ trong suốt (opacity / alpha) cho widget trong khoảng [0f, 1f].
 */
fun Modifier.alpha(alpha: Float): Modifier = this.then(AlphaModifier(alpha))
