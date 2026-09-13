package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.LayoutNode

data class ClipModifier(
    val corners: RoundedCorners
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clip = true
        node.setCornerRadius(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)
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
 * Cắt gọt (clip) container theo bán kính bo 4 góc [corners] chỉ định.
 */
fun Modifier.clip(corners: RoundedCorners): Modifier = this.then(ClipModifier(corners))

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
