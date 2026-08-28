package org.mdt.core.ui.layout

import org.mdt.core.ui.node.TextNode
import org.mdt.core.ui.node.UINode

/**
 * ## GodotLayout [2-Pass Container Layout Engine]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - 2-Pass responsive container layout engine inspired by Godot Engine's UI architecture.
 * - Computes Box Model fitting, proportional flex weights (Row / Column), uniform/flexible Grid distributions, and dual-coordinate Anchors.
 * - Respects intrinsic minimum bounds, inward padding, outward margins, and size flags ([SizeFlags.FILL], [SizeFlags.EXPAND], [SizeFlags.SHRINK_CENTER], etc.).
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (OpenGL Bottom-Left Coordinates):** All layout math operates in bottom-left origin ($y=0$ bottom, $y=\text{parentH}$ top).
 * - **Rule 2 (Zero-GC Traversals):** Direct indexed `for` loops MUST be used instead of `.filter {}` to prevent Heap allocations per frame.
 * - **Rule 3 (Hug Content by Default):** Unconstrained children hug content intrinsic size unless `FILL` or fixed dimension is specified.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 📐 **Measure Policy:** `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 * - 🌲 **Primitive Node:** `src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt`
 * - 🌲 **Base Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 * - 🌲 **Root Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `fitChildInRect` correctly offsets by `marginL` and `marginB` and clamps slot dimensions.
 * - [x] `layoutBox` distributes excess main-axis space proportionally according to `stretchRatio` when `EXPAND` is active.
 * - [x] `layoutSingleAnchor` applies anchor ratio and offset bounds without subtracting height from top-anchored nodes.
 */
object GodotLayout {

    // --- INTRINSIC MEASUREMENT HELPERS ---

    fun getChildMinWidth(child: UINode): Float {
        val preferred = child.getPrefWidth()
        val baseWidth = if (preferred > 0.0f) preferred else if (child.minWidth > 0.0f) child.minWidth else 0.0f
        return baseWidth + child.marginL + child.marginR
    }

    fun getChildMinHeight(child: UINode, availableWidth: Float = -1.0f): Float {
        val preferred = child.getPrefHeight(availableWidth)
        val baseHeight = if (preferred > 0.0f) preferred else if (child.minHeight > 0.0f) child.minHeight else 0.0f
        return baseHeight + child.marginT + child.marginB
    }

    // --- SLOT RECTANGLE FITTING ---

    fun fitChildInRect(
        child: UINode,
        rectX: Float,
        rectY: Float,
        rectWidth: Float,
        rectHeight: Float,
        horizontalFlags: Int = child.sizeFlagsHorizontal,
        verticalFlags: Int = child.sizeFlagsVertical
    ) {
        val slotInnerX = rectX + child.marginL
        val slotInnerY = rectY + child.marginB
        val slotInnerWidth = maxOf(0.0f, rectWidth - child.marginL - child.marginR)
        val slotInnerHeight = maxOf(0.0f, rectHeight - child.marginT - child.marginB)

        val childPureMinWidth = run {
            val preferred = child.getPrefWidth()
            if (preferred > 0.0f) preferred else if (child.minWidth > 0.0f) child.minWidth else 0.0f
        }
        val childPureMinHeight = run {
            val preferred = child.getPrefHeight()
            if (preferred > 0.0f) preferred else if (child.minHeight > 0.0f) child.minHeight else 0.0f
        }

        // 1. Horizontal Dimension & Position
        val computedWidth: Float = when {
            child.width > 0.0f -> child.width
            (horizontalFlags and SizeFlags.FILL) != 0 -> slotInnerWidth
            child is TextNode && child.wrap -> slotInnerWidth
            else -> minOf(slotInnerWidth, childPureMinWidth)
        }

        val computedX: Float = when {
            (horizontalFlags and SizeFlags.FILL) != 0 && child.width <= 0.0f -> slotInnerX
            (horizontalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerWidth - computedWidth
            (horizontalFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerX + (slotInnerWidth - computedWidth) * 0.5f
            else -> slotInnerX // Default / SHRINK_BEGIN: Left-aligned
        }

        // 2. Vertical Dimension & Position (OpenGL Bottom-Left)
        val computedHeight: Float = when {
            child.height > 0.0f -> child.height
            (verticalFlags and SizeFlags.FILL) != 0 -> slotInnerHeight
            else -> minOf(slotInnerHeight, childPureMinHeight)
        }

        val computedY: Float = when {
            (verticalFlags and SizeFlags.FILL) != 0 && child.height <= 0.0f -> slotInnerY
            (verticalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerY
            (verticalFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerY + (slotInnerHeight - computedHeight) * 0.5f
            else -> slotInnerY + slotInnerHeight - computedHeight // Default / SHRINK_BEGIN: Top of slot
        }

        child.setBounds(computedX, computedY, computedWidth, computedHeight)
    }

    // --- FLEX & BOX LAYOUT (ROW / COLUMN) ---

    fun layoutBox(
        children: List<UINode>,
        parentX: Float,
        parentY: Float,
        parentWidth: Float,
        parentHeight: Float,
        padLeft: Float,
        padTop: Float,
        padRight: Float,
        padBottom: Float,
        isVertical: Boolean,
        arrangement: Arrangement = Arrangement.Start,
        alignment: Alignment = if (isVertical) Alignment.TopStart else Alignment.CenterStart
    ) {
        val availableWidth = maxOf(0.0f, parentWidth - padLeft - padRight)
        val availableHeight = maxOf(0.0f, parentHeight - padTop - padBottom)

        var visibleCount = 0
        for (i in children.indices) {
            if (children[i].visible) visibleCount++
        }
        if (visibleCount == 0) return

        var unweightedMinSize = 0.0f
        var totalMinMain = 0.0f
        var totalStretchRatio = 0.0f

        for (i in children.indices) {
            val child = children[i]
            if (!child.visible) continue
            val minSize = if (isVertical) getChildMinHeight(child) else getChildMinWidth(child)
            val flags = if (isVertical) child.sizeFlagsVertical else child.sizeFlagsHorizontal
            val ratio = child.stretchRatio

            totalMinMain += minSize
            if ((flags and SizeFlags.EXPAND) != 0) {
                totalStretchRatio += ratio
            } else {
                unweightedMinSize += minSize
            }
        }

        val availableMain = if (isVertical) availableHeight else availableWidth
        val fixedGap = arrangement.spacing

        var actualGap = fixedGap
        var startOffset = 0.0f
        var spaceForExpanding = 0.0f

        if (totalStretchRatio > 0.0f) {
            val totalGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0.0f
            spaceForExpanding = maxOf(0.0f, availableMain - unweightedMinSize - totalGaps)
        } else {
            val totalFixedGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0.0f
            val remainingSpace = maxOf(0.0f, availableMain - totalMinMain)

            when (arrangement.type) {
                ArrangementType.START -> {
                    startOffset = 0.0f
                    actualGap = fixedGap
                }
                ArrangementType.CENTER -> {
                    startOffset = maxOf(0.0f, (availableMain - totalMinMain - totalFixedGaps) * 0.5f)
                    actualGap = fixedGap
                }
                ArrangementType.END -> {
                    startOffset = maxOf(0.0f, availableMain - totalMinMain - totalFixedGaps)
                    actualGap = fixedGap
                }
                ArrangementType.SPACE_BETWEEN -> {
                    startOffset = 0.0f
                    actualGap = if (visibleCount > 1) remainingSpace / (visibleCount - 1) else 0.0f
                }
                ArrangementType.SPACE_AROUND -> {
                    actualGap = if (visibleCount > 0) remainingSpace / visibleCount else 0.0f
                    startOffset = actualGap * 0.5f
                }
                ArrangementType.SPACE_EVENLY -> {
                    actualGap = if (visibleCount > 0) remainingSpace / (visibleCount + 1) else 0.0f
                    startOffset = actualGap
                }
            }
        }

        if (isVertical) {
            var currentTopY = parentY + parentHeight - padTop - startOffset
            for (i in children.indices) {
                val child = children[i]
                if (!child.visible) continue
                val childAvailableWidth = when {
                    (child.sizeFlagsHorizontal and SizeFlags.FILL) != 0 -> availableWidth
                    child.width > 0.0f -> child.width
                    else -> availableWidth
                }
                val slotTotalHeight = getChildMinHeight(child, childAvailableWidth)
                val verticalFlags = child.sizeFlagsVertical
                val ratio = child.stretchRatio

                val slotHeight = if ((verticalFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0.0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalHeight
                }

                val horizontalFlags = if ((child.sizeFlagsHorizontal and SizeFlags.FILL) != 0) {
                    child.sizeFlagsHorizontal
                } else {
                    when (alignment.horizontal) {
                        HorizontalAlign.START -> SizeFlags.SHRINK_BEGIN
                        HorizontalAlign.CENTER -> SizeFlags.SHRINK_CENTER
                        HorizontalAlign.END -> SizeFlags.SHRINK_END
                        HorizontalAlign.FILL -> SizeFlags.FILL
                    }
                }

                val slotY = currentTopY - slotHeight
                fitChildInRect(child, parentX + padLeft, slotY, availableWidth, slotHeight, horizontalFlags, verticalFlags)
                currentTopY -= slotHeight + actualGap
            }
        } else {
            var currentLeftX = parentX + padLeft + startOffset
            for (i in children.indices) {
                val child = children[i]
                if (!child.visible) continue
                val slotTotalWidth = getChildMinWidth(child)
                val horizontalFlags = child.sizeFlagsHorizontal
                val ratio = child.stretchRatio

                val slotWidth = if ((horizontalFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0.0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalWidth
                }

                val verticalFlags = if ((child.sizeFlagsVertical and SizeFlags.FILL) != 0) {
                    child.sizeFlagsVertical
                } else {
                    when (alignment.vertical) {
                        VerticalAlign.TOP -> SizeFlags.SHRINK_BEGIN
                        VerticalAlign.CENTER -> SizeFlags.SHRINK_CENTER
                        VerticalAlign.BOTTOM -> SizeFlags.SHRINK_END
                        VerticalAlign.FILL -> SizeFlags.FILL
                    }
                }

                fitChildInRect(child, currentLeftX, parentY + padBottom, slotWidth, availableHeight, horizontalFlags, verticalFlags)
                currentLeftX += slotWidth + actualGap
            }
        }
    }

    // --- GRID LAYOUT ---

    fun layoutGrid(
        children: List<UINode>,
        parentX: Float,
        parentY: Float,
        parentWidth: Float,
        parentHeight: Float,
        padLeft: Float,
        padTop: Float,
        padRight: Float,
        padBottom: Float,
        columns: Int,
        horizontalSeparation: Float,
        verticalSeparation: Float
    ) {
        if (columns <= 0) return

        var visibleCount = 0
        for (i in children.indices) {
            if (children[i].visible) visibleCount++
        }
        if (visibleCount == 0) return

        val rows = (visibleCount + columns - 1) / columns
        val availableWidth = maxOf(0.0f, parentWidth - padLeft - padRight)
        val availableHeight = maxOf(0.0f, parentHeight - padTop - padBottom)

        val columnWidths = FloatArray(columns)
        val rowHeights = FloatArray(rows)
        val columnExpand = FloatArray(columns)
        val rowExpand = FloatArray(rows)

        var visibleIndex = 0
        for (i in children.indices) {
            val child = children[i]
            if (!child.visible) continue

            val columnIndex = visibleIndex % columns
            val rowIndex = visibleIndex / columns

            val minWidth = getChildMinWidth(child)
            val minHeight = getChildMinHeight(child)

            columnWidths[columnIndex] = maxOf(columnWidths[columnIndex], minWidth)
            rowHeights[rowIndex] = maxOf(rowHeights[rowIndex], minHeight)

            if ((child.sizeFlagsHorizontal and SizeFlags.EXPAND) != 0) {
                columnExpand[columnIndex] = maxOf(columnExpand[columnIndex], child.stretchRatio)
            }
            if ((child.sizeFlagsVertical and SizeFlags.EXPAND) != 0) {
                rowExpand[rowIndex] = maxOf(rowExpand[rowIndex], child.stretchRatio)
            }
            visibleIndex++
        }

        val totalColumnGaps = if (columns > 1) (columns - 1) * horizontalSeparation else 0.0f
        val totalRowGaps = if (rows > 1) (rows - 1) * verticalSeparation else 0.0f

        val totalMinWidth = columnWidths.sum()
        val totalMinHeight = rowHeights.sum()
        val totalExpandWidth = columnExpand.sum()
        val totalExpandHeight = rowExpand.sum()

        val freeWidth = maxOf(0.0f, availableWidth - totalMinWidth - totalColumnGaps)
        val freeHeight = maxOf(0.0f, availableHeight - totalMinHeight - totalRowGaps)

        if (totalExpandWidth > 0.0f) {
            for (columnIndex in 0 until columns) {
                columnWidths[columnIndex] += freeWidth * (columnExpand[columnIndex] / totalExpandWidth)
            }
        }
        if (totalExpandHeight > 0.0f) {
            for (rowIndex in 0 until rows) {
                rowHeights[rowIndex] += freeHeight * (rowExpand[rowIndex] / totalExpandHeight)
            }
        }

        val columnPositionsX = FloatArray(columns)
        var currentPositionX = parentX + padLeft
        for (columnIndex in 0 until columns) {
            columnPositionsX[columnIndex] = currentPositionX
            currentPositionX += columnWidths[columnIndex] + horizontalSeparation
        }

        val rowPositionsY = FloatArray(rows)
        var currentTopY = parentY + parentHeight - padTop
        for (rowIndex in 0 until rows) {
            val cellHeight = rowHeights[rowIndex]
            rowPositionsY[rowIndex] = currentTopY - cellHeight
            currentTopY -= cellHeight + verticalSeparation
        }

        visibleIndex = 0
        for (i in children.indices) {
            val child = children[i]
            if (!child.visible) continue
            val columnIndex = visibleIndex % columns
            val rowIndex = visibleIndex / columns
            fitChildInRect(child, columnPositionsX[columnIndex], rowPositionsY[rowIndex], columnWidths[columnIndex], rowHeights[rowIndex])
            visibleIndex++
        }
    }

    // --- ANCHOR LAYOUT ---

    fun layoutSingleAnchor(child: UINode, parentX: Float, parentY: Float, parentWidth: Float, parentHeight: Float) {
        val anchor = child.anchorData

        val computedWidth = if (anchor.anchorLeft != anchor.anchorRight) {
            maxOf(0.0f, (parentWidth * anchor.anchorRight + anchor.offsetRight) - (parentWidth * anchor.anchorLeft + anchor.offsetLeft) - child.marginL - child.marginR)
        } else {
            if (child.width > 0.0f) child.width else if (child.getPrefWidth() > 0.0f) child.getPrefWidth() else 0.0f
        }

        val computedHeight = if (anchor.anchorTop != anchor.anchorBottom) {
            maxOf(0.0f, (parentHeight * (1.0f - anchor.anchorTop) - anchor.offsetTop) - (parentHeight * (1.0f - anchor.anchorBottom) + anchor.offsetBottom) - child.marginT - child.marginB)
        } else {
            if (child.height > 0.0f) child.height else if (child.getPrefHeight() > 0.0f) child.getPrefHeight() else 0.0f
        }

        val computedX = when {
            anchor.anchorLeft != anchor.anchorRight -> parentX + parentWidth * anchor.anchorLeft + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 0.0f -> parentX + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 1.0f -> parentX + parentWidth + anchor.offsetRight - computedWidth - child.marginR
            anchor.anchorLeft == 0.5f -> parentX + parentWidth * 0.5f + anchor.offsetLeft - computedWidth * 0.5f + (child.marginL - child.marginR) * 0.5f
            else -> parentX + parentWidth * anchor.anchorLeft + anchor.offsetLeft - computedWidth * anchor.anchorLeft + child.marginL * (1.0f - anchor.anchorLeft) - child.marginR * anchor.anchorLeft
        }

        val computedY = when {
            anchor.anchorTop != anchor.anchorBottom -> parentY + parentHeight * (1.0f - anchor.anchorBottom) + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0.0f -> parentY + parentHeight - anchor.offsetTop - computedHeight - child.marginT
            anchor.anchorTop == 1.0f -> parentY + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0.5f -> parentY + parentHeight * 0.5f - anchor.offsetTop - computedHeight * 0.5f + (child.marginB - child.marginT) * 0.5f
            else -> parentY + parentHeight * (1.0f - anchor.anchorTop) - anchor.offsetTop - computedHeight * (1.0f - anchor.anchorTop) + child.marginB * anchor.anchorTop - child.marginT * (1.0f - anchor.anchorTop)
        }

        child.setBounds(computedX, computedY, computedWidth, computedHeight)
    }
}
