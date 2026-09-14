package org.hubdustry.core.compose.runtime

import androidx.compose.runtime.AbstractApplier
import org.hubdustry.core.layout.LayoutNode

/**
 * [LayoutNodeApplier] — Cầu nối thực thi giữa Compose Runtime và cây [LayoutNode].
 *
 * TẠI SAO CẦN FILE NÀY?
 * Compose Runtime là bộ não trừu tượng chỉ ra lệnh: "Thêm node ở vị trí X, xóa node ở vị trí Y".
 * File này trực tiếp gọi các lệnh đó trên cây [LayoutNode] của NekoMod.
 */
class LayoutNodeApplier(
    root: LayoutNode,
    private val onNodeRemovedCallback: ((LayoutNode) -> Unit)? = null,
    private val onEndChangesCallback: () -> Unit = {}
) : AbstractApplier<LayoutNode>(root) {

    /** Báo cho ComposeView biết cây đã đổi để tính lại kích thước bố cục. */
    override fun onEndChanges() {
        super.onEndChanges()
        onEndChangesCallback()
    }

    override fun insertTopDown(index: Int, instance: LayoutNode) {
        current.addChild(instance, index)
    }

    override fun insertBottomUp(index: Int, instance: LayoutNode) {
        // No-op: NekoMod dựng cây từ trên xuống (Top-Down)
    }

    override fun remove(index: Int, count: Int) {
        if (onNodeRemovedCallback != null) {
            val children = current.children
            val safeStart = index.coerceIn(0, children.size)
            val safeCount = count.coerceIn(0, children.size - safeStart)
            for (i in safeStart until (safeStart + safeCount)) {
                children[i].forEachInSubtree(onNodeRemovedCallback)
            }
        }
        current.removeChildren(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.moveChildren(from, to, count)
    }

    override fun onClear() {
        if (onNodeRemovedCallback != null) {
            val children = root.children
            val count = children.size
            for (i in 0 until count) {
                children[i].forEachInSubtree(onNodeRemovedCallback)
            }
        }
        root.clearChildren()
    }
}
