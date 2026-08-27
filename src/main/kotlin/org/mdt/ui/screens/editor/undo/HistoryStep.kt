package org.mdt.ui.screens.editor.undo

import org.mdt.core.ui.UINode
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.state.EditorDocumentState

/**
 * ## HistoryStep
 *
 * Direct in-memory Virtual Node mutation contract for instant, 0-GC undo/redo history.
 *
 * See: docs/architecture/architecture_en.md
 */
sealed interface HistoryStep {
    fun undo(state: EditorDocumentState)
    fun redo(state: EditorDocumentState)
}

/**
 * ## NodeTransformStep
 *
 * Records 2D transform mutations (X, Y, Width, Height) directly on a Virtual Node.
 */
data class NodeTransformStep(
    val targetId: String,
    val oldX: Float,
    val oldY: Float,
    val oldW: Float,
    val oldH: Float,
    val newX: Float,
    val newY: Float,
    val newW: Float,
    val newH: Float
) : HistoryStep {
    override fun undo(state: EditorDocumentState) {
        val node = state.findNodeById(targetId) ?: return
        node.anchorData.offsetLeft = oldX
        node.anchorData.offsetTop = oldY
        node.width = oldW
        node.height = oldH
        node.invalidateLayout()
        state.selectedNodeId = targetId
    }

    override fun redo(state: EditorDocumentState) {
        val node = state.findNodeById(targetId) ?: return
        node.anchorData.offsetLeft = newX
        node.anchorData.offsetTop = newY
        node.width = newW
        node.height = newH
        node.invalidateLayout()
        state.selectedNodeId = targetId
    }
}

/**
 * ## NodeHierarchyStep
 *
 * Records node reparenting or structural re-ordering across the Virtual Node tree.
 */
data class NodeHierarchyStep(
    val node: UINode,
    val oldParent: UINode,
    val oldIndex: Int,
    val newParent: UINode,
    val newIndex: Int
) : HistoryStep {
    override fun undo(state: EditorDocumentState) {
        oldParent.addChildAt(oldIndex, node)
        state.selectedNodeId = node.id
    }

    override fun redo(state: EditorDocumentState) {
        newParent.addChildAt(newIndex, node)
        state.selectedNodeId = node.id
    }
}

/**
 * ## NodeAddStep
 *
 * Records node insertion into the live Virtual DOM hierarchy.
 */
data class NodeAddStep(
    val node: UINode,
    val parent: UINode,
    val index: Int
) : HistoryStep {
    override fun undo(state: EditorDocumentState) {
        parent.removeChild(node)
        if (state.selectedNodeId == node.id) state.selectedNodeId = null
    }

    override fun redo(state: EditorDocumentState) {
        parent.addChildAt(index, node)
        state.selectedNodeId = node.id
    }
}

/**
 * ## NodeDeleteStep
 *
 * Records node deletion and restores it with full subtree state and index position.
 */
data class NodeDeleteStep(
    val node: UINode,
    val parent: UINode,
    val index: Int
) : HistoryStep {
    override fun undo(state: EditorDocumentState) {
        parent.addChildAt(index, node)
        state.selectedNodeId = node.id
    }

    override fun redo(state: EditorDocumentState) {
        parent.removeChild(node)
        if (state.selectedNodeId == node.id) state.selectedNodeId = null
    }
}

/**
 * ## TextContentStep
 *
 * Records text string changes on a [TextNode].
 */
data class TextContentStep(
    val targetId: String,
    val oldText: String,
    val newText: String
) : HistoryStep {
    override fun undo(state: EditorDocumentState) {
        (state.findNodeById(targetId) as? TextNode)?.let {
            it.text = oldText
            it.invalidateLayout()
        }
        state.selectedNodeId = targetId
    }

    override fun redo(state: EditorDocumentState) {
        (state.findNodeById(targetId) as? TextNode)?.let {
            it.text = newText
            it.invalidateLayout()
        }
        state.selectedNodeId = targetId
    }
}
