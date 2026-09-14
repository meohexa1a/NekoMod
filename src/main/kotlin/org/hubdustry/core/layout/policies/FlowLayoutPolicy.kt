package org.hubdustry.core.layout.policies

import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.Orientation
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.computeOffset

/**
 * Chiến lược bố cục đa dòng tự động ngắt dòng/cột (Flow / Wrap Layout).
 * Tuân thủ nghiêm ngặt kỷ luật Zero-GC và Pure Math của NekoMod:
 * - 1D Axis Projection: Chiếu thống nhất theo [Orientation.HORIZONTAL] (FlowRow) và [Orientation.VERTICAL] (FlowColumn).
 * - Zero-GC Flat Scratchpads: Mảng phẳng nguyên thủy mở rộng bằng doubling, không cấp phát heap trong frame loop.
 * - Greedy Line-Breaking O(N): Ngắt dòng theo không gian khả dụng trên trục chính.
 * - Per-Line Cross Alignment: Căn chỉnh trục phụ độc lập theo từng dòng (START, CENTER, END).
 * - Gateway Sanitization: Bảo vệ tuyệt đối trước NaN, Infinity và khoảng cách âm.
 */
class FlowLayoutPolicy(
    val orientation: Orientation = Orientation.HORIZONTAL,
    val mainGap: Float = 0f,
    val crossGap: Float = 0f
) : LayoutPolicy {

    private val effectiveMainGap: Float
        get() = if (mainGap.isNaN() || mainGap < 0f) 0f else mainGap

    private val effectiveCrossGap: Float
        get() = if (crossGap.isNaN() || crossGap < 0f) 0f else crossGap

    override fun computeMinSize(node: LayoutNode) {
        var maxChildMinMain = 0f
        var maxChildMinCross = 0f
        val cross = orientation.cross()

        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue
            child.policy.computeMinSize(child)
            if (child.anchor.isEnabled) continue

            val childMain = child.minSize(orientation) + child.marginMain(orientation)
            if (childMain > maxChildMinMain) {
                maxChildMinMain = childMain
            }

            val childCross = child.minSize(cross) + child.marginCross(orientation)
            if (childCross > maxChildMinCross) {
                maxChildMinCross = childCross
            }
        }

        val padMain = node.paddingMain(orientation)
        val padCross = node.paddingCross(orientation)

        val isScrollMain = (orientation == Orientation.VERTICAL && node.isScrollableVertical) ||
                (orientation == Orientation.HORIZONTAL && node.isScrollableHorizontal)
        val isScrollCross = (cross == Orientation.VERTICAL && node.isScrollableVertical) ||
                (cross == Orientation.HORIZONTAL && node.isScrollableHorizontal)

        val calculatedMinMain = if (isScrollMain) padMain else maxChildMinMain + padMain
        val calculatedMinCross = if (isScrollCross) padCross else maxChildMinCross + padCross

        node.setMinSizeByAxis(
            orientation,
            maxOf(node.minSize(orientation), calculatedMinMain).coerceAtMost(node.maxSize(orientation)),
            maxOf(node.minSize(cross), calculatedMinCross).coerceAtMost(node.maxSize(cross))
        )
    }

    // ── Zero-GC Primitive Flat Scratchpads ──────────────────────────────────
    private var lineStart = IntArray(16)
    private var lineEnd = IntArray(16)
    private var lineCrossSize = FloatArray(16)
    private var lineMainSize = FloatArray(16)

    private fun ensureLineCapacity(needed: Int) {
        if (lineStart.size < needed) {
            val newCap = maxOf(needed, lineStart.size * 2)
            lineStart = IntArray(newCap)
            lineEnd = IntArray(newCap)
            lineCrossSize = FloatArray(newCap)
            lineMainSize = FloatArray(newCap)
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
        val safeAvailableMain = if (isUnconstrainedMain) Float.MAX_VALUE else rawAvailableMain

        val count = node.children.size
        ensureLineCapacity(maxOf(1, count))

        val mainGapVal = effectiveMainGap
        val crossGapVal = effectiveCrossGap

        // ── Pass 1: Greedy Line Breaking O(N) ─────────────────────────────────
        var totalLines = 0
        var currentLineStart = -1
        var currentLineEnd = -1
        var currentLineCount = 0
        var currentLineMain = 0f
        var currentLineCross = 0f

        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || child.anchor.isEnabled) continue

            val childMinMain = child.minSize(orientation)
            val childMarginMain = child.marginMain(orientation)
            val childTotalMain = childMinMain + childMarginMain

            val childMinCross = child.minSize(cross)
            val childMarginCross = child.marginCross(orientation)
            val childTotalCross = childMinCross + childMarginCross

            val itemGap = if (currentLineCount > 0) mainGapVal else 0f
            val wouldOverflow = currentLineCount > 0 && (currentLineMain + itemGap + childTotalMain > safeAvailableMain)

            if (wouldOverflow) {
                lineStart[totalLines] = currentLineStart
                lineEnd[totalLines] = currentLineEnd
                lineMainSize[totalLines] = currentLineMain
                lineCrossSize[totalLines] = currentLineCross
                totalLines++
                currentLineCount = 0
            }

            if (currentLineCount == 0) {
                currentLineStart = i
                currentLineEnd = i
                currentLineCount = 1
                currentLineMain = childTotalMain
                currentLineCross = childTotalCross
            } else {
                currentLineEnd = i
                currentLineCount++
                currentLineMain += itemGap + childTotalMain
                currentLineCross = maxOf(currentLineCross, childTotalCross)
            }
        }

        // Chốt dòng cuối cùng nếu còn phần tử
        if (currentLineCount > 0) {
            lineStart[totalLines] = currentLineStart
            lineEnd[totalLines] = currentLineEnd
            lineMainSize[totalLines] = currentLineMain
            lineCrossSize[totalLines] = currentLineCross
            totalLines++
        }

        // Cập nhật tổng content bounds cho ScrollState
        var maxObservedMain = 0f
        var totalObservedCross = 0f
        for (l in 0 until totalLines) {
            maxObservedMain = maxOf(maxObservedMain, lineMainSize[l])
            totalObservedCross += lineCrossSize[l]
        }
        if (totalLines > 1) {
            totalObservedCross += (totalLines - 1) * crossGapVal
        }
        node.setContentSize(orientation, main = maxObservedMain, cross = totalObservedCross)

        // ── Pass 2: Position children along lines ─────────────────────────────
        var currentCrossPos = startCross

        for (l in 0 until totalLines) {
            val lStart = lineStart[l]
            val lEnd = lineEnd[l]
            val lCross = lineCrossSize[l]
            var currentMainPos = startMain

            for (i in lStart..lEnd) {
                val child = node.children[i]
                if (!child.visible || child.anchor.isEnabled) continue

                val marginLeadMain = child.marginLeading(orientation)
                val marginTrailMain = child.marginTrailing(orientation)
                val marginLeadCross = child.marginCrossLeading(orientation)
                val marginCrossTotal = child.marginCross(orientation)

                val childMinMain = child.minSize(orientation)
                val childMaxMain = child.maxSize(orientation)
                val childMain = childMinMain.coerceIn(childMinMain, childMaxMain)

                val childMinCross = child.minSize(cross)
                val childMaxCross = child.maxSize(cross)
                val availCrossInLine = maxOf(0f, lCross - marginCrossTotal)

                val childCross = when (child.sizeFlag(cross)) {
                    SizeFlag.FILL -> availCrossInLine
                    SizeFlag.SHRINK -> childMinCross
                }.coerceIn(childMinCross, childMaxCross)

                val alignOffsetCross = child.alignment(cross).computeOffset(availCrossInLine, childCross)

                child.arrangeAxis(
                    orientation,
                    mainPos = currentMainPos + marginLeadMain,
                    crossPos = currentCrossPos + marginLeadCross + alignOffsetCross,
                    mainSize = childMain,
                    crossSize = childCross
                )

                currentMainPos += marginLeadMain + childMain + marginTrailMain + mainGapVal
            }

            currentCrossPos += lCross + crossGapVal
        }

        // ── Pass 3: Resolve ghost nodes (Anchor) ──────────────────────────────
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || !child.anchor.isEnabled) continue
            child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
        }
    }
}

/** Factory function tạo [FlowLayoutPolicy] theo hàng ngang ngắt dòng (FlowRow/HFlow). */
fun FlowRowPolicy(horizontalGap: Float = 0f, verticalGap: Float = 0f): FlowLayoutPolicy =
    FlowLayoutPolicy(Orientation.HORIZONTAL, mainGap = horizontalGap, crossGap = verticalGap)

/** Factory function tạo [FlowLayoutPolicy] theo hàng dọc ngắt cột (FlowColumn/VFlow). */
fun FlowColumnPolicy(verticalGap: Float = 0f, horizontalGap: Float = 0f): FlowLayoutPolicy =
    FlowLayoutPolicy(Orientation.VERTICAL, mainGap = verticalGap, crossGap = horizontalGap)
