package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RectangleShape
import org.hubdustry.core.graphics.RoundedCornerShape
import org.hubdustry.core.graphics.Shape
import org.hubdustry.core.layout.LayoutNode

data class ClipModifier(
    val shape: Shape
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clip = true
        when (shape) {
            is RoundedCornerShape -> {
                node.setCornerRadius(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
            }
            RectangleShape -> {
                node.setCornerRadius(0f)
            }
        }
    }
}

data object ClipToBoundsModifier : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clip = true
    }
}

data class CornerRadiusModifier(
    val topStart: Float,
    val topEnd: Float,
    val bottomEnd: Float,
    val bottomStart: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.setCornerRadius(topStart, topEnd, bottomEnd, bottomStart)
    }
}

/**
 * Cắt gọt (clip) container theo hình dạng [Shape] chỉ định.
 */
fun Modifier.clip(shape: Shape): Modifier = this.then(ClipModifier(shape))

/**
 * Cắt gọt (clip) toàn bộ nội dung con theo đúng khung viền chữ nhật của container này.
 */
fun Modifier.clipToBounds(): Modifier = this.then(ClipToBoundsModifier)

/**
 * Bo góc đồng đều cho container với bán kính [radius] mà không kích hoạt scissor clip.
 */
fun Modifier.cornerRadius(radius: Float): Modifier =
    this.then(CornerRadiusModifier(radius, radius, radius, radius))

/**
 * Bo góc độc lập 4 đỉnh cho container mà không kích hoạt scissor clip.
 */
fun Modifier.cornerRadius(
    topStart: Float = 0f,
    topEnd: Float = 0f,
    bottomEnd: Float = 0f,
    bottomStart: Float = 0f
): Modifier = this.then(CornerRadiusModifier(topStart, topEnd, bottomEnd, bottomStart))
