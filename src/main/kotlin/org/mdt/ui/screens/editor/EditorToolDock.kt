package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.graphics.Color
import mindustry.gen.Icon
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.theme.Theme

/**
 * ## EditorToolDock
 *
 * Apple macOS-inspired floating tool dock with frosted glass styling and continuous curvature.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorToolDock(
    selectedTool: String = "select",
    onSelectTool: (String) -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    val tools = listOf(
        "select" to Icon.move,
        "frame" to Icon.resize,
        "rect" to Icon.box,
        "text" to Icon.fileText,
        "pen" to Icon.pencil,
        "component" to Icon.tree,
        "comment" to Icon.chat
    )

    Box(
        modifier = Modifier
            .radius(shapes.pill)
            .background(colors.surfaceElevated)
            .border(1f, colors.borderHairline)
            .shadow(colors.shadowKey, blur = 18f)
            .pad(horizontal = spacing.sm, vertical = spacing.xs + 2f)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(spacing.xs),
            alignment = Alignment.CenterStart
        ) {
            for ((toolId, icon) in tools) {
                val isSelected = toolId == selectedTool
                Box(
                    modifier = Modifier
                        .size(34f, 34f)
                        .radius(shapes.sm)
                        .background(if (isSelected) colors.blue else Color.clear)
                        .clickable { onSelectTool(toolId) }
                        .pad(spacing.sm)
                ) {
                    Image(
                        region = icon.region,
                        modifier = Modifier.fillMaxSize(),
                        tint = if (isSelected) Color.white else colors.textSecondary
                    )
                }
            }
        }
    }
}
