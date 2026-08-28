package org.mdt.core.ui.compose

import androidx.compose.runtime.AbstractApplier
import org.mdt.core.ui.node.UINode

/**
 * ## NodeApplier
 *
 * Custom Compose Applier connecting the Jetpack Compose Runtime slot table
 * directly to the [UINode] Virtual DOM tree.
 *
 * See: docs/compose-dsl/compose_dsl_en.md
 */
class NodeApplier(root: UINode) : AbstractApplier<UINode>(root) {

    override fun insertTopDown(index: Int, instance: UINode) = current.addChildAt(index, instance)

    override fun insertBottomUp(index: Int, instance: UINode) {}

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            if (index < current.children.size) current.removeChildAt(index)
        }
    }

    override fun move(from: Int, to: Int, count: Int) = current.moveChild(from, to, count)

    override fun onClear() = current.clearChildren()
}
