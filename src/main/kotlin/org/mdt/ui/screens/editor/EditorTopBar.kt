package org.mdt.ui.screens.editor

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.scene.style.TextureRegionDrawable
import mindustry.gen.Icon
import mindustry.ui.Fonts
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonVariant
import org.mdt.ui.components.text.Text

/**
 * ## EditorMode
 *
 * Primary navigation modes for the NXML Studio workspace.
 */
enum class EditorMode(val title: String, val icon: TextureRegionDrawable) {
    LAYERS("Layers", Icon.tree),
    COMPONENTS("Components", Icon.box),
    I18N("i18n", Icon.book),
    CODE("NXML", Icon.fileText)
}

/**
 * ## EditorTopBar
 *
 * Clean, mathematically centered top navigation bar using [LayoutPreset.CENTER]
 * to guarantee true dead-center alignment for the segmented mode switcher.
 */
@Composable
fun EditorTopBar(
    currentMode: EditorMode = EditorMode.LAYERS,
    onSelectMode: (EditorMode) -> Unit = {},
    onPreview: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    val figmaBg = Color.valueOf("1e1e1e")
    val figmaBorder = Color.valueOf("333333")
    val figmaText = Color.valueOf("ffffff")
    val figmaMuted = Color.valueOf("8f8f8f")
    val figmaAccent = Color.valueOf("0d99ff")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48f)
            .background(figmaBg)
            .border(1f, figmaBorder)
    ) {
        // =====================================================================
        // 1. Left: Brand Logo & Title (Anchored to CENTER_LEFT)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_LEFT)
                .margin(left = 16f)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(10f),
                alignment = Alignment.CenterStart
            ) {
                Image(
                    region = Icon.hammer.region,
                    modifier = Modifier.size(20f),
                    tint = figmaAccent
                )
                Text(
                    text = "Neko Studio",
                    color = figmaText,
                    font = Fonts.def
                )
            }
        }

        // =====================================================================
        // 2. Center: Segmented Mode Switcher (True Screen Mathematical Dead-Center)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .radius(8f)
                .background(Color.valueOf("141414"))
                .border(1f, figmaBorder)
                .pad(3f)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(4f),
                alignment = Alignment.CenterStart
            ) {
                for (mode in EditorMode.values()) {
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .radius(6f)
                            .background(if (isSelected) Color.valueOf("2c3e55") else Color.clear)
                            .clickable { onSelectMode(mode) }
                            .pad(horizontal = 14f, vertical = 6f)
                    ) {
                        Row(
                            arrangement = Arrangement.spacedBy(8f),
                            alignment = Alignment.CenterStart
                        ) {
                            Image(
                                region = mode.icon.region,
                                modifier = Modifier.size(16f),
                                tint = if (isSelected) figmaAccent else figmaMuted
                            )
                            Text(
                                text = mode.title,
                                color = if (isSelected) figmaText else figmaMuted,
                                font = Fonts.def,
                                scale = 1.0f
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. Right: Studio Actions (Anchored to CENTER_RIGHT)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_RIGHT)
                .margin(right = 16f)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(8f),
                alignment = Alignment.CenterStart
            ) {
                Button(
                    text = "Reload",
                    icon = Icon.refresh,
                    variant = ButtonVariant.PLAIN,
                    paddingH = 12f,
                    paddingV = 6f,
                    onClick = onReload
                )

                Button(
                    text = "Preview",
                    icon = Icon.play,
                    variant = ButtonVariant.FILLED,
                    radius = 6f,
                    paddingH = 16f,
                    paddingV = 6f,
                    onClick = onPreview
                )
            }
        }
    }
}
