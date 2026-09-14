package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.LayoutNode

private data class MarginModifier(
    val start: Float,
    val top: Float,
    val end: Float,
    val bottom: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.marginLeft += start
        node.marginTop += top
        node.marginRight += end
        node.marginBottom += bottom
    }
}

/**
 * Thêm khoảng đệm ngoài (margin) [all] pixel cho cả 4 cạnh của widget đối với container cha.
 * Tự động tích lũy (cộng dồn) nếu chuỗi modifier có nhiều lệnh margin liên tiếp.
 */
fun Modifier.margin(all: Float): Modifier =
    this.then(MarginModifier(start = all, top = all, end = all, bottom = all))

fun Modifier.margin(all: Dp): Modifier = this.margin(all.toPx)

/**
 * Thêm khoảng đệm ngoài theo phương ngang [horizontal] và phương dọc [vertical].
 */
fun Modifier.margin(horizontal: Float = 0f, vertical: Float = 0f): Modifier =
    this.then(MarginModifier(start = horizontal, top = vertical, end = horizontal, bottom = vertical))

fun Modifier.margin(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier =
    this.margin(horizontal.toPx, vertical.toPx)

/**
 * Thêm khoảng đệm ngoài độc lập cho từng cạnh (chuẩn LTR: start = left, end = right).
 */
fun Modifier.margin(
    start: Float = 0f,
    top: Float = 0f,
    end: Float = 0f,
    bottom: Float = 0f
): Modifier = this.then(MarginModifier(start = start, top = top, end = end, bottom = bottom))

fun Modifier.margin(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
): Modifier = this.margin(start.toPx, top.toPx, end.toPx, bottom.toPx)
