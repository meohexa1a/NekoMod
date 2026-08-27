package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.EditorDocumentState
import org.mdt.ui.theme.Theme

/**
 * ## InspectorElementTab
 *
 * Real-time 2-way property inspector directly binding to live Virtual [UINode] instances.
 * Provides instant 60 FPS feedback for Scene Artboards, Layout Containers, and Text Nodes.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorElementTab(
    docState: EditorDocumentState?,
    node: UINode?
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    if (node != null && docState != null) {
        val nodeName = node.name.ifEmpty { node.javaClass.simpleName }
        val nodeId = node.id

        Column(
            arrangement = Arrangement.spacedBy(spacing.md),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Node Header
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    source = EditorDocumentState.resolveNodeIcon(node),
                    modifier = Modifier.size(16f),
                    tint = EditorDocumentState.resolveNodeTint(node)
                )
                MonoText(text = nodeName, color = colors.textPrimary)
                Spacer(modifier = Modifier.weight(1.0f))
                MonoText(text = nodeId, color = colors.textTertiary)
            }

            Divider(modifier = Modifier.fillMaxWidth().height(1f))

            // =================================================================
            // Case A: Scene Node (Artboard Frame Settings)
            // =================================================================
            if (node is SceneNode) {
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Device Presets", color = colors.textSecondary)
                    Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "1080p" to "desktop_1080p",
                            "720p" to "desktop_720p",
                            "Mobile" to "mobile_portrait",
                            "Modal" to "dialog_modal"
                        ).forEach { (label, preset) ->
                            val isSel = node.preset == preset
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .clickable { node.applyPreset(preset) }
                                    .pad(vertical = 4f)
                            ) {
                                MonoText(
                                    text = label,
                                    color = if (isSel) Color.white else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Artboard Dimensions
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Artboard Dimensions", color = colors.textSecondary)
                    Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "W: ${node.artboardWidth.toInt()} px", color = colors.textPrimary)
                        }
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "H: ${node.artboardHeight.toInt()} px", color = colors.textPrimary)
                        }
                    }
                }

                // Background Fill
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Artboard Background", color = colors.textSecondary)
                    Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                        listOf("#181926", "#0f1015", "#000000", "#1c1d22", "#2c3e55", "#ffffff").forEach { hex ->
                            val col = Color.valueOf(hex)
                            val isSel = node.backgroundColor == col
                            Box(
                                modifier = Modifier
                                    .size(24f, 24f)
                                    .radius(shapes.xs)
                                    .background(col)
                                    .border(if (isSel) 2f else 1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable {
                                        node.backgroundColor.set(col)
                                        node.ensureVisuals().background.color.set(col)
                                        node.invalidateLayout()
                                    }
                            )
                        }
                    }
                }
            } else {
                // =============================================================
                // Case B: General Layout Node & Text Node
                // =============================================================

                // 0. 9-Point Anchor Preset
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Anchor Preset", color = colors.textSecondary)
                    val anchorRows: List<List<Pair<LayoutPreset, String>>> = listOf(
                        listOf(LayoutPreset.TOP_LEFT to "TL", LayoutPreset.CENTER_TOP to "TC", LayoutPreset.TOP_RIGHT to "TR"),
                        listOf(LayoutPreset.CENTER_LEFT to "CL", LayoutPreset.CENTER to "C", LayoutPreset.CENTER_RIGHT to "CR"),
                        listOf(LayoutPreset.BOTTOM_LEFT to "BL", LayoutPreset.CENTER_BOTTOM to "BC", LayoutPreset.BOTTOM_RIGHT to "BR")
                    )

                    Column(arrangement = Arrangement.spacedBy(2f), modifier = Modifier.fillMaxWidth()) {
                        for (row in anchorRows) {
                            Row(arrangement = Arrangement.spacedBy(2f), modifier = Modifier.fillMaxWidth()) {
                                for ((preset, label) in row) {
                                    val isSel = node.anchorData.activePreset == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .radius(shapes.xs)
                                            .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                            .clickable {
                                                node.anchorData.setPreset(preset)
                                                node.invalidateLayout()
                                            }
                                            .pad(vertical = 4f)
                                    ) {
                                        MonoText(
                                            text = label,
                                            color = if (isSel) Color.white else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Position X & Y
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Position (px)", color = colors.textSecondary)
                    Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "X: ${node.anchorData.offsetLeft.toInt()}", color = colors.textPrimary)
                            Row(arrangement = Arrangement.spacedBy(2f)) {
                                listOf(0f, 50f, 150f, 300f).forEach { x ->
                                    Box(
                                        modifier = Modifier
                                            .radius(shapes.xs)
                                            .background(if (node.anchorData.offsetLeft == x) colors.surfaceHighlight else colors.surfaceSecondary)
                                            .clickable {
                                                node.anchorData.offsetLeft = x
                                                node.invalidateLayout()
                                            }
                                            .pad(horizontal = 4f, vertical = 2f)
                                    ) {
                                        MonoText(text = "${x.toInt()}", color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "Y: ${node.anchorData.offsetTop.toInt()}", color = colors.textPrimary)
                            Row(arrangement = Arrangement.spacedBy(2f)) {
                                listOf(0f, 50f, 150f, 300f).forEach { y ->
                                    Box(
                                        modifier = Modifier
                                            .radius(shapes.xs)
                                            .background(if (node.anchorData.offsetTop == y) colors.surfaceHighlight else colors.surfaceSecondary)
                                            .clickable {
                                                node.anchorData.offsetTop = y
                                                node.invalidateLayout()
                                            }
                                            .pad(horizontal = 4f, vertical = 2f)
                                    ) {
                                        MonoText(text = "${y.toInt()}", color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Dimensions Width & Height
                Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                    MonoText(text = "Dimensions (px)", color = colors.textSecondary)
                    Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        // Width Presets
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "W: ${if (node.width < 0f) "Auto" else "${node.width.toInt()}"}", color = colors.textPrimary)
                            Row(arrangement = Arrangement.spacedBy(2f)) {
                                listOf(100f, 200f, 400f, -1f).forEach { w ->
                                    Box(
                                        modifier = Modifier
                                            .radius(shapes.xs)
                                            .background(if (node.width == w) colors.surfaceHighlight else colors.surfaceSecondary)
                                            .clickable {
                                                node.width = w
                                                node.invalidateLayout()
                                            }
                                            .pad(horizontal = 4f, vertical = 2f)
                                    ) {
                                        MonoText(text = if (w < 0f) "Auto" else "${w.toInt()}", color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        // Height Presets
                        Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                            MonoText(text = "H: ${if (node.height < 0f) "Auto" else "${node.height.toInt()}"}", color = colors.textPrimary)
                            Row(arrangement = Arrangement.spacedBy(2f)) {
                                listOf(40f, 80f, 140f, -1f).forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .radius(shapes.xs)
                                            .background(if (node.height == h) colors.surfaceHighlight else colors.surfaceSecondary)
                                            .clickable {
                                                node.height = h
                                                node.invalidateLayout()
                                            }
                                            .pad(horizontal = 4f, vertical = 2f)
                                    ) {
                                        MonoText(text = if (h < 0f) "Auto" else "${h.toInt()}", color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Visuals: Background Color & Glass Blur (For LayoutNode)
                if (node is LayoutNode) {
                    val vis = node.ensureVisuals()
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Background Fill", color = colors.textSecondary)
                        Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                            listOf("#0a84ff", "#30d158", "#bf5af2", "#ff9f0a", "#1c1d22", "#ffffff").forEach { hex ->
                                val col = Color.valueOf(hex)
                                val isSel = vis.background.color == col
                                Box(
                                    modifier = Modifier
                                        .size(24f, 24f)
                                        .radius(shapes.xs)
                                        .background(col)
                                        .border(if (isSel) 2f else 1f, if (isSel) colors.blue else colors.borderHairline)
                                        .clickable {
                                            vis.background.mode = BackgroundFill.Mode.COLOR
                                            vis.background.color.set(col)
                                            node.invalidateLayout()
                                        }
                                )
                            }
                        }
                    }

                    // 4. Corner Radius
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Corner Radius: ${vis.radii.topLeft.toInt()}px", color = colors.textSecondary)
                        Row(arrangement = Arrangement.spacedBy(4f)) {
                            listOf(0f, 4f, 8f, 12f, 16f, 999f).forEach { r ->
                                Box(
                                    modifier = Modifier
                                        .radius(shapes.xs)
                                        .background(if (vis.radii.topLeft == r) colors.surfaceHighlight else colors.surfaceSecondary)
                                        .border(1f, colors.borderHairline)
                                        .clickable {
                                            vis.radii.set(r)
                                            node.invalidateLayout()
                                        }
                                        .pad(horizontal = 6f, vertical = 2f)
                                ) {
                                    MonoText(text = "${r.toInt()}", color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    // 5. Backdrop Blur Radius
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Frosted Glass Blur: ${if (vis.backdrop.enabled) "${vis.backdrop.blurRadius.toInt()}px" else "Off"}", color = colors.textSecondary)
                        Row(arrangement = Arrangement.spacedBy(4f)) {
                            listOf(0f, 12f, 20f, 24f, 32f).forEach { b ->
                                Box(
                                    modifier = Modifier
                                        .radius(shapes.xs)
                                        .background(if (vis.backdrop.enabled && vis.backdrop.blurRadius == b) colors.surfaceHighlight else colors.surfaceSecondary)
                                        .border(1f, colors.borderHairline)
                                        .clickable {
                                            vis.backdrop.enabled = b > 0f
                                            vis.backdrop.blurRadius = b
                                            if (b > 0f) {
                                                vis.background.mode = BackgroundFill.Mode.BACKDROP
                                                vis.background.color.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                                                vis.backdrop.tint.set(Color(0.10f, 0.10f, 0.16f, 0.75f))
                                            }
                                            node.invalidateLayout()
                                        }
                                        .pad(horizontal = 6f, vertical = 2f)
                                ) {
                                    MonoText(text = if (b == 0f) "Off" else "${b.toInt()}px", color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    // 6. Padding
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Padding: ${node.padL.toInt()}px", color = colors.textSecondary)
                        Row(arrangement = Arrangement.spacedBy(4f)) {
                            listOf(0f, 8f, 16f, 22f, 32f).forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .radius(shapes.xs)
                                        .background(if (node.padL == p) colors.surfaceHighlight else colors.surfaceSecondary)
                                        .border(1f, colors.borderHairline)
                                        .clickable {
                                            node.pad(p)
                                        }
                                        .pad(horizontal = 6f, vertical = 2f)
                                ) {
                                    MonoText(text = "${p.toInt()}px", color = colors.textPrimary)
                                }
                            }
                        }
                    }
                }

                // 7. Text Content (For TextNode)
                if (node is TextNode) {
                    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        MonoText(text = "Text Content", color = colors.textSecondary)
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.xs),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .radius(shapes.sm)
                                .background(colors.surfaceSecondary)
                                .border(1f, colors.borderHairline)
                                .pad(spacing.sm)
                        ) {
                            MonoText(text = "\"${node.text}\"", color = colors.green)
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Delete Node
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .radius(shapes.sm)
                        .background(Color.valueOf("3a1c1e"))
                        .border(1f, Color.valueOf("ff453a"))
                        .clickable { docState.removeSelectedNode() }
                        .pad(spacing.sm)
                ) {
                    MonoText(text = "Delete Virtual Node", color = Color.valueOf("ff453a"))
                }
            }
        }
    } else {
        // Empty State: Scene Overview & Shortcuts Cheat Sheet
        Column(
            arrangement = Arrangement.spacedBy(spacing.md),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
                MonoText(text = "SCENE OVERVIEW", color = colors.textTertiary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .radius(shapes.sm)
                        .background(colors.surfaceSecondary)
                        .border(1f, colors.borderHairline)
                        .pad(spacing.sm)
                ) {
                    Column(arrangement = Arrangement.spacedBy(4f)) {
                        MonoText(text = "Root: <SceneNode>", color = colors.textPrimary)
                        MonoText(text = "Artboard: ${docState?.rootScene?.artboardWidth?.toInt()} × ${docState?.rootScene?.artboardHeight?.toInt()} px", color = colors.textSecondary)
                        MonoText(text = "Preset: ${docState?.rootScene?.preset}", color = colors.blue)
                    }
                }
            }

            Divider(modifier = Modifier.fillMaxWidth().height(1f))

            Column(arrangement = Arrangement.spacedBy(6f), modifier = Modifier.fillMaxWidth()) {
                MonoText(text = "SHORTCUTS REFERENCE", color = colors.textTertiary)
                listOf(
                    "V" to "Select & Transform",
                    "R" to "Draw Box / Frame",
                    "T" to "Place Text Node",
                    "Del" to "Delete Selection",
                    "Ctrl+Z" to "Undo Last Action",
                    "Ctrl+Y" to "Redo Action",
                    "Ctrl+0" to "Reset Zoom 100%",
                    "Space+Drag" to "Pan Canvas Viewport"
                ).forEach { (key, desc) ->
                    Row(
                        arrangement = Arrangement.spacedBy(6f),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .pad(horizontal = 4f, vertical = 2f)
                        ) {
                            MonoText(text = key, color = colors.blue)
                        }
                        MonoText(text = desc, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
