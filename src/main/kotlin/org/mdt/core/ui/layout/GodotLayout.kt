package org.mdt.core.ui.layout

import org.mdt.core.ui.UINode

/**
 * ## GodotLayout
 *
 * 2-Pass container layout engine inspired by Godot Engine's UI architecture.
 * Manages Flexbox (Row/Column), Grid, and Multi-Anchor responsive layout distributions.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
object GodotLayout {

    // =========================================================================
    // I. Intrinsic Measurement Helpers
    // =========================================================================

    fun getChildMinWidth(child: UINode): Float {
        val preferred = child.getPrefWidth()
        val baseWidth = if (preferred > 0f) preferred else if (child.minWidth > 0f) child.minWidth else 0f
        return baseWidth + child.marginL + child.marginR
    }

    fun getChildMinHeight(child: UINode): Float {
        val preferred = child.getPrefHeight()
        val baseHeight = if (preferred > 0f) preferred else if (child.minHeight > 0f) child.minHeight else 0f
        return baseHeight + child.marginT + child.marginB
    }

    // =========================================================================
    // II. Slot Rectangle Fitting
    // =========================================================================

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
        val slotInnerWidth = maxOf(0f, rectWidth - child.marginL - child.marginR)
        val slotInnerHeight = maxOf(0f, rectHeight - child.marginT - child.marginB)

        // Case 1: Fixed dimensions already explicitly set on child
        if (child.width > 0f && child.height > 0f) {
            val computedX = when {
                (horizontalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerWidth - child.width
                (horizontalFlags and SizeFlags.SHRINK_BEGIN) != 0 -> slotInnerX
                else -> slotInnerX + (slotInnerWidth - child.width) * 0.5f
            }
            val computedY = when {
                (verticalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerY
                (verticalFlags and SizeFlags.SHRINK_BEGIN) != 0 -> slotInnerY + slotInnerHeight - child.height
                else -> slotInnerY + (slotInnerHeight - child.height) * 0.5f
            }

            child.setBounds(computedX, computedY, child.width, child.height)
            return
        }

        val childPureMinWidth = run {
            val preferred = child.getPrefWidth()
            if (preferred > 0f) preferred else if (child.minWidth > 0f) child.minWidth else 0f
        }
        val childPureMinHeight = run {
            val preferred = child.getPrefHeight()
            if (preferred > 0f) preferred else if (child.minHeight > 0f) child.minHeight else 0f
        }

        // 1. Horizontal axis
        val computedWidth: Float
        val computedX: Float
        if ((horizontalFlags and SizeFlags.FILL) == 0) {
            computedWidth = minOf(slotInnerWidth, childPureMinWidth)
            computedX = when {
                (horizontalFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerX + (slotInnerWidth - computedWidth) * 0.5f
                (horizontalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerWidth - computedWidth
                else -> slotInnerX // SHRINK_BEGIN
            }
        } else {
            computedWidth = slotInnerWidth
            computedX = slotInnerX
        }

        // 2. Vertical axis (OpenGL Bottom-Left coordinate: slotInnerY is bottom, slotInnerY + slotInnerHeight is top)
        val computedHeight: Float
        val computedY: Float
        if ((verticalFlags and SizeFlags.FILL) == 0) {
            computedHeight = minOf(slotInnerHeight, childPureMinHeight)
            computedY = when {
                (verticalFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerY + (slotInnerHeight - computedHeight) * 0.5f
                (verticalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerY // SHRINK_END: Bottom of slot
                else -> slotInnerY + slotInnerHeight - computedHeight // SHRINK_BEGIN: Top of slot
            }
        } else {
            computedHeight = slotInnerHeight
            computedY = slotInnerY
        }

        child.setBounds(computedX, computedY, computedWidth, computedHeight)
    }

    // =========================================================================
    // III. Flex & Box Layout (Row / Column)
    // =========================================================================

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
        arrangement: Arrangement = Arrangement.Start
    ) {
        val availableWidth = maxOf(0f, parentWidth - padLeft - padRight)
        val availableHeight = maxOf(0f, parentHeight - padTop - padBottom)

        val visibleChildren = children.filter { it.visible }
        if (visibleChildren.isEmpty()) return

        var unweightedMinSize = 0f
        var totalMinMain = 0f
        var totalStretchRatio = 0f

        for (child in visibleChildren) {
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
        val visibleCount = visibleChildren.size
        val fixedGap = arrangement.spacing

        var actualGap = fixedGap
        var startOffset = 0f
        var spaceForExpanding = 0f

        if (totalStretchRatio > 0f) {
            val totalGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0f
            spaceForExpanding = maxOf(0f, availableMain - unweightedMinSize - totalGaps)
        } else {
            val totalFixedGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0f
            val remainingSpace = maxOf(0f, availableMain - totalMinMain)

            when (arrangement.type) {
                ArrangementType.START -> {
                    startOffset = 0f
                    actualGap = fixedGap
                }
                ArrangementType.CENTER -> {
                    startOffset = maxOf(0f, (availableMain - totalMinMain - totalFixedGaps) * 0.5f)
                    actualGap = fixedGap
                }
                ArrangementType.END -> {
                    startOffset = maxOf(0f, availableMain - totalMinMain - totalFixedGaps)
                    actualGap = fixedGap
                }
                ArrangementType.SPACE_BETWEEN -> {
                    startOffset = 0f
                    actualGap = if (visibleCount > 1) remainingSpace / (visibleCount - 1) else 0f
                }
                ArrangementType.SPACE_AROUND -> {
                    actualGap = if (visibleCount > 0) remainingSpace / visibleCount else 0f
                    startOffset = actualGap * 0.5f
                }
                ArrangementType.SPACE_EVENLY -> {
                    actualGap = if (visibleCount > 0) remainingSpace / (visibleCount + 1) else 0f
                    startOffset = actualGap
                }
            }
        }

        if (isVertical) {
            // Stack from Top to Bottom
            var currentTopY = parentY + parentHeight - padTop - startOffset
            for (child in visibleChildren) {
                val slotTotalHeight = getChildMinHeight(child)
                val verticalFlags = child.sizeFlagsVertical
                val ratio = child.stretchRatio

                val slotHeight = if ((verticalFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalHeight
                }

                val slotY = currentTopY - slotHeight
                fitChildInRect(child, parentX + padLeft, slotY, availableWidth, slotHeight, child.sizeFlagsHorizontal, verticalFlags)
                currentTopY -= slotHeight + actualGap
            }
        } else {
            // Stack from Left to Right
            var currentLeftX = parentX + padLeft + startOffset
            for (child in visibleChildren) {
                val slotTotalWidth = getChildMinWidth(child)
                val horizontalFlags = child.sizeFlagsHorizontal
                val ratio = child.stretchRatio

                val slotWidth = if ((horizontalFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalWidth
                }

                fitChildInRect(child, currentLeftX, parentY + padBottom, slotWidth, availableHeight, horizontalFlags, child.sizeFlagsVertical)
                currentLeftX += slotWidth + actualGap
            }
        }
    }

    // =========================================================================
    // IV. Grid Layout
    // =========================================================================

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
        hSeparation: Float,
        vSeparation: Float
    ) {
        if (columns <= 0) return

        val visibleChildren = children.filter { it.visible }
        if (visibleChildren.isEmpty()) return

        val rows = (visibleChildren.size + columns - 1) / columns
        val availableWidth = maxOf(0f, parentWidth - padLeft - padRight)
        val availableHeight = maxOf(0f, parentHeight - padTop - padBottom)

        val columnWidths = FloatArray(columns)
        val rowHeights = FloatArray(rows)
        val columnExpand = FloatArray(columns)
        val rowExpand = FloatArray(rows)

        for (i in visibleChildren.indices) {
            val child = visibleChildren[i]
            val columnIndex = i % columns
            val rowIndex = i / columns

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
        }

        val totalColumnGaps = if (columns > 1) (columns - 1) * hSeparation else 0f
        val totalRowGaps = if (rows > 1) (rows - 1) * vSeparation else 0f

        val totalMinWidth = columnWidths.sum()
        val totalMinHeight = rowHeights.sum()
        val totalExpandWidth = columnExpand.sum()
        val totalExpandHeight = rowExpand.sum()

        val freeWidth = maxOf(0f, availableWidth - totalMinWidth - totalColumnGaps)
        val freeHeight = maxOf(0f, availableHeight - totalMinHeight - totalRowGaps)

        if (totalExpandWidth > 0f) {
            for (c in 0 until columns) {
                columnWidths[c] += freeWidth * (columnExpand[c] / totalExpandWidth)
            }
        }
        if (totalExpandHeight > 0f) {
            for (r in 0 until rows) {
                rowHeights[r] += freeHeight * (rowExpand[r] / totalExpandHeight)
            }
        }

        val columnPositionsX = FloatArray(columns)
        var currentPositionX = parentX + padLeft
        for (c in 0 until columns) {
            columnPositionsX[c] = currentPositionX
            currentPositionX += columnWidths[c] + hSeparation
        }

        val rowPositionsY = FloatArray(rows)
        var currentTopY = parentY + parentHeight - padTop
        for (r in 0 until rows) {
            val cellHeight = rowHeights[r]
            rowPositionsY[r] = currentTopY - cellHeight
            currentTopY -= cellHeight + vSeparation
        }

        for (i in visibleChildren.indices) {
            val child = visibleChildren[i]
            val columnIndex = i % columns
            val rowIndex = i / columns
            fitChildInRect(child, columnPositionsX[columnIndex], rowPositionsY[rowIndex], columnWidths[columnIndex], rowHeights[rowIndex])
        }
    }

    // =========================================================================
    // V. Anchor Layout
    // =========================================================================

    fun layoutSingleAnchor(child: UINode, parentX: Float, parentY: Float, parentWidth: Float, parentHeight: Float) {
        val anchor = child.anchorData

        // Compute width factoring in margins when anchored across edges
        val computedWidth = if (anchor.anchorLeft != anchor.anchorRight) {
            maxOf(0f, (parentWidth * anchor.anchorRight + anchor.offsetRight) - (parentWidth * anchor.anchorLeft + anchor.offsetLeft) - child.marginL - child.marginR)
        } else {
            if (child.width > 0f) child.width else if (child.getPrefWidth() > 0f) child.getPrefWidth() else 0f
        }

        // Compute height factoring in margins when anchored across edges
        val computedHeight = if (anchor.anchorTop != anchor.anchorBottom) {
            maxOf(0f, (parentHeight * (1f - anchor.anchorTop) - anchor.offsetTop) - (parentHeight * (1f - anchor.anchorBottom) + anchor.offsetBottom) - child.marginT - child.marginB)
        } else {
            if (child.height > 0f) child.height else if (child.getPrefHeight() > 0f) child.getPrefHeight() else 0f
        }

        // Compute X (Factoring in Margins in standard left-to-right coordinate space)
        val computedX = when {
            anchor.anchorLeft != anchor.anchorRight -> parentX + parentWidth * anchor.anchorLeft + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 0f -> parentX + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 1f -> parentX + parentWidth + anchor.offsetRight - computedWidth - child.marginR
            anchor.anchorLeft == 0.5f -> parentX + parentWidth * 0.5f + anchor.offsetLeft - computedWidth * 0.5f + (child.marginL - child.marginR) * 0.5f
            else -> parentX + parentWidth * anchor.anchorLeft + anchor.offsetLeft - computedWidth * anchor.anchorLeft + child.marginL * (1f - anchor.anchorLeft) - child.marginR * anchor.anchorLeft
        }

        // Compute Y (OpenGL coordinate: y=0 is bottom, y=parentHeight is top)
        val computedY = when {
            anchor.anchorTop != anchor.anchorBottom -> parentY + parentHeight * (1f - anchor.anchorBottom) + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0f -> parentY + parentHeight - anchor.offsetTop - computedHeight - child.marginT
            anchor.anchorTop == 1f -> parentY + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0.5f -> parentY + parentHeight * 0.5f - anchor.offsetTop - computedHeight * 0.5f + (child.marginB - child.marginT) * 0.5f
            else -> parentY + parentHeight * (1f - anchor.anchorTop) - anchor.offsetTop - computedHeight * (1f - anchor.anchorTop) + child.marginB * anchor.anchorTop - child.marginT * (1f - anchor.anchorTop)
        }

        child.setBounds(computedX, computedY, computedWidth, computedHeight)
    }

    fun layoutAnchors(children: List<UINode>, parentWidth: Float, parentHeight: Float, parentX: Float = 0f, parentY: Float = 0f) {
        for (child in children) {
            if (!child.visible) continue
            layoutSingleAnchor(child, parentX, parentY, parentWidth, parentHeight)
        }
    }
}
