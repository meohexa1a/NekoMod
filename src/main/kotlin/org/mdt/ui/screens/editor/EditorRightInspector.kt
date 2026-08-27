package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.scroll.ScrollView
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.inspector.*
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## EditorRightInspector
 *
 * Modular right inspector panel coordinating tabs for Element properties, Component Schema, and Materials.
 * Supports resizing and collapsing via [onCollapse].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorRightInspector(
    docState: EditorDocumentState? = null,
    selectedComponent: String = "PrimaryButton",
    isComponentMode: Boolean = false,
    onCollapse: () -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    var selectedTab by remember { mutableStateOf("Inspect") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
    ) {
        ScrollView(
            modifier = Modifier.fillMaxSize(),
            enableVertical = true
        ) {
            Column(
                arrangement = Arrangement.spacedBy(spacing.md),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().pad(spacing.md)
            ) {
                // -------------------------------------------------------------
                // 1. Inspector Tabs (Inspect | Material) + Collapse Button
                // -------------------------------------------------------------
                Row(
                    arrangement = Arrangement.spacedBy(spacing.lg),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (tab in listOf("Inspect", "Material")) {
                        val isSelected = tab == selectedTab
                        Column(
                            arrangement = Arrangement.spacedBy(spacing.xs),
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            MonoText(
                                text = tab,
                                color = if (isSelected) colors.textPrimary else colors.textTertiary
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2f)
                                        .radius(shapes.pill)
                                        .background(colors.blue)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    // Collapse Inspector Button
                    Box(
                        modifier = Modifier
                            .size(24f, 24f)
                            .radius(shapes.xs)
                            .clickable { onCollapse() }
                            .pad(4f)
                    ) {
                        Image(
                            source = StudioIcons.CHEVRON_RIGHT,
                            modifier = Modifier.fillMaxSize(),
                            tint = colors.textSecondary
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // -------------------------------------------------------------
                // 2. Tab Content Body (Delegated to modular sub-components)
                // -------------------------------------------------------------
                if (selectedTab == "Inspect") {
                    if (isComponentMode) {
                        InspectorComponentSchemaTab(selectedComponent = selectedComponent)
                    } else {
                        InspectorElementTab(docState = docState, node = docState?.selectedNode)
                    }
                } else if (selectedTab == "Material") {
                    InspectorMaterialTab()
                }
            }
        }
    }
}
