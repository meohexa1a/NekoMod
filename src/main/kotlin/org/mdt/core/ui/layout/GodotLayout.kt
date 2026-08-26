package org.mdt.core.ui.layout

import org.mdt.core.ui.UINode

/**
 * ## GodotLayout
 *
 * 2-Pass container layout algorithm inspired by Godot Engine's UI architecture.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
object GodotLayout {

    fun getChildMinWidth(child: UINode): Float {
        val pref = child.getPrefWidth()
        val base = if (pref > 0f) pref else if (child.minWidth > 0f) child.minWidth else 0f
        return base + child.marginL + child.marginR
    }

    fun getChildMinHeight(child: UINode): Float {
        val pref = child.getPrefHeight()
        val base = if (pref > 0f) pref else if (child.minHeight > 0f) child.minHeight else 0f
        return base + child.marginT + child.marginB
    }

    fun fitChildInRect(
        child: UINode,
        rx: Float, ry: Float, rw: Float, rh: Float,
        hFlags: Int = child.sizeFlagsHorizontal,
        vFlags: Int = child.sizeFlagsVertical
    ) {
        val slotInnerX = rx + child.marginL
        val slotInnerY = ry + child.marginB
        val slotInnerW = maxOf(0f, rw - child.marginL - child.marginR)
        val slotInnerH = maxOf(0f, rh - child.marginT - child.marginB)

        if (child.width > 0f && child.height > 0f) {
            val cx = when {
                (hFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerW - child.width
                (hFlags and SizeFlags.SHRINK_BEGIN) != 0 -> slotInnerX
                else -> slotInnerX + (slotInnerW - child.width) * 0.5f
            }
            val cy = when {
                (vFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerY
                (vFlags and SizeFlags.SHRINK_BEGIN) != 0 -> slotInnerY + slotInnerH - child.height
                else -> slotInnerY + (slotInnerH - child.height) * 0.5f
            }
            child.setBounds(cx, cy, child.width, child.height)
            return
        }

        val childPureMinW = run {
            val pref = child.getPrefWidth()
            if (pref > 0f) pref else if (child.minWidth > 0f) child.minWidth else 0f
        }
        val childPureMinH = run {
            val pref = child.getPrefHeight()
            if (pref > 0f) pref else if (child.minHeight > 0f) child.minHeight else 0f
        }

        // 1. Horizontal axis
        val cw: Float
        val cx: Float
        if ((hFlags and SizeFlags.FILL) == 0) {
            cw = minOf(slotInnerW, childPureMinW)
            cx = when {
                (hFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerX + (slotInnerW - cw) * 0.5f
                (hFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerX + slotInnerW - cw
                else -> slotInnerX // SHRINK_BEGIN
            }
        } else {
            cw = slotInnerW
            cx = slotInnerX
        }

        // 2. Vertical axis (Bottom-left coordinate system: slotInnerY is bottom, slotInnerY + slotInnerH is top)
        val ch: Float
        val cy: Float
        if ((vFlags and SizeFlags.FILL) == 0) {
            ch = minOf(slotInnerH, childPureMinH)
            cy = when {
                (vFlags and SizeFlags.SHRINK_CENTER) != 0 -> slotInnerY + (slotInnerH - ch) * 0.5f
                (vFlags and SizeFlags.SHRINK_END) != 0 -> slotInnerY // SHRINK_END: Bottom of slot
                else -> slotInnerY + slotInnerH - ch // SHRINK_BEGIN: Top of slot
            }
        } else {
            ch = slotInnerH
            cy = slotInnerY
        }

        child.setBounds(cx, cy, cw, ch)
    }

    fun layoutBox(
        children: List<UINode>,
        parentX: Float, parentY: Float,
        parentW: Float, parentH: Float,
        padL: Float, padT: Float, padR: Float, padB: Float,
        isVertical: Boolean,
        arrangement: Arrangement = Arrangement.Start
    ) {
        val availW = maxOf(0f, parentW - padL - padR)
        val availH = maxOf(0f, parentH - padT - padB)

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

        val availMain = if (isVertical) availH else availW
        val visibleCount = visibleChildren.size
        val fixedGap = arrangement.spacing

        var actualGap = fixedGap
        var startOffset = 0f
        var spaceForExpanding = 0f

        if (totalStretchRatio > 0f) {
            val totalGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0f
            spaceForExpanding = maxOf(0f, availMain - unweightedMinSize - totalGaps)
        } else {
            val totalFixedGaps = if (visibleCount > 1) (visibleCount - 1) * fixedGap else 0f
            val remainingSpace = maxOf(0f, availMain - totalMinMain)

            when (arrangement.type) {
                ArrangementType.START -> {
                    startOffset = 0f
                    actualGap = fixedGap
                }
                ArrangementType.CENTER -> {
                    startOffset = maxOf(0f, (availMain - totalMinMain - totalFixedGaps) * 0.5f)
                    actualGap = fixedGap
                }
                ArrangementType.END -> {
                    startOffset = maxOf(0f, availMain - totalMinMain - totalFixedGaps)
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
            var currentTopY = parentY + parentH - padT - startOffset
            for (child in visibleChildren) {
                val slotTotalH = getChildMinHeight(child)
                val vFlags = child.sizeFlagsVertical
                val ratio = child.stretchRatio

                val slotH = if ((vFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalH
                }

                val slotY = currentTopY - slotH
                fitChildInRect(child, parentX + padL, slotY, availW, slotH, child.sizeFlagsHorizontal, vFlags)
                currentTopY -= slotH + actualGap
            }
        } else {
            // Stack from Left to Right
            var currentLeftX = parentX + padL + startOffset
            for (child in visibleChildren) {
                val slotTotalW = getChildMinWidth(child)
                val hFlags = child.sizeFlagsHorizontal
                val ratio = child.stretchRatio

                val slotW = if ((hFlags and SizeFlags.EXPAND) != 0 && totalStretchRatio > 0f) {
                    spaceForExpanding * (ratio / totalStretchRatio)
                } else {
                    slotTotalW
                }

                fitChildInRect(child, currentLeftX, parentY + padB, slotW, availH, hFlags, child.sizeFlagsVertical)
                currentLeftX += slotW + actualGap
            }
        }
    }

    fun layoutGrid(
        children: List<UINode>,
        parentX: Float, parentY: Float,
        parentW: Float, parentH: Float,
        padL: Float, padT: Float, padR: Float, padB: Float,
        columns: Int,
        hSeparation: Float,
        vSeparation: Float
    ) {
        if (columns <= 0) return
        val visibleChildren = children.filter { it.visible }
        if (visibleChildren.isEmpty()) return

        val rows = (visibleChildren.size + columns - 1) / columns
        val availW = maxOf(0f, parentW - padL - padR)
        val availH = maxOf(0f, parentH - padT - padB)

        val colWidths = FloatArray(columns)
        val rowHeights = FloatArray(rows)
        val colExpand = FloatArray(columns)
        val rowExpand = FloatArray(rows)

        for (i in visibleChildren.indices) {
            val child = visibleChildren[i]
            val c = i % columns
            val r = i / columns

            val minW = getChildMinWidth(child)
            val minH = getChildMinHeight(child)

            colWidths[c] = maxOf(colWidths[c], minW)
            rowHeights[r] = maxOf(rowHeights[r], minH)

            if ((child.sizeFlagsHorizontal and SizeFlags.EXPAND) != 0) {
                colExpand[c] = maxOf(colExpand[c], child.stretchRatio)
            }
            if ((child.sizeFlagsVertical and SizeFlags.EXPAND) != 0) {
                rowExpand[r] = maxOf(rowExpand[r], child.stretchRatio)
            }
        }

        val totalColGaps = if (columns > 1) (columns - 1) * hSeparation else 0f
        val totalRowGaps = if (rows > 1) (rows - 1) * vSeparation else 0f

        val totalMinW = colWidths.sum()
        val totalMinH = rowHeights.sum()
        val totalExpandW = colExpand.sum()
        val totalExpandH = rowExpand.sum()

        val freeW = maxOf(0f, availW - totalMinW - totalColGaps)
        val freeH = maxOf(0f, availH - totalMinH - totalRowGaps)

        if (totalExpandW > 0f) {
            for (c in 0 until columns) {
                colWidths[c] += freeW * (colExpand[c] / totalExpandW)
            }
        }
        if (totalExpandH > 0f) {
            for (r in 0 until rows) {
                rowHeights[r] += freeH * (rowExpand[r] / totalExpandH)
            }
        }

        val colXs = FloatArray(columns)
        var curX = parentX + padL
        for (c in 0 until columns) {
            colXs[c] = curX
            curX += colWidths[c] + hSeparation
        }

        val rowYs = FloatArray(rows)
        var curTopY = parentY + parentH - padT
        for (r in 0 until rows) {
            val rh = rowHeights[r]
            rowYs[r] = curTopY - rh
            curTopY -= rh + vSeparation
        }

        for (i in visibleChildren.indices) {
            val child = visibleChildren[i]
            val c = i % columns
            val r = i / columns
            fitChildInRect(child, colXs[c], rowYs[r], colWidths[c], rowHeights[r])
        }
    }

    fun layoutSingleAnchor(child: UINode, parentX: Float, parentY: Float, parentW: Float, parentH: Float) {
        val anchor = child.anchorData

        // Compute width factoring in margins when anchored across edges
        val w = if (anchor.anchorLeft != anchor.anchorRight) {
            maxOf(0f, (parentW * anchor.anchorRight + anchor.offsetRight) - (parentW * anchor.anchorLeft + anchor.offsetLeft) - child.marginL - child.marginR)
        } else {
            if (child.width > 0f) child.width else if (child.getPrefWidth() > 0f) child.getPrefWidth() else 0f
        }

        // Compute height factoring in margins when anchored across edges
        val h = if (anchor.anchorTop != anchor.anchorBottom) {
            maxOf(0f, (parentH * (1f - anchor.anchorTop) - anchor.offsetTop) - (parentH * (1f - anchor.anchorBottom) + anchor.offsetBottom) - child.marginT - child.marginB)
        } else {
            if (child.height > 0f) child.height else if (child.getPrefHeight() > 0f) child.getPrefHeight() else 0f
        }

        // Compute X (Factoring in Margins in standard left-to-right coordinate space)
        val x = when {
            anchor.anchorLeft != anchor.anchorRight -> parentX + parentW * anchor.anchorLeft + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 0f -> parentX + anchor.offsetLeft + child.marginL
            anchor.anchorLeft == 1f -> parentX + parentW + anchor.offsetRight - w - child.marginR
            anchor.anchorLeft == 0.5f -> parentX + parentW * 0.5f + anchor.offsetLeft - w * 0.5f + (child.marginL - child.marginR) * 0.5f
            else -> parentX + parentW * anchor.anchorLeft + anchor.offsetLeft - w * anchor.anchorLeft + child.marginL * (1f - anchor.anchorLeft) - child.marginR * anchor.anchorLeft
        }

        // Compute Y (OpenGL coordinate: y=0 is bottom, y=parentH is top)
        val y = when {
            anchor.anchorTop != anchor.anchorBottom -> parentY + parentH * (1f - anchor.anchorBottom) + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0f -> parentY + parentH - anchor.offsetTop - h - child.marginT
            anchor.anchorTop == 1f -> parentY + anchor.offsetBottom + child.marginB
            anchor.anchorTop == 0.5f -> parentY + parentH * 0.5f - anchor.offsetTop - h * 0.5f + (child.marginB - child.marginT) * 0.5f
            else -> parentY + parentH * (1f - anchor.anchorTop) - anchor.offsetTop - h * (1f - anchor.anchorTop) + child.marginB * anchor.anchorTop - child.marginT * (1f - anchor.anchorTop)
        }

        child.setBounds(x, y, w, h)
    }

    fun layoutAnchors(children: List<UINode>, parentW: Float, parentH: Float, parentX: Float = 0f, parentY: Float = 0f) {
        for (child in children) {
            if (!child.visible) continue
            layoutSingleAnchor(child, parentX, parentY, parentW, parentH)
        }
    }
}
