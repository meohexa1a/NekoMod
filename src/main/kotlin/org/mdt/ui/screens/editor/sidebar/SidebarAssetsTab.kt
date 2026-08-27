package org.mdt.ui.screens.editor.sidebar

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## SidebarAssetsTab
 *
 * Assets and Sprite Atlas browser panel.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SidebarAssetsTab() {
    val colors = Theme.colors
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.sm),
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Assets & Sprite Atlas", color = colors.textPrimary, font = typography.title)
        Text(
            text = "Browse Mindustry sprite atlas regions and vector glyphs to insert into your scene.",
            color = colors.textSecondary
        )
    }
}
