// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Core Layout Calculation & Flex Math Engine.
// - Operating Mechanism: Calculates AABB bounds, proportional flex weights, stretch ratios, and dual-axis anchor offsets in OpenGL bottom-left space.
// - Invariants: 100% Pure Kotlin (Zero Engine Imports); Float calculations; bottom-left anchor math ($y=0$ bottom).
// - Dependencies: [MeasurePolicy], [AnchorData], [PrimitiveBuffers], [UINode], [LayoutNode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

import org.mdt.core.ui.node.TextNode
import org.mdt.core.ui.node.UINode
import org.mdt.core.ui.unit.Alignment
import org.mdt.core.ui.unit.AnchorData
import org.mdt.core.ui.unit.Arrangement
import org.mdt.core.ui.unit.ArrangementType
import org.mdt.core.ui.unit.GrowDirection
import org.mdt.core.ui.unit.HorizontalAlign
import org.mdt.core.ui.unit.SizeFlags
import org.mdt.core.ui.unit.VerticalAlign

/**
 * ## GodotLayout
 *
 * Core layout math engine calculating positions and dimensions for child UI nodes.
 * Handles Box layouts (Row/Column flex weights), FlowRows, Grids, and dual-axis Anchors in OpenGL bottom-left coordinates.
 *
 * @see MeasurePolicy
 * @see org.mdt.core.ui.node.LayoutNode
 * @see org.mdt.core.ui.node.UINode
 * @see AnchorData
 */
object GodotLayout {

    private val scratchSlotSizes = FloatArray(128)

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
        verticalFlags: Int = child.sizeFlagsVertical,
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
            (horizontalFlags and SizeFlags.FILL) != 0 -> slotInnerWidth
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
            (verticalFlags and SizeFlags.FILL) != 0 -> slotInnerHeight
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
        alignment: Alignment = if (isVertical) Alignment.TopStart else Alignment.CenterStart,
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
            var remainingFreeSpace = maxOf(0.0f, availableMain - unweightedMinSize - totalGaps)
            var remainingStretchRatio = totalStretchRatio
            var frozenMask = 0L

            // Pass 1: Freeze expanding elements whose minSize exceeds their proportional share
            var repeatPass = true
            var passes = 0
            while (repeatPass && passes < 4) {
                repeatPass = false
                passes++
                for (i in children.indices) {
                    if (i >= 64) break
                    val child = children[i]
                    if (!child.visible) continue
                    val flags = if (isVertical) child.sizeFlagsVertical else child.sizeFlagsHorizontal
                    val isExpand = (flags and SizeFlags.EXPAND) != 0

                    if (isExpand && (frozenMask and (1L shl i)) == 0L) {
                        val minSize = if (isVertical) {
                            val childAvailableWidth = when {
                                (child.sizeFlagsHorizontal and SizeFlags.FILL) != 0 -> availableWidth
                                child.width > 0.0f -> child.width
                                else -> availableWidth
                            }
                            getChildMinHeight(child, childAvailableWidth)
                        } else {
                            getChildMinWidth(child)
                        }

                        val share =
                            if (remainingStretchRatio > 0.0f) remainingFreeSpace * (child.stretchRatio / remainingStretchRatio) else 0.0f
                        if (minSize > share) {
                            frozenMask = frozenMask or (1L shl i)
                            scratchSlotSizes[i] = minSize
                            remainingFreeSpace = maxOf(0.0f, remainingFreeSpace - minSize)
                            remainingStretchRatio = maxOf(0.0f, remainingStretchRatio - child.stretchRatio)
                            repeatPass = true
                        }
                    }
                }
            }

            // Assign final slot sizes
            for (i in children.indices) {
                if (i >= 128) break
                val child = children[i]
                if (!child.visible) continue
                val flags = if (isVertical) child.sizeFlagsVertical else child.sizeFlagsHorizontal
                val isExpand = (flags and SizeFlags.EXPAND) != 0

                scratchSlotSizes[i] = when {
                    !isExpand -> if (isVertical) {
                        val childAvailableWidth = when {
                            (child.sizeFlagsHorizontal and SizeFlags.FILL) != 0 -> availableWidth
                            child.width > 0.0f -> child.width
                            else -> availableWidth
                        }
                        getChildMinHeight(child, childAvailableWidth)
                    } else getChildMinWidth(child)

                    i < 64 && (frozenMask and (1L shl i)) != 0L -> scratchSlotSizes[i]
                    remainingStretchRatio > 0.0f -> remainingFreeSpace * (child.stretchRatio / remainingStretchRatio)
                    else -> if (isVertical) getChildMinHeight(child) else getChildMinWidth(child)
                }
            }
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

                val slotHeight = if (totalStretchRatio > 0.0f && i < 128) {
                    scratchSlotSizes[i]
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
                fitChildInRect(
                    child,
                    parentX + padLeft,
                    slotY,
                    availableWidth,
                    slotHeight,
                    horizontalFlags,
                    verticalFlags,
                )
                currentTopY -= slotHeight + actualGap
            }
        } else {
            var currentLeftX = parentX + padLeft + startOffset
            for (i in children.indices) {
                val child = children[i]
                if (!child.visible) continue
                val slotTotalWidth = getChildMinWidth(child)
                val horizontalFlags = child.sizeFlagsHorizontal

                val slotWidth = if (totalStretchRatio > 0.0f && i < 128) {
                    scratchSlotSizes[i]
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

                fitChildInRect(
                    child,
                    currentLeftX,
                    parentY + padBottom,
                    slotWidth,
                    availableHeight,
                    horizontalFlags,
                    verticalFlags,
                )
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
