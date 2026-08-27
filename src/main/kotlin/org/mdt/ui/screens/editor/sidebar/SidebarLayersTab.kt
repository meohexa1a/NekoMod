package org.mdt.ui.screens.editor.sidebar

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.ColumnMeasurePolicy
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.TextNode
import org.mdt.ui.screens.editor.EditorDocumentState
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SidebarLayersTab
 *
 * Semantic layer tree panel directly reflecting the In-Memory Virtual Node Scene Graph.
 * Supports depth-based background shading, quick-insert popovers, and 0-latency selection.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SidebarLayersTab(
    docState: EditorDocumentState?,
    showAddMenu: Boolean,
    onCloseAddMenu: () -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    val layerTree = docState?.buildLayerTree() ?: emptyList()
    val selectedId = docState?.selectedNodeId

    Column(
        arrangement = Arrangement.spacedBy(spacing.sm),
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        // =====================================================================
        // 1. Quick Add Virtual Node Popover Menu
        // =====================================================================
        if (showAddMenu && docState != null) {
            Column(
                arrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.sm)
                    .background(colors.surfaceSecondary)
                    .border(1f, colors.blue)
                    .pad(spacing.sm)
            ) {
                MonoText(text = "Insert Virtual Node:", color = colors.textPrimary)
                Row(
                    arrangement = Arrangement.spacedBy(4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "Box" to {
                            LayoutNode().apply {
                                width = 200f
                                height = 100f
                                val v = ensureVisuals()
                                v.background.mode = BackgroundFill.Mode.COLOR
                                v.background.color.set(Color.valueOf("0a84ff"))
                                v.radii.set(8f)
                            }
                        },
                        "Text" to { TextNode(text = "Sample Text").apply { textVisuals.color = Color.white } },
                        "Column" to { LayoutNode().apply { measurePolicy = ColumnMeasurePolicy(gap = 8f) } },
                        "Row" to { LayoutNode().apply { measurePolicy = RowMeasurePolicy(gap = 8f) } },
                        "Glass" to {
                            LayoutNode().apply {
                                width = 300f
                                pad(16f)
                                val v = ensureVisuals()
                                v.background.mode = BackgroundFill.Mode.BACKDROP
                                v.backdrop.enabled = true
                                v.backdrop.blurRadius = 20f
                                v.backdrop.tint.set(Color(0.12f, 0.13f, 0.18f, 0.70f))
                                v.background.color.set(Color(0.12f, 0.13f, 0.18f, 0.70f))
                                v.radii.set(16f)
                            }
                        }
                    ).forEach { (label, creator) ->
                        Box(
                            modifier = Modifier
                                .radius(shapes.xs)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .clickable {
                                    val parent = (docState.selectedNode as? LayoutNode) ?: docState.rootScene
                                    docState.addNode(parent, creator())
                                    onCloseAddMenu()
                                }
                                .pad(horizontal = 6f, vertical = 3f)
                        ) {
                            MonoText(text = "+$label", color = colors.blue)
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. Dynamic Live Virtual Node Tree with Depth Shading
        // =====================================================================
        Column(
            arrangement = Arrangement.spacedBy(2f),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (item in layerTree) {
                val isSelected = item.id == selectedId || item.name == selectedId
                val indentPx = minOf(60f, item.depth * 10f)

                // Progressive background shading: deeper levels get progressively darker
                val depthShade = Color(0.12f, 0.13f, 0.18f, minOf(0.90f, 0.20f + item.depth * 0.12f))
                val rowBackground = if (isSelected) colors.blue else depthShade

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .radius(shapes.xs)
                        .background(rowBackground)
                        .border(1f, if (isSelected) colors.blue else Color(1f, 1f, 1f, 0.04f))
                        .clickable {
                            docState?.selectedNodeId = item.id
                        }
                        .pad(horizontal = 4f, vertical = 3f)
                ) {
                    Row(
                        arrangement = Arrangement.spacedBy(4f),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (indentPx > 0f) {
                            Spacer(modifier = Modifier.width(indentPx))
                        }

                        if (item.isExpandable) {
                            Box(
                                modifier = Modifier
                                    .size(12f, 12f)
                                    .clickable { docState?.toggleExpand(item.id) }
                            ) {
                                Image(
                                    source = if (item.isExpanded) StudioIcons.CHEVRON_DOWN else StudioIcons.CHEVRON_RIGHT,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = if (isSelected) Color.white else colors.textTertiary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(12f, 12f))
                        }

                        Image(
                            source = item.iconUrl,
                            modifier = Modifier.size(14f),
                            tint = if (isSelected) Color.white else item.iconTint
                        )

                        MonoText(
                            text = item.name,
                            color = if (isSelected) Color.white else colors.textPrimary,
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                }
            }
        }
    }
}
