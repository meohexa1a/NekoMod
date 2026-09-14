package org.hubdustry.core.layout.policies

import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.Orientation
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.computeOffset

/**
 * Bố cục tuyến tính Flex đa năng (hỗ trợ cả HBox/Row và VBox/Column).
 * Tuân thủ nghiêm ngặt kỷ luật Zero-GC và Pure Math:
 * - 1D Axis Projection: Xóa sổ 100% phân mảnh rẽ nhánh ngang/dọc.
 * - Tự trị đệ quy: Giao toàn quyền định vị và cascade đệ quy cho LayoutNode.
 * - Giải quyết triệt để lỗi kẹp trần (Clamping Redistribution) và hấp thụ sai số float (Remainder Absorption).
 * - Phòng vệ tuyệt đối trước Infinity, NaN và kích thước âm.
 *
 * Sơ đồ thuật toán Clamping Redistribution:
 *
 *  freeSpace = availableMain - totalMinMain - gapsTotal
 *      │
 *      ├─ freeSpace <= 0 ──► Skip redistribution, dùng minSize cho tất cả
 *      │
 *      └─ freeSpace > 0:
 *          while (unfrozen FILL children còn lại):
 *              spacePerRatio = remainingFreeSpace / remainingStretchRatio
 *              │
 *              ├─ Tìm child: tentative = min + spacePerRatio * ratio > max?
 *              │       └─ Có → Freeze child (gán max, trả lại phần dư)
 *              │               Lặp lại vòng while
 *              │
 *              └─ Không có child nào bị kẹp:
 *                      Phân bổ cuối (Remainder Absorption)
 *                      └─ Child cuối nhận toàn bộ phần dư float precision
 */
class FlexLayoutPolicy(
    val orientation: Orientation,
    val gap: Float = 0f
) : LayoutPolicy {

    override fun computeMinSize(node: LayoutNode) {
        var totalMinMain = 0f
        var maxMinCross = 0f
        var visibleCount = 0
        val cross = orientation.cross()

        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue
            child.policy.computeMinSize(child)
            if (child.anchor.isEnabled) continue

            totalMinMain += child.minSize(orientation) + child.marginMain(orientation)
            val childMinCross = child.minSize(cross) + child.marginCross(orientation)
            if (childMinCross > maxMinCross) {
                maxMinCross = childMinCross
            }
            visibleCount++
        }

        val effectiveGap = if (gap.isNaN() || gap < 0f) 0f else gap
        val gapsTotal = if (visibleCount > 1) (visibleCount - 1) * effectiveGap else 0f
        val padMain = node.paddingMain(orientation)
        val padCross = node.paddingCross(orientation)

        val pureContentMain = totalMinMain + gapsTotal
        val pureContentCross = maxMinCross

        val totalCalculatedMain = pureContentMain + padMain
        val totalCalculatedCross = pureContentCross + padCross

        val isScrollMain = (orientation == Orientation.VERTICAL && node.isScrollableVertical) ||
                (orientation == Orientation.HORIZONTAL && node.isScrollableHorizontal)

        // contentWidth / contentHeight là kích thước thuần của nội dung bên trong inner bounds (tránh cộng đúp padding khi cuộn)
        node.setContentSize(orientation, main = pureContentMain, cross = pureContentCross)
        val minMain = if (isScrollMain) padMain else totalCalculatedMain
        node.setMinSizeByAxis(
            orientation,
            maxOf(node.minSize(orientation), minMain).coerceAtMost(node.maxSize(orientation)),
            maxOf(node.minSize(cross), totalCalculatedCross).coerceAtMost(node.maxSize(cross))
        )
    }

    private var allocatedMainSlots = FloatArray(32)
    private var isSlotFrozen = BooleanArray(32)

    private fun ensureCapacity(size: Int) {
        if (allocatedMainSlots.size < size) {
            val newCap = maxOf(size, allocatedMainSlots.size * 2)
            allocatedMainSlots = FloatArray(newCap)
            isSlotFrozen = BooleanArray(newCap)
        }
    }

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val cross = orientation.cross()
        val rawAvailableMain = orientation.main(innerWidth, innerHeight)
        val rawAvailableCross = orientation.cross(innerWidth, innerHeight)
        val startMain = orientation.main(innerX, innerY)
        val startCross = orientation.cross(innerX, innerY)

        val isUnconstrainedMain = rawAvailableMain.isNaN() || rawAvailableMain == Float.MAX_VALUE || rawAvailableMain.isInfinite() || rawAvailableMain <= 0f
        val isUnconstrainedCross = rawAvailableCross.isNaN() || rawAvailableCross == Float.MAX_VALUE || rawAvailableCross.isInfinite() || rawAvailableCross <= 0f

        val safeAvailableMain = if (isUnconstrainedMain) 0f else rawAvailableMain
        val safeAvailableCross = if (isUnconstrainedCross) 0f else rawAvailableCross

        val count = node.children.size
        ensureCapacity(count)
        var totalMinMain = 0f
        var totalStretchRatio = 0f
        var visibleCount = 0
        val effectiveGap = if (gap.isNaN() || gap < 0f) 0f else gap

        // ── Pass 1: Collect intrinsic metrics & stretch ratios ────────────────
        for (i in 0 until count) {
            val child = node.children[i]
            isSlotFrozen[i] = false

            if (!child.visible || child.anchor.isEnabled) {
                allocatedMainSlots[i] = 0f
                continue
            }

            val childMin = child.minSize(orientation)
            allocatedMainSlots[i] = childMin
            totalMinMain += childMin + child.marginMain(orientation)

            if (child.sizeFlag(orientation) == SizeFlag.FILL && child.stretchRatio > 0f) {
                totalStretchRatio += child.stretchRatio
            }
            visibleCount++
        }

        val gapsTotal = if (visibleCount > 1) (visibleCount - 1) * effectiveGap else 0f
        val freeSpace = if (isUnconstrainedMain) 0f else maxOf(0f, safeAvailableMain - totalMinMain - gapsTotal)

        // ── Clamping Redistribution & Remainder Absorption ───────────────────
        fun redistributeFreeSpace(initialFreeSpace: Float, initialTotalRatio: Float) {
            var remainingFreeSpace = initialFreeSpace
            var remainingStretchRatio = initialTotalRatio

            while (remainingStretchRatio > 1e-5f && remainingFreeSpace > 1e-4f) {
                var newlyClamped = false
                val spacePerRatio = remainingFreeSpace / remainingStretchRatio

                for (i in 0 until count) {
                    val child = node.children[i]
                    if (!child.visible || isSlotFrozen[i] || child.anchor.isEnabled) continue
                    if (child.sizeFlag(orientation) != SizeFlag.FILL || child.stretchRatio <= 0f) continue

                    val childMin = child.minSize(orientation)
                    val childMax = child.maxSize(orientation)
                    val tentative = childMin + spacePerRatio * child.stretchRatio

                    if (tentative >= childMax) {
                        isSlotFrozen[i] = true
                        allocatedMainSlots[i] = childMax
                        remainingFreeSpace = maxOf(0f, remainingFreeSpace - (childMax - childMin))
                        remainingStretchRatio = maxOf(0f, remainingStretchRatio - child.stretchRatio)
                        newlyClamped = true
                        break
                    }
                }

                // Nếu có node bị kẹp trần, lập tức tính toán lại từ đầu với không gian mới
                if (newlyClamped) continue

                // ── Remainder Absorption (khi không còn node nào bị kẹp) ────────
                var unfrozenFillCount = 0
                for (i in 0 until count) {
                    val child = node.children[i]
                    if (!child.visible || isSlotFrozen[i] || child.anchor.isEnabled) continue
                    if (child.sizeFlag(orientation) != SizeFlag.FILL || child.stretchRatio <= 0f) continue

                    unfrozenFillCount++
                }

                var distributedFreeSpace = 0f
                var processedUnfrozen = 0
                for (i in 0 until count) {
                    val child = node.children[i]
                    if (!child.visible || isSlotFrozen[i] || child.anchor.isEnabled) continue
                    if (child.sizeFlag(orientation) != SizeFlag.FILL || child.stretchRatio <= 0f) continue

                    processedUnfrozen++
                    val childMin = child.minSize(orientation)
                    val childMax = child.maxSize(orientation)

                    val additional = if (processedUnfrozen == unfrozenFillCount) {
                        maxOf(0f, remainingFreeSpace - distributedFreeSpace)
                    } else {
                        remainingFreeSpace * (child.stretchRatio / remainingStretchRatio)
                    }

                    allocatedMainSlots[i] = (childMin + additional).coerceIn(childMin, childMax)
                    distributedFreeSpace += maxOf(0f, allocatedMainSlots[i] - childMin)
                }
                break
            }
        }

        if (freeSpace > 0f && totalStretchRatio > 0f) {
            redistributeFreeSpace(freeSpace, totalStretchRatio)
        }

        // ── Pass 2: Position children along main axis ─────────────────────────
        var currentMain = startMain
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || child.anchor.isEnabled) continue

            val marginLeadMain = child.marginLeading(orientation)
            val marginTrailMain = child.marginTrailing(orientation)
            val marginLeadCross = child.marginCrossLeading(orientation)
            val marginTotalCross = child.marginCross(orientation)

            val slotMain = allocatedMainSlots[i]
            val childMinMain = child.minSize(orientation)
            val childMaxMain = child.maxSize(orientation)
            val childMinCross = child.minSize(cross)
            val childMaxCross = child.maxSize(cross)

            val childMain = when (child.sizeFlag(orientation)) {
                SizeFlag.FILL -> slotMain
                SizeFlag.SHRINK -> childMinMain
            }.coerceIn(childMinMain, childMaxMain)

            val alignOffsetMain = child.alignment(orientation).computeOffset(slotMain, childMain)

            val availableCross = maxOf(0f, safeAvailableCross - marginTotalCross)
            val shouldFillCross = child.sizeFlag(cross) == SizeFlag.FILL && !isUnconstrainedCross
            val childCross = (if (shouldFillCross) availableCross else childMinCross).coerceAtMost(childMaxCross)

            val crossSlot = if (isUnconstrainedCross) childCross else availableCross
            val alignOffsetCross = child.alignment(cross).computeOffset(crossSlot, childCross)

            child.arrangeAxis(
                orientation,
                mainPos = currentMain + marginLeadMain + alignOffsetMain,
                crossPos = startCross + marginLeadCross + alignOffsetCross,
                mainSize = childMain,
                crossSize = childCross
            )

            currentMain += marginLeadMain + slotMain + marginTrailMain + effectiveGap
        }

        // ── Pass 3: Resolve ghost nodes (Anchor) ──────────────────────────────
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || !child.anchor.isEnabled) continue
            child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
        }
    }
}

/** Factory function tạo [FlexLayoutPolicy] theo hàng ngang (Row/HBox). */
fun RowPolicy(gap: Float = 0f): FlexLayoutPolicy = FlexLayoutPolicy(Orientation.HORIZONTAL, gap)

/** Factory function tạo [FlexLayoutPolicy] theo hàng dọc (Column/VBox). */
fun ColumnPolicy(gap: Float = 0f): FlexLayoutPolicy = FlexLayoutPolicy(Orientation.VERTICAL, gap)

