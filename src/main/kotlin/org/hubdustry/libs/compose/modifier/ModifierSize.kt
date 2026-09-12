package org.hubdustry.libs.compose.modifier

import org.hubdustry.libs.compose.Modifier
import org.hubdustry.libs.layout.Alignment
import org.hubdustry.libs.layout.LayoutNode
import org.hubdustry.libs.layout.SizeFlag

private data class SizeModifier(
    val minWidth: Float,
    val minHeight: Float,
    val maxWidth: Float,
    val maxHeight: Float,
    val enforceShrink: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minWidth = minWidth
        node.maxWidth = maxWidth
        node.minHeight = minHeight
        node.maxHeight = maxHeight
        if (enforceShrink) {
            node.sizeFlagHorizontal = SizeFlag.SHRINK
            node.sizeFlagVertical = SizeFlag.SHRINK
        }
    }
}

private data class FillModifier(
    val fillHorizontal: Boolean,
    val fillVertical: Boolean
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        if (fillHorizontal) node.sizeFlagHorizontal = SizeFlag.FILL
        if (fillVertical) node.sizeFlagVertical = SizeFlag.FILL
    }
}

private data class WrapContentModifier(
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

/**
 * Đặt kích thước chính xác [width] và [height] cho widget.
 */
fun Modifier.size(width: Float, height: Float): Modifier =
    this.then(SizeModifier(minWidth = width, minHeight = height, maxWidth = width, maxHeight = height))

/**
 * Đặt kích thước vuông [size] x [size] cho widget.
 */
fun Modifier.size(size: Float): Modifier = this.size(size, size)

/**
 * Đặt chiều rộng chính xác [width] cho widget.
 */
fun Modifier.width(width: Float): Modifier =
    this.then(WidthModifier(minWidth = width, maxWidth = width))

/**
 * Đặt chiều cao chính xác [height] cho widget.
 */
fun Modifier.height(height: Float): Modifier =
    this.then(HeightModifier(minHeight = height, maxHeight = height))

/**
 * Kẹp biên kích thước trong khoảng cho phép.
 */
fun Modifier.sizeIn(
    minWidth: Float = 0f,
    minHeight: Float = 0f,
    maxWidth: Float = Float.MAX_VALUE,
    maxHeight: Float = Float.MAX_VALUE
): Modifier = this.then(SizeModifier(minWidth, minHeight, maxWidth, maxHeight, enforceShrink = false))

fun Modifier.widthIn(
    min: Float = 0f,
    max: Float = Float.MAX_VALUE
): Modifier = this.then(WidthModifier(minWidth = min, maxWidth = max))

fun Modifier.heightIn(
    min: Float = 0f,
    max: Float = Float.MAX_VALUE
): Modifier = this.then(HeightModifier(minHeight = min, maxHeight = max))

/**
 * Yêu cầu widget lấp đầy không gian khả dụng của container cha theo cả 2 chiều.
 */
fun Modifier.fillMaxSize(): Modifier = this.then(FillModifier(fillHorizontal = true, fillVertical = true))

/**
 * Yêu cầu widget lấp đầy chiều rộng của container cha.
 */
fun Modifier.fillMaxWidth(): Modifier = this.then(FillModifier(fillHorizontal = true, fillVertical = false))

/**
 * Yêu cầu widget lấp đầy chiều cao của container cha.
 */
fun Modifier.fillMaxHeight(): Modifier = this.then(FillModifier(fillHorizontal = false, fillVertical = true))

/**
 * Yêu cầu widget chỉ chiếm không gian vừa đủ theo kích thước nội tại tự thân (wrap content).
 */
fun Modifier.wrapContentSize(
    alignHorizontal: Alignment = Alignment.CENTER,
    alignVertical: Alignment = Alignment.CENTER
): Modifier = this.then(WrapContentModifier(wrapHorizontal = true, wrapVertical = true, alignHorizontal = alignHorizontal, alignVertical = alignVertical))

fun Modifier.wrapContentWidth(align: Alignment = Alignment.START): Modifier =
    this.then(WrapContentModifier(wrapHorizontal = true, wrapVertical = false, alignHorizontal = align, alignVertical = Alignment.START))

fun Modifier.wrapContentHeight(align: Alignment = Alignment.START): Modifier =
    this.then(WrapContentModifier(wrapHorizontal = false, wrapVertical = true, alignHorizontal = Alignment.START, alignVertical = align))

private data class WidthModifier(
    val minWidth: Float,
    val maxWidth: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minWidth = minWidth
        node.maxWidth = maxWidth
        node.sizeFlagHorizontal = SizeFlag.SHRINK
    }
}

private data class HeightModifier(
    val minHeight: Float,
    val maxHeight: Float
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.minHeight = minHeight
        node.maxHeight = maxHeight
        node.sizeFlagVertical = SizeFlag.SHRINK
    }
}
