package org.hubdustry.core.layout.policies

import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.Orientation
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.computeOffset

// ─────────────────────────────────────────────────────────────────────────────
// 1. FACTORY POLICIES FOR FLOW CONTAINERS (FLOW-ROW & FLOW-COLUMN)
// ─────────────────────────────────────────────────────────────────────────────

/** Factory function tạo [FlowLayoutPolicy] theo hàng ngang ngắt dòng (FlowRow/HFlow). */
fun FlowRowPolicy(horizontalGap: Float = 0f, verticalGap: Float = 0f): FlowLayoutPolicy =
    FlowLayoutPolicy(Orientation.HORIZONTAL, mainGap = horizontalGap, crossGap = verticalGap)

/** Factory function tạo [FlowLayoutPolicy] theo hàng dọc ngắt cột (FlowColumn/VFlow). */
fun FlowColumnPolicy(verticalGap: Float = 0f, horizontalGap: Float = 0f): FlowLayoutPolicy =
    FlowLayoutPolicy(Orientation.VERTICAL, mainGap = verticalGap, crossGap = horizontalGap)

// ─────────────────────────────────────────────────────────────────────────────
// 2. FLOW LAYOUT POLICY & ALGORITHMS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Chiến lược bố cục đa dòng tự động ngắt dòng/cột (Flow / Wrap Layout).
 * Tuân thủ nghiêm ngặt kỷ luật Zero-GC và Pure Math của NekoMod:
 * - 1D Axis Projection: Chiếu thống nhất theo [Orientation.HORIZONTAL] (FlowRow) và [Orientation.VERTICAL] (FlowColumn).
 * - Zero-GC Flat Scratchpads: Mảng phẳng nguyên thủy mở rộng bằng doubling, không cấp phát heap trong frame loop.
 * - Greedy Line-Breaking O(N): Ngắt dòng theo không gian khả dụng trên trục chính.
 * - Per-Line Cross Alignment: Căn chỉnh trục phụ độc lập theo từng dòng (START, CENTER, END).
 * - Gateway Sanitization: Bảo vệ tuyệt đối trước NaN, Infinity và khoảng cách âm.
 *
 * ```
 *  Multi-Pass Flow Wrap Architecture:
 *
 *  [safeAvailableMain] ────────────────────────────────────────────────┐
 *  │ Line 0:  [Child 1] ──mainGap── [Child 2] ──mainGap── [Child 3]    │ (Fits)
 *  ├───────────────────────────────────────────────────────────────────┤
 *  │ Line 1:  [Child 4 (Oversized or Next Item)] ─────────────┐        │ (Wrapped)
 *  │          │ Cross Alignment: START / CENTER / END         │        │
 *  │          └───────────────────────────────────────────────┘        │
 *  └───────────────────────────────────────────────────────────────────┘
 *                           │
 *                           ▼
 *             advance by (lineCrossExtent + crossGap)
 * ```
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

    // ─────────────────────────────────────────────────────────────────────────
    // 3. INTRINSIC MEASUREMENT & CONSTRAINTS GATEWAY
    // ─────────────────────────────────────────────────────────────────────────

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
            maxChildMinMain = maxOf(maxChildMinMain, childMain)

            val childCross = child.minSize(cross) + child.marginCross(orientation)
            maxChildMinCross = maxOf(maxChildMinCross, childCross)
        }

        val paddingMain = node.paddingMain(orientation)
        val paddingCross = node.paddingCross(orientation)

        val isScrollMain = (orientation == Orientation.VERTICAL && node.isScrollableVertical) ||
                (orientation == Orientation.HORIZONTAL && node.isScrollableHorizontal)
        val isScrollCross = (cross == Orientation.VERTICAL && node.isScrollableVertical) ||
                (cross == Orientation.HORIZONTAL && node.isScrollableHorizontal)

        val calculatedMinMain = if (isScrollMain) paddingMain else maxChildMinMain + paddingMain
        val calculatedMinCross = if (isScrollCross) paddingCross else maxChildMinCross + paddingCross

        node.setMinSizeByAxis(
            orientation,
            maxOf(node.minSize(orientation), calculatedMinMain).coerceAtMost(node.maxSize(orientation)),
            maxOf(node.minSize(cross), calculatedMinCross).coerceAtMost(node.maxSize(cross))
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ZERO-GC PRIMITIVE FLAT SCRATCHPADS & STORAGE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    private var lineStart = IntArray(16)
    private var lineEnd = IntArray(16)
    private var lineCrossSize = FloatArray(16)
    private var lineMainSize = FloatArray(16)

    private fun ensureLineCapacity(needed: Int) {
        if (lineStart.size < needed) {
            val newCapacity = maxOf(needed, lineStart.size * 2)
            lineStart = IntArray(newCapacity)
            lineEnd = IntArray(newCapacity)
            lineCrossSize = FloatArray(newCapacity)
            lineMainSize = FloatArray(newCapacity)
        }
    }

    private inline fun recordCommittedLine(
        startIndex: Int,
        endIndex: Int,
        mainSpan: Float,
        crossExtent: Float,
        totalLines: Int
    ): Int {
        lineStart[totalLines] = startIndex
        lineEnd[totalLines] = endIndex
        lineMainSize[totalLines] = mainSpan
        lineCrossSize[totalLines] = crossExtent
        return totalLines + 1
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. MULTI-PASS ARRANGE ORCHESTRATION & EXECUTION
    // ─────────────────────────────────────────────────────────────────────────

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val cross = orientation.cross()
        val rawAvailableMain = orientation.main(innerWidth, innerHeight)
        val originMainPosition = orientation.main(innerX, innerY)
        val originCrossPosition = orientation.cross(innerX, innerY)

        val isUnconstrainedMain = rawAvailableMain.isNaN() || rawAvailableMain == Float.MAX_VALUE || rawAvailableMain.isInfinite() || rawAvailableMain <= 0f
        val safeAvailableMain = if (isUnconstrainedMain) Float.MAX_VALUE else rawAvailableMain

        val count = node.children.size
        ensureLineCapacity(maxOf(1, count))

        val effectiveMainGapValue = effectiveMainGap
        val effectiveCrossGapValue = effectiveCrossGap

        // Pass 1: Greedy Line Breaking O(N)
        val totalLines = breakLinesGreedy(node, cross, safeAvailableMain, effectiveMainGapValue)

        // Pass 2: Cập nhật tổng content bounds cho ScrollState
        updateContentBounds(node, totalLines, effectiveCrossGapValue)

        // Pass 3: Position children along lines
        positionChildrenInLines(node, cross, totalLines, originMainPosition, originCrossPosition, effectiveMainGapValue, effectiveCrossGapValue)

        // Pass 4: Resolve ghost nodes (Anchor)
        resolveAnchoredChildren(node, innerX, innerY, innerWidth, innerHeight)
    }

    private fun breakLinesGreedy(
        node: LayoutNode,
        cross: Orientation,
        safeAvailableMain: Float,
        mainGapValue: Float
    ): Int {
        var totalLines = 0
        var currentLineStart = -1
        var currentLineEnd = -1
        var currentLineCount = 0
        var currentLineMainSpan = 0f
        var currentLineCrossExtent = 0f

        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || child.anchor.isEnabled) continue

            val childMinMainSize = child.minSize(orientation)
            val childMarginMain = child.marginMain(orientation)
            val childMainSpan = childMinMainSize + childMarginMain

            val childMinCrossSize = child.minSize(cross)
            val childMarginCross = child.marginCross(orientation)
            val childCrossSpan = childMinCrossSize + childMarginCross

            val itemGap = if (currentLineCount > 0) mainGapValue else 0f
            val wouldOverflow = currentLineCount > 0 && (currentLineMainSpan + itemGap + childMainSpan > safeAvailableMain)

            if (wouldOverflow) {
                totalLines = recordCommittedLine(
                    currentLineStart,
                    currentLineEnd,
                    currentLineMainSpan,
                    currentLineCrossExtent,
                    totalLines
                )
                currentLineCount = 0
            }

            if (currentLineCount == 0) {
                currentLineStart = i
                currentLineEnd = i
                currentLineCount = 1
                currentLineMainSpan = childMainSpan
                currentLineCrossExtent = childCrossSpan
            } else {
                currentLineEnd = i
                currentLineCount++
                currentLineMainSpan += itemGap + childMainSpan
                currentLineCrossExtent = maxOf(currentLineCrossExtent, childCrossSpan)
            }
        }

        if (currentLineCount > 0) {
            totalLines = recordCommittedLine(
                currentLineStart,
                currentLineEnd,
                currentLineMainSpan,
                currentLineCrossExtent,
                totalLines
            )
        }

        return totalLines
    }

    private fun updateContentBounds(
        node: LayoutNode,
        totalLines: Int,
        crossGapValue: Float
    ) {
        var maxObservedMain = 0f
        var totalObservedCross = 0f
        for (lineIndex in 0 until totalLines) {
            maxObservedMain = maxOf(maxObservedMain, lineMainSize[lineIndex])
            totalObservedCross += lineCrossSize[lineIndex]
        }
        if (totalLines > 1) {
            totalObservedCross += (totalLines - 1) * crossGapValue
        }
        node.setContentSize(orientation, main = maxObservedMain, cross = totalObservedCross)
    }

    private fun positionChildrenInLines(
        node: LayoutNode,
        cross: Orientation,
        totalLines: Int,
        originMainPosition: Float,
        originCrossPosition: Float,
        mainGapValue: Float,
        crossGapValue: Float
    ) {
        var currentCrossPosition = originCrossPosition

        for (lineIndex in 0 until totalLines) {
            val lineStartIndex = lineStart[lineIndex]
            val lineEndIndex = lineEnd[lineIndex]
            val lineCrossExtent = lineCrossSize[lineIndex]
            var currentMainPosition = originMainPosition

            for (i in lineStartIndex..lineEndIndex) {
                val child = node.children[i]
                if (!child.visible || child.anchor.isEnabled) continue

                val childLeadingMarginMain = child.marginLeading(orientation)
                val childTrailingMarginMain = child.marginTrailing(orientation)
                val childLeadingMarginCross = child.marginCrossLeading(orientation)
                val childCrossMarginTotal = child.marginCross(orientation)

                val childMinMainSize = child.minSize(orientation)
                val childMaxMainSize = child.maxSize(orientation)
                val childMainSize = childMinMainSize.coerceAtMost(childMaxMainSize)

                val childMinCrossSize = child.minSize(cross)
                val childMaxCrossSize = child.maxSize(cross)
                val availableCrossLineSpace = maxOf(0f, lineCrossExtent - childCrossMarginTotal)

                val childCrossSize = when (child.sizeFlag(cross)) {
                    SizeFlag.FILL -> availableCrossLineSpace
                    SizeFlag.SHRINK -> childMinCrossSize
                }.coerceIn(childMinCrossSize, childMaxCrossSize)

                val crossAlignmentOffset = child.alignment(cross).computeOffset(availableCrossLineSpace, childCrossSize)

                child.arrangeAxis(
                    orientation,
                    mainPos = currentMainPosition + childLeadingMarginMain,
                    crossPos = currentCrossPosition + childLeadingMarginCross + crossAlignmentOffset,
                    mainSize = childMainSize,
                    crossSize = childCrossSize
                )

                currentMainPosition += childLeadingMarginMain + childMainSize + childTrailingMarginMain + mainGapValue
            }

            currentCrossPosition += lineCrossExtent + crossGapValue
        }
    }

    private fun resolveAnchoredChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible || !child.anchor.isEnabled) continue
            child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
        }
    }
}
