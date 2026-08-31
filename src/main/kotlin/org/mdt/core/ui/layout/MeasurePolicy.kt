// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Container Measure & Layout Policies ([Row], [Column], [Box], [FlowRow], [Grid]).
// - Operating Mechanism: Computes intrinsic min/pref dimensions and places children using reusable Zero-GC [PrimitiveBuffers].
// - Invariants: 100% Pure Kotlin; proportional flex weight redistribution; Zero-GC hot loops.
// - Dependencies: [PrimitiveBuffers], [GodotLayout], [LayoutNode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

import org.mdt.core.ui.node.LayoutNode
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.Arrangement
import org.mdt.core.ui.unit.ArrangementType
import org.mdt.core.ui.unit.VerticalAlign

/**
 * ## MeasurePolicy
 *
 * Defines measurement and layout strategies for container nodes ([LayoutNode]).
 * Calculates preferred dimensions (hug content) and arranges child nodes within container bounds.
 *
 * @see BoxMeasurePolicy
 * @see RowMeasurePolicy
 * @see ColumnMeasurePolicy
 * @see FlowRowMeasurePolicy
 * @see LayoutNode
 */
interface MeasurePolicy {

    /** Computes preferred width of the [node] factoring in inward padding and children constraints. */
    fun measureWidth(node: LayoutNode): Float

    /** Computes preferred height of the [node] factoring in inward padding, children constraints, and optional [availableWidth]. */
    fun measureHeight(node: LayoutNode, availableWidth: Float = -1.0f): Float

    /** Computes and applies spatial layouts onto [node] children inside the available inner box. */
    fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float)
}

// --- BOX MEASURE POLICY ---

/**
 * ## BoxMeasurePolicy
 *
 * Layout policy supporting Hug Content (intrinsic sizing), Godot anchors, and content alignment.
 *
 * @see MeasurePolicy
 * @see LayoutNode
 */
object BoxMeasurePolicy : MeasurePolicy {

    override fun measureWidth(node: LayoutNode): Float {
        var maxChildWidth = 0.0f
        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                maxChildWidth = maxOf(maxChildWidth, GodotLayout.getChildMinWidth(child))
            }
        }
        return if (node.minWidth >= 0.0f) maxOf(maxChildWidth, node.minWidth) else maxChildWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        var maxChildHeight = 0.0f
        val availableInnerWidth = when {
            availableWidth >= 0.0f -> availableWidth
            node.width >= 0.0f -> maxOf(0.0f, node.width - node.padL - node.padR)
            else -> -1.0f
        }

        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                maxChildHeight = maxOf(maxChildHeight, GodotLayout.getChildMinHeight(child, availableInnerWidth))
            }
        }
        return if (node.minHeight >= 0.0f) maxOf(maxChildHeight, node.minHeight) else maxChildHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        for (i in 0 until node.children.size) {
            val child = node.children[i]
            if (!child.visible) continue

            if (child.anchorData.isEnabled) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availableWidth, availableHeight)
            } else {
                GodotLayout.fitChildInRect(
                    child = child,
                    rectX = innerX,
                    rectY = innerY,
                    rectWidth = availableWidth,
                    rectHeight = availableHeight,
                    horizontalFlags = child.sizeFlagsHorizontal,
                    verticalFlags = child.sizeFlagsVertical,
                )
            }
        }
    }
}

// --- COLUMN MEASURE POLICY ---

/**
 * ## ColumnMeasurePolicy
 *
 * Vertical linear layout policy arranging children top-to-bottom with Hug Content and proportional Weight distribution.
 *
 * @property gap Inward spacing between adjacent children (pixels).
 * @property arrangement Main-axis distribution strategy ([Arrangement.Start], [Arrangement.Center], etc.).
 * @property alignment Cross-axis alignment placement ([Alignment.TopStart], [Alignment.CenterStart], etc.).
 *
 * @see RowMeasurePolicy
 * @see MeasurePolicy
 */
data class ColumnMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.TopStart,
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement.spacing == 0.0f) Arrangement(arrangement.type, gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        var maxWidth = 0.0f
        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                maxWidth = maxOf(maxWidth, GodotLayout.getChildMinWidth(child))
            }
        }
        return maxWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        var sumHeight = 0.0f
        var visibleCount = 0
        val availableInnerWidth = when {
            availableWidth >= 0.0f -> availableWidth
            node.width >= 0.0f -> maxOf(0.0f, node.width - node.padL - node.padR)
            else -> -1.0f
        }

        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                sumHeight += GodotLayout.getChildMinHeight(child, availableInnerWidth)
                visibleCount++
            }
        }
        if (visibleCount > 1) sumHeight += (visibleCount - 1) * effectiveArrangement.spacing
        return sumHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
            isVertical = true,
            arrangement = effectiveArrangement,
            alignment = alignment,
        )
    }
}

// --- ROW MEASURE POLICY ---

/**
 * ## RowMeasurePolicy
 *
 * Horizontal linear layout policy arranging children left-to-right with Hug Content and proportional Weight distribution.
 *
 * @property gap Inward spacing between adjacent children (pixels).
 * @property arrangement Main-axis distribution strategy ([Arrangement.Start], [Arrangement.Center], etc.).
 * @property alignment Cross-axis alignment placement ([Alignment.CenterStart], [Alignment.TopStart], etc.).
 *
 * @see ColumnMeasurePolicy
 * @see MeasurePolicy
 */
data class RowMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.CenterStart,
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement.spacing == 0.0f) Arrangement(arrangement.type, gap) else arrangement

    override fun measureWidth(node: LayoutNode): Float {
        var sumWidth = 0.0f
        var visibleCount = 0
        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                sumWidth += GodotLayout.getChildMinWidth(child)
                visibleCount++
            }
        }
        if (visibleCount > 1) sumWidth += (visibleCount - 1) * effectiveArrangement.spacing
        return sumWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        var maxHeight = 0.0f
        for (i in node.children.indices) {
            val child = node.children[i]
            if (child.visible) {
                maxHeight = maxOf(maxHeight, GodotLayout.getChildMinHeight(child))
            }
        }
        return maxHeight
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutBox(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
            isVertical = false,
            arrangement = effectiveArrangement,
            alignment = alignment,
        )
    }
}

// --- FLOW ROW MEASURE POLICY (FLEX WRAP) ---

/**
 * ## FlowRowMeasurePolicy
 *
 * Arranges children horizontally from left to right, automatically wrapping onto the next line
 * when available width is exceeded. Implements Godot Engine's `HFlowContainer` 2-pass algorithm.
 *
 * @property horizontalGap Horizontal spacing between adjacent items on the same line (pixels).
 * @property verticalGap Vertical spacing between wrapped lines (pixels).
 * @property arrangement Main-axis line distribution strategy.
 * @property alignment Cross-axis item alignment within each line.
 *
 * @see RowMeasurePolicy
 * @see MeasurePolicy
 */
data class FlowRowMeasurePolicy(
    val horizontalGap: Float = 0.0f,
    val verticalGap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.TopStart,
) : MeasurePolicy {

    override fun measureWidth(node: LayoutNode): Float {
        var maxLineWidth = 0.0f
        var currentLineX = 0.0f
        var childrenInLine = 0

        for (i in node.children.indices) {
            val child = node.children[i]
            if (!child.visible) continue

            val childWidth = GodotLayout.getChildMinWidth(child)
            val neededWidth = if (childrenInLine > 0) childWidth + horizontalGap else childWidth

            currentLineX += neededWidth
            childrenInLine++
            maxLineWidth = maxOf(maxLineWidth, currentLineX)
        }

        return maxLineWidth
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        val maxContainerWidth = if (availableWidth > 0.0f) availableWidth else Float.MAX_VALUE
        var totalHeight = 0.0f
        var currentLineHeight = 0.0f
        var currentLineX = 0.0f
        var childrenInLine = 0
        var lineCount = 0

        for (i in node.children.indices) {
            val child = node.children[i]
            if (!child.visible) continue

            val childWidth = GodotLayout.getChildMinWidth(child)
            val childHeight = GodotLayout.getChildMinHeight(child)

            val neededWidth = if (childrenInLine > 0) childWidth + horizontalGap else childWidth
            if (childrenInLine > 0 && currentLineX + neededWidth > maxContainerWidth) {
                // Wrap to next line
                totalHeight += currentLineHeight
                lineCount++
                currentLineX = childWidth
                currentLineHeight = childHeight
                childrenInLine = 1
            } else {
                currentLineX += neededWidth
                currentLineHeight = maxOf(currentLineHeight, childHeight)
                childrenInLine++
            }
        }

        if (childrenInLine > 0) {
            totalHeight += currentLineHeight
            lineCount++
        }

        val totalGaps = if (lineCount > 1) (lineCount - 1) * verticalGap else 0.0f
        return totalHeight + totalGaps
    }

    companion object {
        private val scratchLineChildStartIndex = IntList(32)
        private val scratchLineChildCounts = IntList(32)
        private val scratchLineHeights = FloatList(32)
        private val scratchLineTotalWidths = FloatList(32)
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        val maxContainerWidth = if (availableWidth > 0.0f) availableWidth else Float.MAX_VALUE

        val lineChildStartIndex = scratchLineChildStartIndex.apply { clear() }
        val lineChildCounts = scratchLineChildCounts.apply { clear() }
        val lineHeights = scratchLineHeights.apply { clear() }
        val lineTotalWidths = scratchLineTotalWidths.apply { clear() }

        var currentLineStart = 0
        var currentChildrenInLine = 0
        var currentLineX = 0.0f
        var currentLineHeight = 0.0f

        for (i in node.children.indices) {
            val child = node.children[i]
            if (!child.visible) continue

            val childWidth = GodotLayout.getChildMinWidth(child)
            val childHeight = GodotLayout.getChildMinHeight(child)

            val neededWidth = if (currentChildrenInLine > 0) childWidth + horizontalGap else childWidth
            if (currentChildrenInLine > 0 && currentLineX + neededWidth > maxContainerWidth) {
                // End current line
                lineChildStartIndex.add(currentLineStart)
                lineChildCounts.add(currentChildrenInLine)
                lineHeights.add(currentLineHeight)
                lineTotalWidths.add(currentLineX)

                // Start new line
                currentLineStart = i
                currentChildrenInLine = 1
                currentLineX = childWidth
                currentLineHeight = childHeight
            } else {
                if (currentChildrenInLine == 0) currentLineStart = i
                currentLineX += neededWidth
                currentLineHeight = maxOf(currentLineHeight, childHeight)
                currentChildrenInLine++
            }
        }

        if (currentChildrenInLine > 0) {
            lineChildStartIndex.add(currentLineStart)
            lineChildCounts.add(currentChildrenInLine)
            lineHeights.add(currentLineHeight)
            lineTotalWidths.add(currentLineX)
        }

        if (lineChildCounts.isEmpty) return

        // Pass 2: Layout each line from Top to Bottom (in OpenGL Bottom-Left Coordinates)
        var currentTopY = innerY + availableHeight
        val totalLines = lineChildCounts.size
        for (lineIdx in 0 until totalLines) {
            val startChildIdx = lineChildStartIndex.get(lineIdx)
            val childCountInLine = lineChildCounts.get(lineIdx)
            val lineH = lineHeights.get(lineIdx)
            val lineW = lineTotalWidths.get(lineIdx)

            val lineSlotY = currentTopY - lineH

            // Calculate horizontal start offset
            val startOffsetX = when (arrangement.type) {
                ArrangementType.START -> 0.0f
                ArrangementType.CENTER -> maxOf(0.0f, (availableWidth - lineW) * 0.5f)
                ArrangementType.END -> maxOf(0.0f, availableWidth - lineW)
                else -> 0.0f
            }

            var currentItemX = innerX + startOffsetX
            var processed = 0
            var childIdx = startChildIdx
            while (processed < childCountInLine && childIdx < node.children.size) {
                val child = node.children[childIdx]
                if (child.visible) {
                    val childWidth = GodotLayout.getChildMinWidth(child)
                    val childHeight = GodotLayout.getChildMinHeight(child)

                    val childY = when (alignment.vertical) {
                        VerticalAlign.TOP -> lineSlotY + lineH - childHeight
                        VerticalAlign.CENTER -> lineSlotY + (lineH - childHeight) * 0.5f
                        VerticalAlign.BOTTOM -> lineSlotY
                        VerticalAlign.FILL -> lineSlotY
                    }

                    GodotLayout.fitChildInRect(
                        child,
                        currentItemX,
                        childY,
                        childWidth,
                        if (alignment.vertical == VerticalAlign.FILL) lineH else childHeight,
                    )
                    currentItemX += childWidth + horizontalGap
                    processed++
                }
                childIdx++
            }

            currentTopY -= lineH + verticalGap
        }
    }
}
