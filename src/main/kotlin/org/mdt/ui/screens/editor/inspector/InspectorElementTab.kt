package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.screens.editor.state.SceneGraphFactory
import org.mdt.ui.theme.Theme

/**
 * ## InspectorElementTab
 *
 * Real-time 2-way property inspector router binding to live Virtual [UINode] instances.
 * Coordinates modular sub-inspectors:
 * - [InspectorSceneSection] for Artboard frames
 * - [InspectorLayoutSection] for 2D Box Model, Anchors, and Dimensions
 * - [InspectorVisualsSection] for Colors, Radii, and Glass Blur
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
            // 1. Node Header
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    source = SceneGraphFactory.resolveNodeIcon(node),
                    modifier = Modifier.size(16f),
                    tint = SceneGraphFactory.resolveNodeTint(node)
                )
                MonoText(text = nodeName, color = colors.textPrimary)
                Spacer(modifier = Modifier.weight(1.0f))
                MonoText(text = nodeId, color = colors.textTertiary)
            }

            Divider(modifier = Modifier.fillMaxWidth().height(1f))

            // 2. Scene Artboard Section or General Layout & Visuals Sections
            if (node is SceneNode) {
                InspectorSceneSection(node = node)
            } else {
                InspectorLayoutSection(node = node)
                InspectorVisualsSection(node = node)

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Delete Node Button
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
