package org.mdt.core.ui.layout

import org.mdt.core.ui.node.TextNode
import org.mdt.core.ui.node.UINode

/**
 * ## GodotLayout [Layout Engine]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Calculates positions and dimensions for child UI nodes using Godot-style layout policies.
 * - Handles Box layouts (Row/Column flex weights), Grids, and dual-axis Anchors.
 * - Supports padding, margins, and size flags (`FILL`, `EXPAND`, `SHRINK_CENTER`).
 *
 * ### 2. Key Rules & Checklist
 * - [x] All layout calculations must use OpenGL bottom-left origin (`y=0` bottom, `y=parentH` top).
 * - [x] Use indexed `for` loops instead of lambdas (`.filter {}`, `.map {}`) to avoid per-frame GC allocations.
 * - [x] Unconstrained containers must hug content size by default.
 * - [x] `layoutBox` distributes free main-axis space proportionally based on child `stretchRatio` when `EXPAND` is set.
 *
 * ### 3. Related Files
 * - Measure Policy: `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 * - Layout Virtual Node: `src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt`
 * - Base Virtual Node: `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 * - Root Screen Node: `src/main/kotlin/org/mdt/core/ui/node/CanvasNode.kt`
 */
object GodotLayout {

    // --- INTRINSIC MEASUREMENT HELPERS ---

    fun getChildMinWidth(child: UINode): Float {
        val preferredWidth = child.getPrefWidth()
        val baseWidth = when {
            preferredWidth > 0.0f -> preferredWidth
            child.minWidth > 0.0f -> child.minWidth
            else -> 0.0f
        }
        return baseWidth + child.marginL + child.marginR
    }

    fun getChildMinHeight(child: UINode, availableWidth: Float = -1.0f): Float {
        val preferredHeight = child.getPrefHeight(availableWidth)
        val baseHeight = when {
            preferredHeight > 0.0f -> preferredHeight
            child.minHeight > 0.0f -> child.minHeight
            else -> 0.0f
        }
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

        val childPureMinWidth = when {
            child.getPrefWidth() > 0.0f -> child.getPrefWidth()
            child.minWidth > 0.0f -> child.minWidth
            else -> 0.0f
        }
        val childPureMinHeight = when {
            child.getPrefHeight() > 0.0f -> child.getPrefHeight()
            child.minHeight > 0.0f -> child.minHeight
            else -> 0.0f
        }

        // 1. Horizontal Dimension & Position
        val computedWidth: Float = when {
            child.width >= 0.0f -> child.width
            (horizontalFlags and SizeFlags.FILL) != 0 -> maxOf(slotInnerWidth, childPureMinWidth)
            child is TextNode && child.wrap -> slotInnerWidth
            else -> childPureMinWidth
        }

        val computedX: Float = when {
            (horizontalFlags and SizeFlags.FILL) != 0 && child.width < 0.0f -> slotInnerX
            (horizontalFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerWidth - computedWidth
            (horizontalFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerX + (slotInnerWidth - computedWidth) * 0.5f
            else -> slotInnerX // Default / SHRINK_BEGIN: Left-aligned
        }

        // 2. Vertical Dimension & Position (OpenGL Bottom-Left)
        val computedHeight: Float = when {
            child.height >= 0.0f -> child.height
            (verticalFlags and SizeFlags.FILL) != 0 -> maxOf(slotInnerHeight, childPureMinHeight)
            else -> childPureMinHeight
        }

        val computedY: Float = when {
            (verticalFlags and SizeFlags.FILL) != 0 && child.height < 0.0f -> slotInnerY
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

                val horizontalFlags = if (child.sizeFlagsHorizontal != 0) {
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

                val verticalFlags = if (child.sizeFlagsVertical != 0) {
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

    // --- ANCHOR LAYOUT (GODOT 4 ALIGNED) ---

    fun layoutSingleAnchor(child: UINode, parentX: Float, parentY: Float, parentWidth: Float, parentHeight: Float) {
        val anchor = child.anchorData
        val isHorizontalSpanned = anchor.anchorLeft != anchor.anchorRight
        val isVerticalSpanned = anchor.anchorTop != anchor.anchorBottom

        val computedWidth = when {
            isHorizontalSpanned -> {
                val rightEdge = parentWidth * anchor.anchorRight + anchor.offsetRight
                val leftEdge = parentWidth * anchor.anchorLeft + anchor.offsetLeft
                maxOf(0.0f, rightEdge - leftEdge - child.marginL - child.marginR)
            }
            child.width > 0.0f -> child.width
            child.getPrefWidth() > 0.0f -> child.getPrefWidth()
            else -> 0.0f
        }

        val computedHeight = when {
            isVerticalSpanned -> {
                val topEdge = parentHeight * (1.0f - anchor.anchorTop) - anchor.offsetTop
                val bottomEdge = parentHeight * (1.0f - anchor.anchorBottom) + anchor.offsetBottom
                maxOf(0.0f, topEdge - bottomEdge - child.marginT - child.marginB)
            }
            child.height > 0.0f -> child.height
            child.getPrefHeight() > 0.0f -> child.getPrefHeight()
            else -> 0.0f
        }

        // 1. Horizontal Positioning with GrowDirection
        val computedX = when {
            isHorizontalSpanned -> parentX + parentWidth * anchor.anchorLeft + anchor.offsetLeft + child.marginL
            else -> {
                val anchorPointX = parentX + parentWidth * anchor.anchorLeft
                when (anchor.growHorizontal) {
                    GrowDirection.END -> anchorPointX + anchor.offsetLeft + child.marginL
                    GrowDirection.BEGIN -> anchorPointX + anchor.offsetRight - computedWidth - child.marginR
                    GrowDirection.BOTH -> anchorPointX + (anchor.offsetLeft + anchor.offsetRight) * 0.5f - computedWidth * 0.5f + (child.marginL - child.marginR) * 0.5f
                }
            }
        }

        // 2. Vertical Positioning with GrowDirection (OpenGL Bottom-Left Coordinates)
        val computedY = when {
            isVerticalSpanned -> parentY + parentHeight * (1.0f - anchor.anchorBottom) + anchor.offsetBottom + child.marginB
            else -> {
                val anchorPointY = parentY + parentHeight * (1.0f - anchor.anchorTop)
                when (anchor.growVertical) {
                    GrowDirection.END -> anchorPointY - anchor.offsetTop - computedHeight - child.marginT
                    GrowDirection.BEGIN -> anchorPointY + anchor.offsetBottom + child.marginB
                    GrowDirection.BOTH -> anchorPointY + (anchor.offsetBottom - anchor.offsetTop) * 0.5f - computedHeight * 0.5f + (child.marginB - child.marginT) * 0.5f
                }
            }
        }

        child.setBounds(computedX, computedY, computedWidth, computedHeight)
    }
}
