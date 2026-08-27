package org.mdt.ui.screens.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mdt.core.ui.UINode
import org.mdt.ui.components.layout.ComponentNode
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.screens.editor.model.TreeItemData
import org.mdt.ui.screens.editor.undo.*

/**
 * ## EditorDocumentState
 *
 * Single Source of Truth (SSOT) managing the live In-Memory Virtual Node Scene Graph,
 * active selection, layer hierarchy generation, and direct [UndoRedoManager] history stack.
 *
 * See: docs/architecture/architecture_en.md
 */
class EditorDocumentState(
    initialScene: SceneNode? = null
) {
    /** Live In-Memory Artboard Scene Root Node. */
    var rootScene: SceneNode by mutableStateOf(initialScene ?: SceneGraphFactory.createDefaultScene())

    /** List of live Master Component Artboards on the Canvas. */
    val masterComponents = mutableListOf<ComponentNode>()

    /** Currently selected Virtual Node ID in the scene graph. */
    var selectedNodeId: String? by mutableStateOf(null)

    /** Set of expanded container node IDs in the layer tree. */
    val expandedNodeIds: MutableSet<String> = mutableSetOf("scene_root")

    /** In-Memory Undo/Redo Manager. */
    val undoRedoManager = UndoRedoManager()

    init {
        expandAll(rootScene)
    }

    /**
     * Recursively expands all container nodes across the tree hierarchy.
     */
    fun expandAll(node: UINode = rootScene) {
        if (node.id.isNotEmpty()) expandedNodeIds.add(node.id)
        for (child in node.children) {
            expandAll(child)
        }
    }

    /**
     * Currently active Virtual [UINode] matching [selectedNodeId].
     */
    val selectedNode: UINode?
        get() {
            val id = selectedNodeId ?: return null
            return findNodeById(id)
        }

    /**
     * Finds a node by ID anywhere across the Scene and Master Component graphs.
     */
    fun findNodeById(id: String): UINode? {
        val inScene = rootScene.findNodeById(id)
        if (inScene != null) return inScene
        for (master in masterComponents) {
            val inMaster = master.findNodeById(id)
            if (inMaster != null) return inMaster
        }
        return null
    }

    // =========================================================================
    // I. Direct Virtual Node Hierarchy Mutations (0-GC, 60 FPS)
    // =========================================================================

    /**
     * Appends a child node under a parent container.
     */
    fun addNode(
        parent: LayoutNode = rootScene,
        node: UINode,
        recordHistory: Boolean = true
    ) {
        if (node.id.isEmpty()) {
            node.id = "${node.javaClass.simpleName.lowercase()}_${System.currentTimeMillis() % 100000}"
        }
        val index = parent.children.size
        parent.addChild(node)
        if (parent.id.isNotEmpty()) expandedNodeIds.add(parent.id)
        selectedNodeId = node.id

        if (recordHistory) {
            undoRedoManager.record(NodeAddStep(node, parent, index))
        }
        hierarchyVersion++
    }

    /**
     * Removes a node from its parent in the Virtual Node tree.
     */
    fun removeNode(node: UINode, recordHistory: Boolean = true): Boolean {
        if (node === rootScene) return false // Do not remove root scene
        val parent = node.parent ?: return false
        val index = parent.children.indexOf(node)

        val removed = parent.removeChild(node)
        if (removed) {
            if (selectedNodeId == node.id) {
                selectedNodeId = null
            }
            if (recordHistory && index >= 0) {
                undoRedoManager.record(NodeDeleteStep(node, parent, index))
            }
            hierarchyVersion++
        }
        return removed
    }

    /**
     * Deletes the currently selected virtual node.
     */
    fun removeSelectedNode(): Boolean {
        val node = selectedNode ?: return false
        return removeNode(node)
    }

    /**
     * Reparents a node to a new parent container with optional index.
     */
    fun reparentNode(
        node: UINode,
        newParent: LayoutNode,
        newIndex: Int = -1,
        recordHistory: Boolean = true
    ) {
        val oldParent = node.parent ?: return
        val oldIndex = oldParent.children.indexOf(node)
        val targetIndex = if (newIndex >= 0) newIndex else newParent.children.size

        newParent.addChildAt(targetIndex, node)
        selectedNodeId = node.id

        if (recordHistory) {
            undoRedoManager.record(
                NodeHierarchyStep(node, oldParent, oldIndex, newParent, targetIndex)
            )
        }
        hierarchyVersion++
    }

    /**
     * Performs an Undo operation.
     */
    fun undo(): Boolean {
        val success = undoRedoManager.undo(this)
        if (success) hierarchyVersion++
        return success
    }

    /**
     * Performs a Redo operation.
     */
    fun redo(): Boolean {
        val success = undoRedoManager.redo(this)
        if (success) hierarchyVersion++
        return success
    }

    // =========================================================================
    // II. Live Hierarchy Generation (Cached 0-GC Layer Tree)
    // =========================================================================

    /** Monotonically increasing version counter for tree structure mutations. */
    var hierarchyVersion by mutableStateOf(0)
        private set

    private var cachedHierarchyVersion = -1
    private var cachedLayerTree: List<TreeItemData> = emptyList()

    /** Explicitly marks the hierarchy cache as dirty. */
    fun invalidateHierarchy() {
        hierarchyVersion++
    }

    /**
     * Builds or returns the cached hierarchy tree directly from [rootScene] and [masterComponents].
     */
    fun buildLayerTree(): List<TreeItemData> {
        if (cachedHierarchyVersion == hierarchyVersion) {
            return cachedLayerTree
        }
        val result = mutableListOf<TreeItemData>()
        buildNodeLayerTree(rootScene, result, 0)
        for (master in masterComponents) {
            buildNodeLayerTree(master, result, 0)
        }
        cachedLayerTree = result
        cachedHierarchyVersion = hierarchyVersion
        return result
    }

    private fun buildNodeLayerTree(node: UINode, outList: MutableList<TreeItemData>, depth: Int) {
        val id = node.id
        val displayName = SceneGraphFactory.resolveSemanticName(node)
        val isExpandable = node.children.isNotEmpty()
        val isExpanded = expandedNodeIds.contains(id) || depth == 0

        outList.add(
            TreeItemData(
                id = id,
                name = displayName,
                type = node.javaClass.simpleName,
                iconUrl = SceneGraphFactory.resolveNodeIcon(node),
                iconTint = SceneGraphFactory.resolveNodeTint(node),
                depth = depth,
                isExpandable = isExpandable,
                isExpanded = isExpanded
            )
        )

        if (isExpanded) {
            for (child in node.children) {
                buildNodeLayerTree(child, outList, depth + 1)
            }
        }
    }

    fun toggleExpand(nodeId: String) {
        if (expandedNodeIds.contains(nodeId)) {
            expandedNodeIds.remove(nodeId)
        } else {
            expandedNodeIds.add(nodeId)
        }
        hierarchyVersion++
    }
}
