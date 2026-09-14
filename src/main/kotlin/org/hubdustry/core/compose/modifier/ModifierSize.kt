package org.hubdustry.core.compose.modifier

import org.hubdustry.core.compose.unit.Dp
import org.hubdustry.core.compose.unit.dp
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag

// ─── Public Dimension Modifiers ───────────────────────────────────

/**
 * Đặt kích thước chính xác [width] và [height] cho widget.
 */
fun Modifier.size(width: Float, height: Float): Modifier =
    this.then(
        SizeModifier(
            minWidth = width,
            minHeight = height,
            maxWidth = width,
            maxHeight = height
        )
    )

/**
 * Đặt kích thước vuông [size] x [size] cho widget.
 */
fun Modifier.size(size: Float): Modifier = this.size(size, size)

fun Modifier.size(width: Dp, height: Dp): Modifier = this.size(width.toPx, height.toPx)

fun Modifier.size(size: Dp): Modifier = this.size(size.toPx, size.toPx)

/**
 * Đặt chiều rộng chính xác [width] cho widget.
 */
fun Modifier.width(width: Float): Modifier =
    this.then(WidthModifier(minWidth = width, maxWidth = width))

fun Modifier.width(width: Dp): Modifier = this.width(width.toPx)

/**
 * Đặt chiều cao chính xác [height] cho widget.
 */
fun Modifier.height(height: Float): Modifier =
    this.then(HeightModifier(minHeight = height, maxHeight = height))

fun Modifier.height(height: Dp): Modifier = this.height(height.toPx)

/**
 * Kẹp biên kích thước trong khoảng cho phép.
 */
fun Modifier.sizeIn(
    minWidth: Float = 0f,
    minHeight: Float = 0f,
    maxWidth: Float = Float.MAX_VALUE,
    maxHeight: Float = Float.MAX_VALUE
): Modifier = this.then(
    SizeModifier(
        minWidth = minWidth,
        minHeight = minHeight,
        maxWidth = maxWidth,
        maxHeight = maxHeight
    )
)

fun Modifier.sizeIn(
    minWidth: Dp = 0.dp,
    minHeight: Dp = 0.dp,
    maxWidth: Dp = Dp.Infinity,
    maxHeight: Dp = Dp.Infinity
): Modifier = this.sizeIn(
    minWidth = minWidth.toPx,
    minHeight = minHeight.toPx,
    maxWidth = maxWidth.toPx,
    maxHeight = maxHeight.toPx
)

fun Modifier.widthIn(
    min: Float = 0f,
    max: Float = Float.MAX_VALUE
): Modifier = this.then(WidthModifier(minWidth = min, maxWidth = max))

fun Modifier.widthIn(
    min: Dp = 0.dp,
    max: Dp = Dp.Infinity
): Modifier = this.widthIn(min = min.toPx, max = max.toPx)

fun Modifier.heightIn(
    min: Float = 0f,
    max: Float = Float.MAX_VALUE
): Modifier = this.then(HeightModifier(minHeight = min, maxHeight = max))

fun Modifier.heightIn(
    min: Dp = 0.dp,
    max: Dp = Dp.Infinity
): Modifier = this.heightIn(min = min.toPx, max = max.toPx)

// ─── Public Fill & Wrap Modifiers ──────────────────────────────────

/**
 * Yêu cầu widget lấp đầy không gian khả dụng của container cha theo cả 2 chiều.
 */
fun Modifier.fillMaxSize(): Modifier =
    this.then(FillModifier(fillHorizontal = true, fillVertical = true))

/**
 * Yêu cầu widget lấp đầy chiều rộng của container cha.
 */
fun Modifier.fillMaxWidth(): Modifier =
    this.then(FillModifier(fillHorizontal = true, fillVertical = false))

/**
 * Yêu cầu widget lấp đầy chiều cao của container cha.
 */
fun Modifier.fillMaxHeight(): Modifier =
    this.then(FillModifier(fillHorizontal = false, fillVertical = true))

/**
 * Yêu cầu widget chỉ chiếm không gian vừa đủ theo kích thước nội tại tự thân (wrap content).
 */
fun Modifier.wrapContentSize(
    alignHorizontal: Alignment = Alignment.CENTER,
    alignVertical: Alignment = Alignment.CENTER
): Modifier = this.then(
    WrapContentModifier(
        wrapHorizontal = true,
        wrapVertical = true,
        alignHorizontal = alignHorizontal,
        alignVertical = alignVertical
    )
)

fun Modifier.wrapContentWidth(align: Alignment = Alignment.START): Modifier =
    this.then(
        WrapContentModifier(
            wrapHorizontal = true,
            wrapVertical = false,
            alignHorizontal = align,
            alignVertical = Alignment.START
        )
    )

fun Modifier.wrapContentHeight(align: Alignment = Alignment.START): Modifier =
    this.then(
        WrapContentModifier(
            wrapHorizontal = false,
            wrapVertical = true,
            alignHorizontal = Alignment.START,
            alignVertical = align
        )
    )

// ─── Internal Modifier Elements ────────────────────────────────────

internal data class SizeModifier(
    val minWidth: Float,
    val minHeight: Float,
    val maxWidth: Float,
    val maxHeight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minWidth = minWidth
        node.maxWidth = maxWidth
        node.minHeight = minHeight
        node.maxHeight = maxHeight
    }
}

internal data class WidthModifier(
    val minWidth: Float,
    val maxWidth: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minWidth = minWidth
        node.maxWidth = maxWidth
    }
}

internal data class HeightModifier(
    val minHeight: Float,
    val maxHeight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minHeight = minHeight
        node.maxHeight = maxHeight
    }
}

internal data class FillModifier(
    val fillHorizontal: Boolean,
    val fillVertical: Boolean
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        if (fillHorizontal) {
            node.sizeFlagHorizontal = SizeFlag.FILL
            if (node.stretchRatio <= 0f) node.stretchRatio = 1f
        }
        if (fillVertical) {
            node.sizeFlagVertical = SizeFlag.FILL
            if (node.stretchRatio <= 0f) node.stretchRatio = 1f
        }
    }
}

internal data class WrapContentModifier(
    val wrapHorizontal: Boolean,
    val wrapVertical: Boolean,
    val alignHorizontal: Alignment,
    val alignVertical: Alignment
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        if (wrapHorizontal) {
            node.sizeFlagHorizontal = SizeFlag.SHRINK
            node.alignHorizontal = alignHorizontal
        }
        if (wrapVertical) {
            node.sizeFlagVertical = SizeFlag.SHRINK
            node.alignVertical = alignVertical
        }
    }
}
