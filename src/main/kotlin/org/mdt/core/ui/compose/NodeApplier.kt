package org.mdt.core.ui.compose

import androidx.compose.runtime.AbstractApplier
import org.mdt.core.ui.node.UINode

/**
 * ## NodeApplier [Compose Applier Bridge]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Connects the Compose runtime tree operations directly to the [UINode] tree.
 * - Handles inserting, moving, removing, and clearing virtual nodes during recomposition.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Nodes must be inserted top-down so parent references exist before children layout.
 * - [x] `remove` safely checks bounds to prevent index out-of-bounds errors.
 * - [x] `onClear` resets and clears all children from the root node.
 *
 * ### 3. Related Files
 * - Base Virtual Node: `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 * - Composition Host: `src/main/kotlin/org/mdt/core/ui/compose/UIComposition.kt`
 * - Runtime Orchestrator: `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
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
