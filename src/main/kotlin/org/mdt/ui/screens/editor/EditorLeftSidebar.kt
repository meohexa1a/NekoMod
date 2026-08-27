package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.model.EditorMode
import org.mdt.ui.screens.editor.model.TreeItemData
import org.mdt.ui.screens.editor.sidebar.*
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## EditorLeftSidebar
 *
 * Clean, modular left sidebar synchronized with the active [EditorMode].
 * Eliminates overlapping sub-tabs and provides dedicated panels for Scene Layers,
 * Component Library, and NXML Code.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorLeftSidebar(
    mode: EditorMode = EditorMode.SCENE,
    docState: EditorDocumentState? = null,
    selectedComponent: String = "PrimaryButton",
    onSelectComponent: (String) -> Unit = {},
    onCollapse: () -> Unit = {}
) {
    var showAddMenu by remember { mutableStateOf(false) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    val headerTitle = when (mode) {
        EditorMode.SCENE -> "Scene Layers"
        EditorMode.COMPONENTS -> "Components"
        EditorMode.ASSETS -> "Project Assets"
        EditorMode.I18N -> "Localization"
        EditorMode.SETTINGS -> "Editor Settings"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =================================================================
            // 1. Sidebar Top Header
            // =================================================================
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .pad(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                MonoText(
                    text = headerTitle,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.weight(1.0f))

                if (mode == EditorMode.SCENE) {
                    // Add Node Button
                    Box(
                        modifier = Modifier
                            .size(24f, 24f)
                            .radius(shapes.xs)
                            .background(if (showAddMenu) colors.blue else colors.surfaceSecondary)
                            .clickable { showAddMenu = !showAddMenu }
                            .pad(4f)
                    ) {
                        Image(
                            source = StudioIcons.PLUS,
                            modifier = Modifier.fillMaxSize(),
                            tint = if (showAddMenu) Color.White else colors.textSecondary
                        )
                    }
                } else if (mode == EditorMode.COMPONENTS) {
                    // New Component Button
                    Box(
                        modifier = Modifier
                            .radius(shapes.xs)
                            .background(colors.blue)
                            .clickable {
                                val newComp = org.mdt.ui.components.layout.ComponentNode("NewComponent", isMaster = true)
                                docState?.masterComponents?.add(newComp)
                            }
                            .pad(horizontal = spacing.xs + 2f, vertical = 2f)
                    ) {
                        Text(text = "+ New", color = Color.White, scale = 1.0f)
                    }
                }

                // Collapse Sidebar Button
                Box(
                    modifier = Modifier
                        .size(24f, 24f)
                        .radius(shapes.xs)
                        .clickable { onCollapse() }
                        .pad(4f)
                ) {
                    Image(
                        source = StudioIcons.CHEVRON_LEFT,
                        modifier = Modifier.fillMaxSize(),
                        tint = colors.textSecondary
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxWidth().height(1f))

            // =================================================================
            // 2. Body: Dedicated Panel based on EditorMode
            // =================================================================
            ScrollView(
                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                enableVertical = true
            ) {
                Box(modifier = Modifier.fillMaxWidth().pad(horizontal = spacing.md, vertical = spacing.sm)) {
                    when (mode) {
                        EditorMode.SCENE -> SidebarLayersTab(
                            docState = docState,
                            showAddMenu = showAddMenu,
                            onCloseAddMenu = { showAddMenu = false }
                        )
                        EditorMode.COMPONENTS -> SidebarComponentsTab(
                            docState = docState,
                            selectedComponent = selectedComponent,
                            onSelectComponent = onSelectComponent
                        )
                        EditorMode.ASSETS, EditorMode.I18N, EditorMode.SETTINGS -> SidebarAssetsTab()
                    }
                }
            }
        }
    }
}
