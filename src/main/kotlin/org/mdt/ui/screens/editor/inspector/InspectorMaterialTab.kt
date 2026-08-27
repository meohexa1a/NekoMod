package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## InspectorMaterialTab
 *
 * Resource and Material browser for templates, in-game sprites, icons, and themes.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorMaterialTab() {
    val colors = Theme.colors
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.md),
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Component Templates", color = colors.textPrimary, font = typography.title)
        Text(
            text = "Click to insert pre-built Apple frosted glass cards, action buttons, and Mindustry slots into your scene.",
            color = colors.textSecondary
        )
    }
}
