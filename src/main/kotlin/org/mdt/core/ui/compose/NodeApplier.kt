// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.compose

import androidx.compose.runtime.AbstractApplier
import org.mdt.core.ui.node.UINode

/**
 * ## NodeApplier
 *
 * Connects the Compose runtime tree operations directly to the [UINode] tree.
 * Handles inserting, moving, removing, and clearing virtual nodes during recomposition.
 *
 * @see UINode
 * @see UIComposition
 * @see org.mdt.core.ui.EngineRuntime
 */
class NodeApplier(root: UINode) : AbstractApplier<UINode>(root) {

    override fun insertTopDown(index: Int, instance: UINode) = current.addChildAt(index, instance)

    override fun insertBottomUp(index: Int, instance: UINode) {}

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            if (index < current.children.size) {
                current.removeChildAt(index)
            }
        }
    }

    override fun move(from: Int, to: Int, count: Int) = current.moveChild(from, to, count)

    override fun onClear() = current.clearChildren()
}
