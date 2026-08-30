// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused")

package org.mdt.core.ui.compose

import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.HorizontalAlign
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.layout.VerticalAlign
import org.mdt.core.ui.node.UINode

// --- TYPED LAYOUT MODIFIER ELEMENTS ---

/**
 * ## PaddingModifier
 *
 * Applies inward padding insets onto a [UINode].
 *
 * @property left Left inward padding in pixels.
 * @property top Top inward padding in pixels.
 * @property right Right inward padding in pixels.
 * @property bottom Bottom inward padding in pixels.
 */
data class PaddingModifier(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.pad(left, top, right, bottom)
}

/**
 * ## MarginModifier
 *
 * Applies outward margin insets around a [UINode].
 *
 * @property left Left outward margin in pixels.
 * @property top Top outward margin in pixels.
 * @property right Right outward margin in pixels.
 * @property bottom Bottom outward margin in pixels.
 */
data class MarginModifier(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.margin(left, top, right, bottom)
}

/**
 * ## SizeModifier
 *
 * Sets explicit dimensions on a [UINode].
 *
 * @property width Explicit width in pixels, or `-1.0f` to leave unconstrained.
 * @property height Explicit height in pixels, or `-1.0f` to leave unconstrained.
 */
data class SizeModifier(
    val width: Float = -1.0f,
    val height: Float = -1.0f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (width >= 0.0f) node.width = width
        if (height >= 0.0f) node.height = height
    }
}

/**
 * ## MinSizeModifier
 *
 * Sets minimum protective bounds on a [UINode].
 *
 * @property minWidth Minimum width bound in pixels, or `-1.0f` to leave unconstrained.
 * @property minHeight Minimum height bound in pixels, or `-1.0f` to leave unconstrained.
 */
data class MinSizeModifier(
    val minWidth: Float = -1.0f,
    val minHeight: Float = -1.0f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (minWidth >= 0.0f) node.minWidth = minWidth
        if (minHeight >= 0.0f) node.minHeight = minHeight
    }
}

/**
 * ## MaxSizeModifier
 *
 * Sets maximum upper bounds on a [UINode].
 *
 * @property maxWidth Maximum width bound in pixels, or `-1.0f` to leave unconstrained.
 * @property maxHeight Maximum height bound in pixels, or `-1.0f` to leave unconstrained.
 */
data class MaxSizeModifier(
    val maxWidth: Float = -1.0f,
    val maxHeight: Float = -1.0f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (maxWidth >= 0.0f) node.maxWidth = maxWidth
        if (maxHeight >= 0.0f) node.maxHeight = maxHeight
    }
}

/**
 * ## FillModifier
 *
 * Configures container slot fill expansion flags on a [UINode].
 *
 * @property horizontal Whether to fill available width in the allocated slot.
 * @property vertical Whether to fill available height in the allocated slot.
 */
data class FillModifier(
    val horizontal: Boolean = true,
    val vertical: Boolean = true
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (horizontal) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.FILL
        if (vertical) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.FILL
    }
}

/**
 * ## ExpandModifier
 *
 * Configures free-space expansion ratio on a [UINode].
 *
 * @property horizontal Whether to expand horizontally into unallocated container space.
 * @property vertical Whether to expand vertically into unallocated container space.
 * @property ratio Relative expansion weight proportion.
 */
data class ExpandModifier(
    val horizontal: Boolean = true,
    val vertical: Boolean = true,
    val ratio: Float = 1.0f
) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        if (horizontal) node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND
        if (vertical) node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND
        node.stretchRatio = ratio
    }
}

/**
 * ## WeightModifier
 *
 * Configures proportional flex expansion weight on a [UINode].
 *
 * @property ratio Flex weight ratio relative to sibling nodes.
 */
data class WeightModifier(val ratio: Float) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        node.sizeFlagsHorizontal = node.sizeFlagsHorizontal or SizeFlags.EXPAND_FILL
        node.sizeFlagsVertical = node.sizeFlagsVertical or SizeFlags.EXPAND_FILL
        node.stretchRatio = ratio
    }
}

/**
 * ## BoxAlignModifier
 *
 * Configures 2D spatial alignment within a Box container on a [UINode].
 *
 * @property alignment 2D horizontal and vertical alignment preset.
 */
data class BoxAlignModifier(val alignment: Alignment) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment.horizontal) {
            HorizontalAlign.START -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> if ((node.sizeFlagsHorizontal and SizeFlags.FILL) == 0) node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
        when (alignment.vertical) {
            VerticalAlign.TOP -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> if ((node.sizeFlagsVertical and SizeFlags.FILL) == 0) node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

/**
 * ## RowAlignModifier
 *
 * Configures cross-axis vertical alignment within a Row container on a [UINode].
 *
 * @property alignment Cross-axis vertical alignment placement.
 */
data class RowAlignModifier(val alignment: VerticalAlign) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment) {
            VerticalAlign.TOP -> node.sizeFlagsVertical = SizeFlags.SHRINK_BEGIN
            VerticalAlign.CENTER -> node.sizeFlagsVertical = SizeFlags.SHRINK_CENTER
            VerticalAlign.BOTTOM -> node.sizeFlagsVertical = SizeFlags.SHRINK_END
            VerticalAlign.FILL -> node.sizeFlagsVertical = SizeFlags.FILL
        }
    }
}

/**
 * ## ColumnAlignModifier
 *
 * Configures cross-axis horizontal alignment within a Column container on a [UINode].
 *
 * @property alignment Cross-axis horizontal alignment placement.
 */
data class ColumnAlignModifier(val alignment: HorizontalAlign) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        when (alignment) {
            HorizontalAlign.START -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_BEGIN
            HorizontalAlign.CENTER -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_CENTER
            HorizontalAlign.END -> node.sizeFlagsHorizontal = SizeFlags.SHRINK_END
            HorizontalAlign.FILL -> node.sizeFlagsHorizontal = SizeFlags.FILL
        }
    }
}

/**
 * ## AnchorPresetModifier
 *
 * Applies a 2D responsive anchor preset on a [UINode].
 *
 * @property preset The 2D anchor layout preset ([LayoutPreset.CENTER], [LayoutPreset.FULL_RECT], etc.).
 */
data class AnchorPresetModifier(val preset: LayoutPreset) : UIModifier.Element {
    override fun applyTo(node: UINode) = node.anchorData.setPreset(preset)
}

// --- FLUENT EXTENSION FUNCTIONS ---

fun UIModifier.pad(all: Float): UIModifier = then(PaddingModifier(all, all, all, all))
fun UIModifier.pad(all: Int): UIModifier = pad(all.toFloat())
fun UIModifier.pad(horizontal: Float = 0.0f, vertical: Float = 0.0f): UIModifier =
    then(PaddingModifier(horizontal, vertical, horizontal, vertical))
fun UIModifier.pad(horizontal: Int, vertical: Int): UIModifier =
    pad(horizontal.toFloat(), vertical.toFloat())
fun UIModifier.pad(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f): UIModifier =
    then(PaddingModifier(left, top, right, bottom))

fun UIModifier.padding(all: Float): UIModifier = pad(all)
fun UIModifier.padding(all: Int): UIModifier = pad(all)
fun UIModifier.padding(horizontal: Float = 0.0f, vertical: Float = 0.0f): UIModifier = pad(horizontal, vertical)
fun UIModifier.padding(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f): UIModifier = pad(left, top, right, bottom)

fun UIModifier.margin(all: Float): UIModifier = then(MarginModifier(all, all, all, all))
fun UIModifier.margin(all: Int): UIModifier = margin(all.toFloat())
fun UIModifier.margin(horizontal: Float = 0.0f, vertical: Float = 0.0f): UIModifier =
    then(MarginModifier(horizontal, vertical, horizontal, vertical))
fun UIModifier.margin(horizontal: Int, vertical: Int): UIModifier =
    margin(horizontal.toFloat(), vertical.toFloat())
fun UIModifier.margin(left: Float = 0.0f, top: Float = 0.0f, right: Float = 0.0f, bottom: Float = 0.0f): UIModifier =
    then(MarginModifier(left, top, right, bottom))

fun UIModifier.size(all: Float): UIModifier = then(SizeModifier(all, all))
fun UIModifier.size(all: Int): UIModifier = size(all.toFloat())
fun UIModifier.size(width: Float, height: Float): UIModifier = then(SizeModifier(width, height))
fun UIModifier.size(width: Int, height: Int): UIModifier = size(width.toFloat(), height.toFloat())
fun UIModifier.width(width: Float): UIModifier = then(SizeModifier(width = width, height = -1.0f))
fun UIModifier.width(width: Int): UIModifier = width(width.toFloat())
fun UIModifier.height(height: Float): UIModifier = then(SizeModifier(width = -1.0f, height = height))
fun UIModifier.height(height: Int): UIModifier = height(height.toFloat())

fun UIModifier.minSize(minWidth: Float, minHeight: Float): UIModifier = then(MinSizeModifier(minWidth, minHeight))
fun UIModifier.minWidth(minWidth: Float): UIModifier = then(MinSizeModifier(minWidth = minWidth, minHeight = -1.0f))
fun UIModifier.minWidth(minWidth: Int): UIModifier = minWidth(minWidth.toFloat())
fun UIModifier.minHeight(minHeight: Float): UIModifier = then(MinSizeModifier(minWidth = -1.0f, minHeight = minHeight))
fun UIModifier.minHeight(minHeight: Int): UIModifier = minHeight(minHeight.toFloat())

fun UIModifier.maxSize(maxWidth: Float, maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth, maxHeight))
fun UIModifier.maxWidth(maxWidth: Float): UIModifier = then(MaxSizeModifier(maxWidth = maxWidth, maxHeight = -1.0f))
fun UIModifier.maxWidth(maxWidth: Int): UIModifier = maxWidth(maxWidth.toFloat())
fun UIModifier.maxHeight(maxHeight: Float): UIModifier = then(MaxSizeModifier(maxWidth = -1.0f, maxHeight = maxHeight))
fun UIModifier.maxHeight(maxHeight: Int): UIModifier = maxHeight(maxHeight.toFloat())

fun UIModifier.fill(horizontal: Boolean = true, vertical: Boolean = true): UIModifier = then(FillModifier(horizontal, vertical))
fun UIModifier.fillMaxWidth(): UIModifier = then(FillModifier(horizontal = true, vertical = false))
fun UIModifier.fillMaxHeight(): UIModifier = then(FillModifier(horizontal = false, vertical = true))
fun UIModifier.fillMaxSize(): UIModifier = then(FillModifier(horizontal = true, vertical = true))

fun UIModifier.expand(horizontal: Boolean = true, vertical: Boolean = true, ratio: Float = 1.0f): UIModifier =
    then(ExpandModifier(horizontal, vertical, ratio))

fun UIModifier.weight(ratio: Float): UIModifier = then(WeightModifier(ratio))
fun UIModifier.weight(ratio: Int): UIModifier = weight(ratio.toFloat())

fun UIModifier.anchor(preset: LayoutPreset): UIModifier = then(AnchorPresetModifier(preset))
fun UIModifier.align(alignment: Alignment): UIModifier = then(BoxAlignModifier(alignment))
fun UIModifier.align(alignment: VerticalAlign): UIModifier = then(RowAlignModifier(alignment))
fun UIModifier.align(alignment: HorizontalAlign): UIModifier = then(ColumnAlignModifier(alignment))
