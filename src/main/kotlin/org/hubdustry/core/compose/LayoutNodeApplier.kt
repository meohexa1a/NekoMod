package org.hubdustry.core.compose

import androidx.compose.runtime.AbstractApplier
import arc.util.Log
import org.hubdustry.core.layout.LayoutNode

/**
 * Applier cho Jetpack Compose Runtime ánh xạ trực tiếp lên cây Virtual [LayoutNode].
 *
 * BẢO TỒN NGUYÊN TẮC V1/V2:
 * 1. Sử dụng [insertTopDown], bỏ qua [insertBottomUp].
 * 2. NEVER-THROW: Toàn bộ phương thức được bọc an toàn để không bao giờ quăng ngoại lệ
 *    làm sập vòng lặp [androidx.compose.runtime.Recomposer].
 */
class LayoutNodeApplier(
    root: LayoutNode,
    private val onNodeRemovedCallback: ((LayoutNode) -> Unit)? = null,
    private val onEndChangesCallback: () -> Unit = {}
) : AbstractApplier<LayoutNode>(root) {

    override fun onEndChanges() {
        super.onEndChanges()
        try {
            onEndChangesCallback()
        } catch (t: Throwable) {
            Log.err("[LayoutNodeApplier] Error in onEndChanges", t)
        }
    }

    override fun insertTopDown(index: Int, instance: LayoutNode) {
        try {
            current.addChild(instance, index)
        } catch (t: Throwable) {
            Log.err("[LayoutNodeApplier] Error in insertTopDown at index $index", t)
        }
    }

    override fun insertBottomUp(index: Int, instance: LayoutNode) {
        // No-op theo thiết kế được bảo tồn từ v1/v2
    }

    override fun remove(index: Int, count: Int) {
        try {
            if (onNodeRemovedCallback != null) {
                val children = current.children
                val safeIndex = index.coerceIn(0, children.size)
                val safeCount = count.coerceIn(0, children.size - safeIndex)
                for (i in safeIndex until (safeIndex + safeCount)) {
                    children[i].forEachInSubtree { node ->
                        onNodeRemovedCallback.invoke(node)
                    }
                }
            }
            current.removeChildren(index, count)
        } catch (t: Throwable) {
            Log.err("[LayoutNodeApplier] Error in remove at index $index, count $count", t)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        try {
            current.moveChildren(from, to, count)
        } catch (t: Throwable) {
            Log.err("[LayoutNodeApplier] Error in move from $from to $to, count $count", t)
        }
    }

    public override fun onClear() {
        try {
            if (onNodeRemovedCallback != null) {
                val children = root.children
                val count = children.size
                for (i in 0 until count) {
                    children[i].forEachInSubtree { node ->
                        onNodeRemovedCallback.invoke(node)
                    }
                }
            }
            root.clearChildren()
        } catch (t: Throwable) {
            Log.err("[LayoutNodeApplier] Error in onClear", t)
        }
    }
}
