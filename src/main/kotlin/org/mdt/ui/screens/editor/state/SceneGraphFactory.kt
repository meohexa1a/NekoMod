package org.mdt.ui.screens.editor.state

import org.mdt.core.ui.UINode
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.ComponentNode
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.theme.StudioIcons

/**
 * ## SceneGraphFactory
 *
 * Factory and descriptor resolver for constructing initial scene templates and resolving
 * semantic icons, tints, and names for Virtual Nodes across the Editor.
 *
 * See: docs/architecture/architecture_en.md
 */
object SceneGraphFactory {

    /**
     * Constructs a high-fidelity Apple HIG HeroCard default Scene.
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
            vis.backdrop.tint = Color(0.10f, 0.10f, 0.16f, 0.75f)
            vis.background.color = Color(0.10f, 0.10f, 0.16f, 0.75f)
            vis.radii.set(16f)
            vis.border.width = 1f
            vis.border.color = Color(1f, 1f, 1f, 0.14f)

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
            vis.background.color = Color.valueOf("0a84ff")
            vis.radii.set(8f)
        }

        val titleColumn = LayoutNode().apply {
            id = "title_column"
            name = "Title Column"
            measurePolicy = ColumnMeasurePolicy(gap = 4f, alignment = Alignment.TopStart)
        }

        val titleText = TextNode(text = "Apple HIG Virtual Studio").apply {
            id = "title_text"
            name = "Studio Title"
            textVisuals.color = Color.White
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
            vis.background.color = Color(1f, 1f, 1f, 0.08f)
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
            vis.background.color = Color.valueOf("0a84ff")
            vis.radii.set(8f)
            addChild(TextNode(text = "Get Started").apply {
                id = "btn_txt_1"
                textVisuals.color = Color.White
            })
        }

        val btnDocs = LayoutNode().apply {
            id = "btn_docs"
            name = "Button: Docs"
            pad(horizontal = 18f, vertical = 10f)
            val vis = ensureVisuals()
            vis.background.mode = BackgroundFill.Mode.COLOR
            vis.background.color = Color.valueOf("2c3e55")
            vis.radii.set(8f)
            addChild(TextNode(text = "Docs").apply {
                id = "btn_txt_2"
                textVisuals.color = Color.White
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

    fun resolveSemanticName(node: UINode): String {
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
}
