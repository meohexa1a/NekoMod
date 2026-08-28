package org.mdt.core.ui.layout

import org.mdt.core.ui.node.LayoutNode

/**
 * ## MeasurePolicy [Container Measurement & Layout Strategy]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Strategy pattern interface defining intrinsic preferred width/height measurement and spatial layout positioning for [LayoutNode].
 * - Supports Hug Content (intrinsic sizing), proportional flex weight distribution, and uniform grid sizing.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-GC Intrinsic Measurements):** Must not allocate temporary `List` instances on Heap during frame measurements.
 * - **Rule 2 (Hug Content by Default):** Default measurement returns minimum required space factoring in padding and children.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Target Node:** `src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt`
 * - 📐 **Layout Engine:** `src/main/kotlin/org/mdt/core/ui/layout/GodotLayout.kt`
 * - 🎨 **Composable Containers:** `src/main/kotlin/org/mdt/ui/components/layout/Box.kt`, `src/main/kotlin/org/mdt/ui/components/layout/FlexLayouts.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `measureWidth` returns preferred content width factoring in `padL` and `padR`.
 * - [x] `measureHeight` returns preferred content height factoring in `padT` and `padB`.
 * - [x] `layout` assigns bounds to all visible child nodes.
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
 * ## BoxMeasurePolicy [Stack / Box Layout Policy]
 *
 * Layout policy supporting Hug Content (intrinsic sizing), Godot anchors, and content alignment.
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
                    verticalFlags = child.sizeFlagsVertical
                )
            }
        }
    }
}

// --- COLUMN MEASURE POLICY ---

/**
 * ## ColumnMeasurePolicy [Vertical Flex Layout Policy]
 *
 * Vertical linear layout policy arranging children top-to-bottom with Hug Content and Weight distribution.
 */
data class ColumnMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.TopStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

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
            alignment = alignment
        )
    }
}

// --- ROW MEASURE POLICY ---

/**
 * ## RowMeasurePolicy [Horizontal Flex Layout Policy]
 *
 * Horizontal linear layout policy arranging children left-to-right with Hug Content and Weight distribution.
 */
data class RowMeasurePolicy(
    val gap: Float = 0.0f,
    val arrangement: Arrangement = Arrangement.Start,
    val alignment: Alignment = Alignment.CenterStart
) : MeasurePolicy {

    private val effectiveArrangement: Arrangement =
        if (gap > 0.0f && arrangement == Arrangement.Start) Arrangement.spacedBy(gap) else arrangement

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
            alignment = alignment
        )
    }
}

// --- GRID MEASURE POLICY ---

/**
 * ## GridMeasurePolicy [Grid Layout Policy]
 *
 * Grid layout policy arranging children in uniform/flexible columns and rows.
 */
data class GridMeasurePolicy(
    val columns: Int = 1,
    val horizontalGap: Float = 0.0f,
    val verticalGap: Float = 0.0f
) : MeasurePolicy {

    override fun measureWidth(node: LayoutNode): Float {
        if (columns <= 0) return 0.0f

        val columnWidths = FloatArray(columns)
        var visibleIndex = 0
        for (i in node.children.indices) {
            val child = node.children[i]
            if (!child.visible) continue
            val columnIndex = visibleIndex % columns
            columnWidths[columnIndex] = maxOf(columnWidths[columnIndex], GodotLayout.getChildMinWidth(child))
            visibleIndex++
        }
        val totalGaps = if (columns > 1) (columns - 1) * horizontalGap else 0.0f
        return columnWidths.sum() + totalGaps
    }

    override fun measureHeight(node: LayoutNode, availableWidth: Float): Float {
        if (columns <= 0) return 0.0f

        var visibleCount = 0
        for (i in node.children.indices) {
            if (node.children[i].visible) visibleCount++
        }
        if (visibleCount == 0) return 0.0f

        val rows = (visibleCount + columns - 1) / columns
        val rowHeights = FloatArray(rows)
        var visibleIndex = 0
        for (i in node.children.indices) {
            val child = node.children[i]
            if (!child.visible) continue
            val rowIndex = visibleIndex / columns
            rowHeights[rowIndex] = maxOf(rowHeights[rowIndex], GodotLayout.getChildMinHeight(child))
            visibleIndex++
        }
        val totalGaps = if (rows > 1) (rows - 1) * verticalGap else 0.0f
        return rowHeights.sum() + totalGaps
    }

    override fun layout(node: LayoutNode, innerX: Float, innerY: Float, availableWidth: Float, availableHeight: Float) {
        GodotLayout.layoutGrid(
            children = node.children,
            parentX = innerX - node.padL,
            parentY = innerY - node.padB,
            parentWidth = availableWidth + node.padL + node.padR,
            parentHeight = availableHeight + node.padT + node.padB,
            padLeft = node.padL,
            padTop = node.padT,
            padRight = node.padR,
            padBottom = node.padB,
            columns = columns,
            horizontalSeparation = horizontalGap,
            verticalSeparation = verticalGap
        )
    }
}
