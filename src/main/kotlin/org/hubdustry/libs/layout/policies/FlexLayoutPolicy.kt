package org.hubdustry.libs.layout.policies

import org.hubdustry.libs.layout.LayoutNode
import org.hubdustry.libs.layout.LayoutPolicy
import org.hubdustry.libs.layout.Orientation
import org.hubdustry.libs.layout.SizeFlag
import org.hubdustry.libs.layout.computeOffset

/**
 * Bố cục tuyến tính Flex đa năng (hỗ trợ cả HBox/Row và VBox/Column).
 * Tuân thủ nghiêm ngặt kỷ luật Zero-GC:
 * - Không cấp phát bộ nhớ heap (0 allocation) trong vòng lặp layout.
 * - Sử dụng vòng lặp chỉ mục (index loop) thay vì Iterator hoặc Higher-Order Functions.
 * - Giải quyết triệt để lỗi kẹp trần (Clamping Redistribution) và hấp thụ sai số float (Remainder Absorption).
 * - Phòng vệ tuyệt đối trước Infinity, NaN và kích thước âm.
 */
class FlexLayoutPolicy(
    val orientation: Orientation,
    val gap: Float = 0f
) : LayoutPolicy {

    override fun computeMinSize(node: LayoutNode) {
        val isHorizontal = orientation == Orientation.HORIZONTAL
        var totalMinMain = 0f
        var maxMinCross = 0f
        var visibleCount = 0

        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue

            child.policy.computeMinSize(child)
            val childMinMain = if (isHorizontal) child.minWidth else child.minHeight
            val childMinCross = if (isHorizontal) child.minHeight else child.minWidth

            totalMinMain += if (childMinMain.isNaN() || childMinMain < 0f) 0f else childMinMain
            val safeCross = if (childMinCross.isNaN() || childMinCross < 0f) 0f else childMinCross
            if (safeCross > maxMinCross) {
                maxMinCross = safeCross
            }
            visibleCount++
        }

        val effectiveGap = if (gap.isNaN() || gap < 0f) 0f else gap
        val gapsTotal = if (visibleCount > 1) (visibleCount - 1) * effectiveGap else 0f
        val padLeft = if (node.paddingLeft.isNaN() || node.paddingLeft < 0f) 0f else node.paddingLeft
        val padRight = if (node.paddingRight.isNaN() || node.paddingRight < 0f) 0f else node.paddingRight
        val padTop = if (node.paddingTop.isNaN() || node.paddingTop < 0f) 0f else node.paddingTop
        val padBottom = if (node.paddingBottom.isNaN() || node.paddingBottom < 0f) 0f else node.paddingBottom

        val padMain = if (isHorizontal) padLeft + padRight else padTop + padBottom
        val padCross = if (isHorizontal) padTop + padBottom else padLeft + padRight

        if (isHorizontal) {
            node.minWidth = maxOf(node.minWidth, totalMinMain + gapsTotal + padMain)
            node.minHeight = maxOf(node.minHeight, maxMinCross + padCross)
        } else {
            node.minWidth = maxOf(node.minWidth, maxMinCross + padCross)
            node.minHeight = maxOf(node.minHeight, totalMinMain + gapsTotal + padMain)
        }
    }

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val isHorizontal = orientation == Orientation.HORIZONTAL
        val rawAvailableMain = if (isHorizontal) innerWidth else innerHeight
        val rawAvailableCross = if (isHorizontal) innerHeight else innerWidth
        val startMain = if (isHorizontal) innerX else innerY
        val startCross = if (isHorizontal) innerY else innerX

        val isUnconstrainedMain = rawAvailableMain.isNaN() ||
                rawAvailableMain == Float.MAX_VALUE ||
                rawAvailableMain.isInfinite() ||
                rawAvailableMain <= 0f

        val isUnconstrainedCross = rawAvailableCross.isNaN() ||
                rawAvailableCross == Float.MAX_VALUE ||
                rawAvailableCross.isInfinite() ||
                rawAvailableCross <= 0f

        val safeAvailableMain = if (isUnconstrainedMain) 0f else rawAvailableMain
        val safeAvailableCross = if (isUnconstrainedCross) 0f else rawAvailableCross

        val count = node.children.size
        var totalMinMain = 0f
        var totalStretchRatio = 0f
        var visibleCount = 0

        val effectiveGap = if (gap.isNaN() || gap < 0f) 0f else gap

        // Khởi tạo scratchpad & Pass 1: Tính toán tổng minMain và tổng stretchRatio
        for (i in 0 until count) {
            val child = node.children[i]
            child.isFrozen = false

            if (!child.visible) {
                child.tempMain = 0f
                continue
            }

            val childMinMain = if (isHorizontal) child.minWidth else child.minHeight
            val safeMinMain = if (childMinMain.isNaN() || childMinMain < 0f) 0f else childMinMain
            child.tempMain = safeMinMain
            totalMinMain += safeMinMain

            val flagMain = if (isHorizontal) child.sizeFlagHorizontal else child.sizeFlagVertical
            val ratio = if (child.stretchRatio.isNaN() || child.stretchRatio < 0f) 0f else child.stretchRatio
            if (flagMain == SizeFlag.EXPAND && ratio > 0f) {
                totalStretchRatio += ratio
            }
            visibleCount++
        }

        val gapsTotal = if (visibleCount > 1) (visibleCount - 1) * effectiveGap else 0f
        val freeSpace = if (isUnconstrainedMain) 0f else maxOf(0f, safeAvailableMain - totalMinMain - gapsTotal)

        // Phân bổ freeSpace với thuật toán Clamping Redistribution và Remainder Absorption
        if (freeSpace > 0f && totalStretchRatio > 0f) {
            var remainingFreeSpace = freeSpace
            var remainingStretchRatio = totalStretchRatio

            while (remainingStretchRatio > 0f && remainingFreeSpace > 0f) {
                var newlyClamped = false
                val spacePerRatio = remainingFreeSpace / remainingStretchRatio

                for (i in 0 until count) {
                    val child = node.children[i]
                    if (!child.visible || child.isFrozen) continue

                    val flagMain = if (isHorizontal) child.sizeFlagHorizontal else child.sizeFlagVertical
                    val ratio = if (child.stretchRatio.isNaN() || child.stretchRatio < 0f) 0f else child.stretchRatio
                    if (flagMain != SizeFlag.EXPAND || ratio <= 0f) continue

                    val childMin = if (isHorizontal) child.minWidth else child.minHeight
                    val childMax = if (isHorizontal) child.maxWidth else child.maxHeight
                    val safeMin = if (childMin.isNaN() || childMin < 0f) 0f else childMin
                    val effectiveMax = if (childMax.isNaN() || childMax < safeMin) safeMin else childMax

                    val tentative = safeMin + spacePerRatio * ratio
                    if (tentative >= effectiveMax) {
                        child.isFrozen = true
                        child.tempMain = effectiveMax
                        val consumed = maxOf(0f, effectiveMax - safeMin)
                        remainingFreeSpace = maxOf(0f, remainingFreeSpace - consumed)
                        remainingStretchRatio = maxOf(0f, remainingStretchRatio - ratio)
                        newlyClamped = true
                        break
                    }
                }

                if (!newlyClamped) {
                    // Không còn con nào bị kẹp trần -> phân bổ trọn vẹn phần còn lại
                    var unfrozenExpandCount = 0
                    for (i in 0 until count) {
                        val child = node.children[i]
                        if (!child.visible || child.isFrozen) continue
                        val flagMain = if (isHorizontal) child.sizeFlagHorizontal else child.sizeFlagVertical
                        val ratio = if (child.stretchRatio.isNaN() || child.stretchRatio < 0f) 0f else child.stretchRatio
                        if (flagMain == SizeFlag.EXPAND && ratio > 0f) {
                            unfrozenExpandCount++
                        }
                    }

                    var distributedFreeSpace = 0f
                    var processedUnfrozen = 0
                    for (i in 0 until count) {
                        val child = node.children[i]
                        if (!child.visible || child.isFrozen) continue

                        val flagMain = if (isHorizontal) child.sizeFlagHorizontal else child.sizeFlagVertical
                        val ratio = if (child.stretchRatio.isNaN() || child.stretchRatio < 0f) 0f else child.stretchRatio
                        if (flagMain == SizeFlag.EXPAND && ratio > 0f) {
                            processedUnfrozen++
                            val childMin = if (isHorizontal) child.minWidth else child.minHeight
                            val childMax = if (isHorizontal) child.maxWidth else child.maxHeight
                            val safeMin = if (childMin.isNaN() || childMin < 0f) 0f else childMin
                            val effectiveMax = if (childMax.isNaN() || childMax < safeMin) safeMin else childMax

                            val additional = if (processedUnfrozen == unfrozenExpandCount) {
                                // Phần tử cuối cùng hấp thụ sai số float sub-pixel
                                maxOf(0f, remainingFreeSpace - distributedFreeSpace)
                            } else {
                                remainingFreeSpace * (ratio / remainingStretchRatio)
                            }

                            val allocated = safeMin + additional
                            child.tempMain = allocated.coerceIn(safeMin, effectiveMax)
                            distributedFreeSpace += maxOf(0f, child.tempMain - safeMin)
                        }
                    }
                    break
                }
            }
        }

        // Pass 2: Định vị và gán kích thước hình học chính xác
        var currentMain = startMain
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue

            val slotMain = child.tempMain
            val sizeFlagMain = if (isHorizontal) child.sizeFlagHorizontal else child.sizeFlagVertical
            val sizeFlagCross = if (isHorizontal) child.sizeFlagVertical else child.sizeFlagHorizontal
            val alignMain = if (isHorizontal) child.alignHorizontal else child.alignVertical
            val alignCross = if (isHorizontal) child.alignVertical else child.alignHorizontal

            val childMinMain = if (isHorizontal) child.minWidth else child.minHeight
            val childMaxMain = if (isHorizontal) child.maxWidth else child.maxHeight
            val safeMinMain = if (childMinMain.isNaN() || childMinMain < 0f) 0f else childMinMain
            val safeMaxMain = if (childMaxMain.isNaN() || childMaxMain < safeMinMain) safeMinMain else childMaxMain

            val childMinCross = if (isHorizontal) child.minHeight else child.minWidth
            val childMaxCross = if (isHorizontal) child.maxHeight else child.maxWidth
            val safeMinCross = if (childMinCross.isNaN() || childMinCross < 0f) 0f else childMinCross
            val safeMaxCross = if (childMaxCross.isNaN() || childMaxCross < safeMinCross) safeMinCross else childMaxCross

            val childMain = when (sizeFlagMain) {
                SizeFlag.EXPAND, SizeFlag.FILL -> slotMain
                SizeFlag.SHRINK -> safeMinMain
            }.coerceIn(safeMinMain, safeMaxMain)

            val alignOffsetMain = alignMain.computeOffset(slotMain, childMain)

            val childCross = when (sizeFlagCross) {
                SizeFlag.FILL, SizeFlag.EXPAND -> if (isUnconstrainedCross) safeMinCross else safeAvailableCross
                SizeFlag.SHRINK -> safeMinCross
            }.coerceIn(safeMinCross, safeMaxCross)

            val alignOffsetCross = alignCross.computeOffset(if (isUnconstrainedCross) childCross else safeAvailableCross, childCross)

            if (isHorizontal) {
                child.x = currentMain + alignOffsetMain
                child.y = startCross + alignOffsetCross
                child.width = childMain
                child.height = childCross
            } else {
                child.x = startCross + alignOffsetCross
                child.y = currentMain + alignOffsetMain
                child.width = childCross
                child.height = childMain
            }

            currentMain += slotMain + effectiveGap

            // Bố cục đệ quy cho cây con
            val childInnerX = child.paddingLeft
            val childInnerY = child.paddingTop
            val childInnerW = maxOf(0f, child.width - child.paddingLeft - child.paddingRight)
            val childInnerH = maxOf(0f, child.height - child.paddingTop - child.paddingBottom)
            child.policy.arrangeChildren(child, childInnerX, childInnerY, childInnerW, childInnerH)
        }
    }
}

/** Factory function tạo [FlexLayoutPolicy] theo hàng ngang (Row/HBox). */
fun RowPolicy(gap: Float = 0f): FlexLayoutPolicy = FlexLayoutPolicy(Orientation.HORIZONTAL, gap)

/** Factory function tạo [FlexLayoutPolicy] theo hàng dọc (Column/VBox). */
fun ColumnPolicy(gap: Float = 0f): FlexLayoutPolicy = FlexLayoutPolicy(Orientation.VERTICAL, gap)

/** Alias tương thích ngược cho RowLayoutPolicy. */
fun RowLayoutPolicy(gap: Float = 0f): FlexLayoutPolicy = RowPolicy(gap)

/** Alias tương thích ngược cho ColumnLayoutPolicy. */
fun ColumnLayoutPolicy(gap: Float = 0f): FlexLayoutPolicy = ColumnPolicy(gap)
