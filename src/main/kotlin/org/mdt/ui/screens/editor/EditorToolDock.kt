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

/**
 * ## EditorToolDock
 *
 * Bottom floating tool dock with crisp native icons and auto-layout spacing.
 */
@Composable
fun EditorToolDock(
    selectedTool: String = "select",
    onSelectTool: (String) -> Unit = {}
) {
    val figmaBg = Color.valueOf("1e1e1e")
    val figmaBorder = Color.valueOf("333333")
    val figmaText = Color.valueOf("ffffff")
    val figmaMuted = Color.valueOf("949494")
    val figmaAccent = Color.valueOf("0d99ff")

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
            .radius(24f)
            .background(figmaBg)
            .border(1f, figmaBorder)
            .shadow(Color(0f, 0f, 0f, 0.45f), blur = 16f)
            .pad(horizontal = 8f, vertical = 6f)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(4f),
            alignment = Alignment.CenterStart
        ) {
            for ((toolId, icon) in tools) {
                val isSelected = toolId == selectedTool
                Box(
                    modifier = Modifier
                        .size(34f, 34f)
                        .radius(6f)
                        .background(if (isSelected) figmaAccent else Color.clear)
                        .clickable { onSelectTool(toolId) }
                        .pad(8f)
                ) {
                    Image(
                        region = icon.region,
                        modifier = Modifier.fillMaxSize(),
                        tint = if (isSelected) Color.white else figmaMuted
                    )
                }
            }
        }
    }
}
