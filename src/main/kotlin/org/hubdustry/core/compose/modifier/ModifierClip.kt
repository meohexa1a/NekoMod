package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.LayoutNode

/**
 * Modifier cắt gọt (SDF Rounded Box Clip) nội dung con và bản thân container theo bán kính bo 4 góc [corners].
 * Hỗ trợ kiểm soát độc lập trục ngang [clipHorizontal] và trục dọc [clipVertical].
 */
data class ClipModifier(
    val corners: RoundedCorners,
    val clipHorizontal: Boolean = true,
    val clipVertical: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clipHorizontal = clipHorizontal
        node.clipVertical = clipVertical
        node.setClipCornerRadius(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)
        // Đồng bộ hình dạng visual tự thân nếu node chưa có bo góc riêng
        if (!node.hasRoundedCorners) {
            node.setCornerRadius(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)
        }
    }
}

/**
 * Modifier cắt gọt (Scissor AABB Clip) nội dung con theo đúng khung viền chữ nhật của container này.
 * Hỗ trợ kiểm soát độc lập trục ngang [clipHorizontal] và trục dọc [clipVertical].
 */
data class ClipToBoundsModifier(
    val clipHorizontal: Boolean = true,
    val clipVertical: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clipHorizontal = clipHorizontal
        node.clipVertical = clipVertical
    }
}

/**
 * Modifier định hình bo góc trực quan cho background và border của chính container (Visual Tokens).
 * Không kích hoạt cắt gọt nội dung con bên trong.
 */
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
 * Cắt gọt (clip) container và các con bên trong theo bán kính bo 4 góc [corners].
 * [clipHorizontal]: Có cắt gọt tràn viền theo trục ngang không (mặc định: true).
 * [clipVertical]: Có cắt gọt tràn viền theo trục dọc không (mặc định: true).
 */
fun Modifier.clip(
    corners: RoundedCorners,
    clipHorizontal: Boolean = true,
    clipVertical: Boolean = true
): Modifier = this.then(ClipModifier(corners, clipHorizontal, clipVertical))

/**
 * Cắt gọt (clip) container và các con bên trong theo bán kính bo đồng đều [radius].
 */
fun Modifier.clip(
    radius: Float,
    clipHorizontal: Boolean = true,
    clipVertical: Boolean = true
): Modifier = clip(RoundedCorners(radius), clipHorizontal, clipVertical)

/**
 * Cắt gọt (clip) toàn bộ nội dung con theo đúng khung viền chữ nhật của container này.
 * Cho phép chỉ cắt gọt trục ngang ([clipHorizontal]) hoặc trục dọc ([clipVertical]) độc lập.
 */
fun Modifier.clipToBounds(
    clipHorizontal: Boolean = true,
    clipVertical: Boolean = true
): Modifier = this.then(ClipToBoundsModifier(clipHorizontal, clipVertical))

/**
 * Bo góc đồng đều cho container với bán kính [radius] mà không kích hoạt cắt gọt con bên trong.
 */
fun Modifier.cornerRadius(radius: Float): Modifier =
    this.then(CornerRadiusModifier(radius, radius, radius, radius))

/**
 * Bo góc độc lập 4 đỉnh cho container mà không kích hoạt cắt gọt con bên trong.
 */
fun Modifier.cornerRadius(
    topStart: Float = 0f,
    topEnd: Float = 0f,
    bottomEnd: Float = 0f,
    bottomStart: Float = 0f
): Modifier = this.then(CornerRadiusModifier(topStart, topEnd, bottomEnd, bottomStart))
