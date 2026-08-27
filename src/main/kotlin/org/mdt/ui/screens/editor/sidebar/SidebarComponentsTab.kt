package org.mdt.ui.screens.editor.sidebar

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.state.EditorDocumentState
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SidebarComponentsTab
 *
 * Dedicated component registry panel for browsing, selecting, and declaring
 * reusable parametric UI components.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SidebarComponentsTab(
    docState: EditorDocumentState?,
    selectedComponent: String = "PrimaryButton",
    onSelectComponent: (String) -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.sm),
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        // =====================================================================
        // 1. Header & New Component Action
        // =====================================================================
        Row(
            arrangement = Arrangement.spacedBy(spacing.sm),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Project Components", color = colors.textPrimary, font = typography.title)
            Spacer(modifier = Modifier.weight(1.0f))

            Box(
                modifier = Modifier
                    .radius(shapes.xs)
                    .background(colors.blue)
                    .clickable {
                        val newComp = ComponentNode("NewComponent", isMaster = true)
                        docState?.masterComponents?.add(newComp)
                    }
                    .pad(horizontal = spacing.sm, vertical = 2f)
            ) {
                Text(text = "+ New", color = Color.white, scale = 1.0f)
            }
        }

        // =====================================================================
        // 2. Component Blueprint List
        // =====================================================================
        val customComponents = listOf(
            "PrimaryButton" to "Interactive action button with loading and icon states",
            "GlassCard" to "Apple frosted glass acrylic container with dynamic blur",
            "StatusBadge" to "Sleek status indicator pill with colored pulsing dot",
            "ItemSlot" to "Mindustry inventory item slot with rarity border"
        )

        for ((name, desc) in customComponents) {
            val isSelected = name == selectedComponent
            Column(
                arrangement = Arrangement.spacedBy(4f),
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.sm)
                    .background(if (isSelected) colors.surfaceHighlight else colors.surfaceSecondary)
                    .border(1f, if (isSelected) colors.blue else colors.borderHairline)
                    .clickable { onSelectComponent(name) }
                    .pad(spacing.sm)
            ) {
                Row(
                    arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        source = StudioIcons.COMPONENTS,
                        modifier = Modifier.size(14f),
                        tint = if (isSelected) colors.blue else colors.textSecondary
                    )
                    MonoText(
                        text = "<$name>",
                        color = if (isSelected) colors.blue else colors.textPrimary
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    Text(text = "Parametric", color = colors.textTertiary, scale = 0.9f)
                }
                Text(
                    text = desc,
                    color = colors.textSecondary
                )
            }
        }
    }
}
