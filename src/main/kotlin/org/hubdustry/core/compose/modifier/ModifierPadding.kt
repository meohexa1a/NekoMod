package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.LayoutNode

private data class PaddingModifier(
    val start: Float,
    val top: Float,
    val end: Float,
    val bottom: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.paddingLeft += start
        node.paddingTop += top
        node.paddingRight += end
        node.paddingBottom += bottom
    }
}

/**
 * Thêm vùng đệm [all] pixel cho cả 4 cạnh của widget.
 * Tự động tích lũy (cộng dồn) nếu chuỗi modifier có nhiều lệnh padding liên tiếp.
 */
fun Modifier.padding(all: Float): Modifier =
    this.then(PaddingModifier(start = all, top = all, end = all, bottom = all))

fun Modifier.padding(all: Dp): Modifier = this.padding(all.toPx)

/**
 * Thêm vùng đệm theo phương ngang [horizontal] và phương dọc [vertical].
 */
fun Modifier.padding(horizontal: Float = 0f, vertical: Float = 0f): Modifier =
    this.then(PaddingModifier(start = horizontal, top = vertical, end = horizontal, bottom = vertical))

fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier =
    this.padding(horizontal.toPx, vertical.toPx)

/**
 * Thêm vùng đệm độc lập cho từng cạnh (chuẩn LTR: start = left, end = right).
 */
fun Modifier.padding(
    start: Float = 0f,
    top: Float = 0f,
    end: Float = 0f,
    bottom: Float = 0f
): Modifier = this.then(PaddingModifier(start = start, top = top, end = end, bottom = bottom))

fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
): Modifier = this.padding(start.toPx, top.toPx, end.toPx, bottom.toPx)
