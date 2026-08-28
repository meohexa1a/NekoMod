package org.mdt.core.ui.compose

import androidx.compose.runtime.AbstractApplier
import org.mdt.core.ui.node.UINode

/**
 * ## NodeApplier [Compose Applier Bridge]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Direct custom [AbstractApplier] bridging the Jetpack Compose Runtime slot table to the [UINode] Virtual DOM tree.
 * - Handles top-down node insertions (`insertTopDown`), tree mutations (`move`, `remove`), and structural clearings (`onClear`).
 * - Dispatches node attach/detach lifecycle hooks automatically during tree tree alterations.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Top-Down Construction):** Node children must be inserted top-down so parent context is established before child layout calculation.
 * - **Rule 2 (Zero-GC Safe Index Clamping):** Removals and moves must respect current children list bounds.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Base Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/UINode.kt`
 * - 🔄 **Composition Host:** `src/main/kotlin/org/mdt/core/ui/compose/UIComposition.kt`
 * - ⚙️ **Runtime Orchestrator:** `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `insertTopDown` invokes `current.addChildAt(index, instance)`.
 * - [x] `remove` safely removes `count` nodes without index out-of-bounds errors.
 * - [x] `move` invokes `current.moveChild(from, to, count)`.
 * - [x] `onClear` invokes `current.clearChildren()`.
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
