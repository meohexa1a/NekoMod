package org.hubdustry.core.compose.view

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerType
import org.hubdustry.core.layout.LayoutNode

/**
 * Ghi lại thông tin node trúng hit-test và tọa độ tuyệt đối của node trong ComposeView.
 */
internal data class LayoutNodeHit(
    var node: LayoutNode,
    var absX: Float,
    var absY: Float
)

/**
 * Bản ghi con trỏ đang được theo dõi trong suốt vòng đời nhấn giữ (Touch Tracking).
 */
internal class PointerHitRecord(
    val chain: List<LayoutNodeHit>,
    var lastComposeX: Float,
    var lastComposeY: Float,
    var lastUptime: Long,
    val button: PointerButton? = PointerButton.Primary,
    val pointerType: PointerType = PointerType.Touch
)

/**
 * [HitTestManager] — Quản lý thuật toán Hit-Testing và bộ đệm con trỏ/hover cho cây Virtual DOM.
 *
 * TRÁCH NHIỆM (Phân Tách Module Đợt 3):
 * 1. Duyệt cây Virtual DOM ([LayoutNode]) tìm chuỗi node nhận tương tác, tôn trọng vùng cắt gọt (scissor clip) và bo góc SDF.
 * 2. Quản lý các bộ đệm tái sử dụng Zero-GC ([hitPool], [hoverPool], [hitScratch], [previousHoverChain]).
 * 3. Theo dõi chuỗi hover và phát hiện các node đã rời khỏi tầm chuột (Hover Exit Detection).
 */
internal class HitTestManager(private val rootLayoutNode: LayoutNode) {

    private val hitScratch = ArrayList<LayoutNodeHit>()
    private val hitPool = ArrayList<LayoutNodeHit>()
    private var hitPoolIndex = 0

    private val previousHoverChain = ArrayList<LayoutNodeHit>()
    private val exitedHoverScratch = ArrayList<LayoutNodeHit>()
    private val hoverPool = ArrayList<LayoutNodeHit>()

    /**
     * Lấy hoặc tái sử dụng một [LayoutNodeHit] từ object pool.
     */
    private fun obtainHit(node: LayoutNode, absX: Float, absY: Float): LayoutNodeHit {
        val hit = if (hitPoolIndex < hitPool.size) {
            val existing = hitPool[hitPoolIndex]
            existing.node = node
            existing.absX = absX
            existing.absY = absY
            existing
        } else {
            val newHit = LayoutNodeHit(node, absX, absY)
            hitPool.add(newHit)
            newHit
        }
        hitPoolIndex++
        return hit
    }

    /**
     * Tìm chuỗi các node nhận tương tác tại tọa độ ([targetX], [targetY]).
     * Kết quả trả về danh sách [LayoutNodeHit] từ gốc xuống lá (Root -> Leaf, phù hợp với pha Tunneling; tái sử dụng [hitScratch], Zero-GC).
     */
    fun findHitChain(targetX: Float, targetY: Float): List<LayoutNodeHit> {
        hitPoolIndex = 0
        hitScratch.clear()
        hitTestChain(
            node = rootLayoutNode,
            parentAbsX = 0f,
            parentAbsY = 0f,
            targetX = targetX,
            targetY = targetY,
            result = hitScratch
        )
        return hitScratch
    }

    private fun hitTestChain(
        node: LayoutNode,
        parentAbsX: Float,
        parentAbsY: Float,
        clipMinX: Float = -100000f,
        clipMinY: Float = -100000f,
        clipMaxX: Float = 100000f,
        clipMaxY: Float = 100000f,
        targetX: Float,
        targetY: Float,
        result: MutableList<LayoutNodeHit>
    ): Boolean {
        if (!node.visible) return false

        // 1. Kiểm tra điểm tương tác có nằm trong vùng cắt gọt thừa kế từ tổ tiên không
        if (targetX < clipMinX || targetX > clipMaxX || targetY < clipMinY || targetY > clipMaxY) {
            return false
        }

        val absX = parentAbsX + node.x + node.offsetX
        val absY = parentAbsY + node.y + node.offsetY
        val w = node.width
        val h = node.height

        // 2. Tọa độ cục bộ trong node
        val localX = targetX - absX
        val localY = targetY - absY

        // 3. Nếu node có clip nhưng điểm chạm rơi ra ngoài AABB của trục được clip -> reject
        if ((node.clipHorizontal && (localX < 0f || localX > w)) ||
            (node.clipVertical && (localY < 0f || localY > h))) {
            return false
        }

        // 4. Kiểm tra điểm có nằm trong hình học thực tế của node (AABB & bo góc) qua Pure Math của chính Node
        val isInShape = node.containsPoint(localX, localY)

        // Nếu node có clip bo góc và điểm chạm rơi vào góc bị xén -> reject
        if (node.hasClipCorners && !isInShape) {
            return false
        }

        val initialSize = result.size

        // Node nhận hit nếu điểm nằm trong hình dạng thực tế và có bộ lọc cử chỉ
        if (isInShape && node.pointerInputFilters.isNotEmpty()) {
            result.add(obtainHit(node, absX, absY))
        }

        // 5. Tính toán vùng clip lũy tiến cho các node con
        val nextClipMinX = if (node.clipHorizontal) maxOf(clipMinX, absX) else clipMinX
        val nextClipMaxX = if (node.clipHorizontal) minOf(clipMaxX, absX + w) else clipMaxX
        val nextClipMinY = if (node.clipVertical) maxOf(clipMinY, absY) else clipMinY
        val nextClipMaxY = if (node.clipVertical) minOf(clipMaxY, absY + h) else clipMaxY

        val childParentAbsX = absX - node.scrollX
        val childParentAbsY = absY - node.scrollY

        val children = node.children
        val count = children.size
        for (i in count - 1 downTo 0) {
            if (hitTestChain(
                    node = children[i],
                    parentAbsX = childParentAbsX,
                    parentAbsY = childParentAbsY,
                    clipMinX = nextClipMinX,
                    clipMinY = nextClipMinY,
                    clipMaxX = nextClipMaxX,
                    clipMaxY = nextClipMaxY,
                    targetX = targetX,
                    targetY = targetY,
                    result = result
                )
            ) {
                break
            }
        }

        return result.size > initialSize
    }

    /**
     * Xác định danh sách các node vừa rời khỏi tầm chuột (Exited) dựa trên [currentHits].
     */
    fun findExitedHoverNodes(currentHits: List<LayoutNodeHit>): List<LayoutNodeHit> {
        exitedHoverScratch.clear()
        previousHoverChain.fastForEach { prevHit ->
            if (!currentHits.fastAny { it.node === prevHit.node }) {
                exitedHoverScratch.add(prevHit)
            }
        }
        return exitedHoverScratch
    }

    /**
     * Cập nhật chuỗi hover trước đó ([previousHoverChain]) từ [currentHits] mà không tạo rác (Zero-GC).
     */
    fun updatePreviousHoverChain(currentHits: List<LayoutNodeHit>) {
        previousHoverChain.clear()
        currentHits.fastForEachIndexed { i, src ->
            val hit = if (i < hoverPool.size) {
                val existing = hoverPool[i]
                existing.node = src.node
                existing.absX = src.absX
                existing.absY = src.absY
                existing
            } else {
                val newHit = LayoutNodeHit(src.node, src.absX, src.absY)
                hoverPool.add(newHit)
                newHit
            }
            previousHoverChain.add(hit)
        }
    }

    /**
     * Lấy toàn bộ chuỗi hover hiện tại khi chuột rời View và dọn dẹp sạch chuỗi lưu trữ.
     */
    fun takePreviousHoverChain(): List<LayoutNodeHit> {
        if (previousHoverChain.isEmpty()) return emptyList()
        val copy = ArrayList(previousHoverChain)
        previousHoverChain.clear()
        return copy
    }

    /**
     * Thông báo khi một node bị tháo khỏi Virtual Tree để gỡ bỏ khỏi hover tracking.
     */
    fun onNodeRemoved(node: LayoutNode) {
        previousHoverChain.removeAll { it.node === node }
    }

    /**
     * Giải phóng và dọn dẹp sạch toàn bộ pools và scratchpads.
     */
    fun dispose() {
        hitScratch.clear()
        hitPool.clear()
        hitPoolIndex = 0

        previousHoverChain.clear()
        exitedHoverScratch.clear()
        hoverPool.clear()
    }
}
