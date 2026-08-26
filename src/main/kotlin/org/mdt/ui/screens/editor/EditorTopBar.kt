package org.mdt.ui.screens.editor

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.scene.style.TextureRegionDrawable
import mindustry.gen.Icon
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
import org.mdt.ui.theme.Theme

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
 * Apple macOS-inspired top navigation bar featuring centered [EditorMode] segmented switcher,
 * frosted glass styling, and unified design tokens.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorTopBar(
    currentMode: EditorMode = EditorMode.LAYERS,
    onSelectMode: (EditorMode) -> Unit = {},
    onPreview: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48f)
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
    ) {
        // =====================================================================
        // 1. Left: Brand Logo & Title (Anchored to CENTER_LEFT)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER_LEFT)
                .margin(left = spacing.lg)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart
            ) {
                Image(
                    region = Icon.hammer.region,
                    modifier = Modifier.size(20f),
                    tint = colors.blue
                )
                Text(
                    text = "Neko Studio",
                    color = colors.textPrimary,
                    font = typography.title
                )
            }
        }

        // =====================================================================
        // 2. Center: Segmented Mode Switcher (True Screen Mathematical Dead-Center)
        // =====================================================================
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .radius(shapes.md)
                .background(colors.surfaceBackground)
                .border(1f, colors.borderHairline)
                .pad(3f)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.xs),
                alignment = Alignment.CenterStart
            ) {
                for (mode in EditorMode.values()) {
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .radius(shapes.sm)
                            .background(if (isSelected) colors.surfaceHighlight else Color.clear)
                            .clickable { onSelectMode(mode) }
                            .pad(horizontal = spacing.md, vertical = spacing.xs + 1f)
                    ) {
                        Row(
                            arrangement = Arrangement.spacedBy(spacing.sm),
                            alignment = Alignment.CenterStart
                        ) {
                            Image(
                                region = mode.icon.region,
                                modifier = Modifier.size(16f),
                                tint = if (isSelected) colors.blue else colors.textTertiary
                            )
                            Text(
                                text = mode.title,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                font = typography.body,
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
                .margin(right = spacing.lg)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart
            ) {
                Button(
                    text = "Reload",
                    icon = Icon.refresh,
                    variant = ButtonVariant.PLAIN,
                    paddingH = spacing.md,
                    paddingV = spacing.xs + 1f,
                    onClick = onReload
                )

                Button(
                    text = "Preview",
                    icon = Icon.play,
                    variant = ButtonVariant.FILLED,
                    radius = shapes.sm,
                    paddingH = spacing.lg,
                    paddingV = spacing.xs + 1f,
                    onClick = onPreview
                )
            }
        }
    }
}
