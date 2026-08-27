package org.mdt.ui.screens.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arc.graphics.Color
import org.mdt.core.ui.UINode
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.ComponentNode
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.undo.*
import org.mdt.ui.theme.StudioIcons

/**
 * ## EditorDocumentState
 *
 * Single Source of Truth for the Neko Studio Editor session.
 * Manages the live In-Memory Virtual Node Scene Graph ([rootScene], [masterComponents]),
 * selection tracking, live hierarchy generation, and direct [UndoRedoManager] history stack.
 *
 * Fully decoupled from XML serialization for instant 60 FPS 0-GC reactivity.
 *
 * See: docs/architecture/architecture_en.md
 */
class EditorDocumentState(
    initialScene: SceneNode? = null
) {
    /** Live In-Memory Artboard Scene Root Node. */
    var rootScene: SceneNode by mutableStateOf(initialScene ?: createDefaultScene())

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
    }

    /**
     * Performs an Undo operation.
     */
    fun undo(): Boolean = undoRedoManager.undo(this)

    /**
     * Performs a Redo operation.
     */
    fun redo(): Boolean = undoRedoManager.redo(this)

    // =========================================================================
    // II. Live Hierarchy Generation (Walking Virtual Node Graph Directly)
    // =========================================================================

    /**
     * Builds the hierarchy tree directly from the in-memory [rootScene] and [masterComponents].
     */
    fun buildLayerTree(): List<TreeItemData> {
        val result = mutableListOf<TreeItemData>()
        buildNodeLayerTree(rootScene, result, 0)
        for (master in masterComponents) {
            buildNodeLayerTree(master, result, 0)
        }
        return result
    }

    private fun buildNodeLayerTree(node: UINode, outList: MutableList<TreeItemData>, depth: Int) {
        val id = node.id
        val displayName = resolveSemanticName(node)
        val isExpandable = node.children.isNotEmpty()
        val isExpanded = expandedNodeIds.contains(id) || depth == 0

        outList.add(
            TreeItemData(
                id = id,
                name = displayName,
                type = node.javaClass.simpleName,
                iconUrl = resolveNodeIcon(node),
                iconTint = resolveNodeTint(node),
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

    private fun resolveSemanticName(node: UINode): String {
        if (node.name.isNotEmpty()) return node.name

        return when (node) {
            is SceneNode -> "Scene: ${node.name} (${node.artboardWidth.toInt()}×${node.artboardHeight.toInt()})"
            is ComponentNode -> if (node.isMaster) "❖ ${node.componentName}" else "◇ ${node.componentName}"
            is TextNode -> "\"${node.text.take(15)}${if (node.text.length > 15) "..." else ""}\""
            is LayoutNode -> {
                val policy = node.measurePolicy
                when {
                    policy is RowMeasurePolicy -> "Row Layout"
                    policy is ColumnMeasurePolicy -> "Column Layout"
                    node.visuals?.backdrop?.enabled == true -> "Glass Card"
                    (node.visuals?.radii?.topLeft ?: 0f) > 0f -> "Rounded Box"
                    else -> "Box"
                }
            }
            else -> node.javaClass.simpleName
        }
    }

    fun toggleExpand(nodeId: String) {
        if (expandedNodeIds.contains(nodeId)) {
            expandedNodeIds.remove(nodeId)
        } else {
            expandedNodeIds.add(nodeId)
        }
    }

    // =========================================================================
    // III. Static Factory & Helpers
    // =========================================================================

    companion object {
        /**
         * Builds the initial clean default scene graph using pure Virtual Nodes.
         */
        fun createDefaultScene(): SceneNode {
            val scene = SceneNode(
                name = "MainScene",
                preset = "desktop_720p",
                artboardWidth = 1280f,
                artboardHeight = 720f,
                backgroundColor = Color.valueOf("181926")
            )

            // HeroCard Glass Box
            val heroCard = LayoutNode().apply {
                id = "hero_card"
                name = "HeroCard"
                width = 440f
                pad(22f)
                anchorData.setPreset(LayoutPreset.CENTER)

                val vis = ensureVisuals()
                vis.background.mode = BackgroundFill.Mode.BACKDROP
                vis.backdrop.enabled = true
                vis.backdrop.blurRadius = 24f
                vis.backdrop.tint.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                vis.background.color.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                vis.radii.set(16f)
                vis.border.width = 1f
                vis.border.color.set(Color(1f, 1f, 1f, 0.14f))

                measurePolicy = ColumnMeasurePolicy(gap = 14f, alignment = Alignment.TopStart)
            }

            // Header Row (Icon + Title Column)
            val headerRow = LayoutNode().apply {
                id = "header_row"
                name = "Header Row"
                measurePolicy = RowMeasurePolicy(gap = 12f, alignment = Alignment.CenterStart)
            }

            val iconBox = LayoutNode().apply {
                id = "app_icon"
                name = "App Icon"
                width = 36f
                height = 36f
                val vis = ensureVisuals()
                vis.background.mode = BackgroundFill.Mode.COLOR
                vis.background.color.set(Color.valueOf("0a84ff"))
                vis.radii.set(8f)
            }

            val titleColumn = LayoutNode().apply {
                id = "title_column"
                name = "Title Column"
                measurePolicy = ColumnMeasurePolicy(gap = 4f, alignment = Alignment.TopStart)
            }

            val titleText = TextNode(text = "Apple HIG NXML Studio").apply {
                id = "title_text"
                name = "Studio Title"
                textVisuals.color = Color.white
            }

            val subtitleText = TextNode(text = "Real-time GPU Gaussian Blur & SDF").apply {
                id = "subtitle_text"
                name = "Subtitle"
                textVisuals.color = Color.valueOf("8f8f8f")
            }

            titleColumn.addChild(titleText)
            titleColumn.addChild(subtitleText)

            headerRow.addChild(iconBox)
            headerRow.addChild(titleColumn)

            // Divider Line
            val divider = LayoutNode().apply {
                id = "divider_line"
                name = "Divider"
                height = 1f
                sizeFlagsHorizontal = org.mdt.core.ui.layout.SizeFlags.FILL
                val vis = ensureVisuals()
                vis.background.mode = BackgroundFill.Mode.COLOR
                vis.background.color.set(Color(1f, 1f, 1f, 0.08f))
            }

            // Body Description
            val bodyDesc = TextNode(
                text = "Declarative Virtual Node layout with real-time SDF GPU shaders, Apple Glassmorphism and zero-GC reactivity."
            ).apply {
                id = "desc_text"
                name = "Description"
                textVisuals.color = Color(0.92f, 0.92f, 0.96f, 0.70f)
                textVisuals.wrap = true
            }

            // Actions Row (Get Started + Docs)
            val actionsRow = LayoutNode().apply {
                id = "actions_row"
                name = "Actions Row"
                measurePolicy = RowMeasurePolicy(gap = 8f, alignment = Alignment.CenterStart)
            }

            val btnGetStarted = LayoutNode().apply {
                id = "btn_get_started"
                name = "Button: Get Started"
                pad(horizontal = 18f, vertical = 10f)
                val vis = ensureVisuals()
                vis.background.mode = BackgroundFill.Mode.COLOR
                vis.background.color.set(Color.valueOf("0a84ff"))
                vis.radii.set(8f)
                addChild(TextNode(text = "Get Started").apply {
                    id = "btn_txt_1"
                    textVisuals.color = Color.white
                })
            }

            val btnDocs = LayoutNode().apply {
                id = "btn_docs"
                name = "Button: Docs"
                pad(horizontal = 18f, vertical = 10f)
                val vis = ensureVisuals()
                vis.background.mode = BackgroundFill.Mode.COLOR
                vis.background.color.set(Color.valueOf("2c3e55"))
                vis.radii.set(8f)
                addChild(TextNode(text = "Docs").apply {
                    id = "btn_txt_2"
                    textVisuals.color = Color.white
                })
            }

            actionsRow.addChild(btnGetStarted)
            actionsRow.addChild(btnDocs)

            heroCard.addChild(headerRow)
            heroCard.addChild(divider)
            heroCard.addChild(bodyDesc)
            heroCard.addChild(actionsRow)

            scene.addChild(heroCard)
            return scene
        }

        fun resolveNodeIcon(node: UINode): String {
            return when (node) {
                is SceneNode -> StudioIcons.SCENE
                is ComponentNode -> if (node.isMaster) StudioIcons.COMPONENTS else StudioIcons.COMPONENT
                is TextNode -> StudioIcons.TEXT
                is LayoutNode -> {
                    when {
                        node.measurePolicy is RowMeasurePolicy -> StudioIcons.CONTAINER
                        node.measurePolicy is ColumnMeasurePolicy -> StudioIcons.CONTAINER
                        node.visuals?.backdrop?.enabled == true -> StudioIcons.CARD
                        else -> StudioIcons.CONTAINER
                    }
                }
                else -> StudioIcons.FILE
            }
        }

        fun resolveNodeTint(node: UINode): Color {
            return when (node) {
                is SceneNode -> Color.valueOf("0a84ff") // Blue
                is ComponentNode -> Color.valueOf("bf5af2") // Purple
                is TextNode -> Color.valueOf("ffd60a") // Yellow
                is LayoutNode -> {
                    when {
                        node.measurePolicy is RowMeasurePolicy || node.measurePolicy is ColumnMeasurePolicy -> Color.valueOf("30d158") // Green
                        node.visuals?.backdrop?.enabled == true -> Color.valueOf("64d2ff") // Teal
                        else -> Color.valueOf("8e8e93") // Gray
                    }
                }
                else -> Color.valueOf("8e8e93")
            }
        }
    }
}
