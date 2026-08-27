package org.mdt.ui.screens.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.surface.Button
import org.mdt.ui.components.surface.ButtonVariant
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.model.EditorMode
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## EditorTopBar
 *
 * Apple macOS-inspired top navigation bar featuring centered [EditorMode] segmented switcher,
 * frosted glass styling, and high-definition internet studio glyphs.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorTopBar(
    currentMode: EditorMode = EditorMode.SCENE,
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
                    source = StudioIcons.STUDIO_LOGO,
                    modifier = Modifier.size(20f),
                    tint = colors.blue
                )
                MonoText(
                    text = "Neko Studio",
                    color = colors.textPrimary
                )
            }
        }

        // =====================================================================
        // 2. Center: Segmented Mode Switcher (True Screen Mathematical Dead-Center)
        // =====================================================================
        val dynamicTabWidth = remember(EditorMode.entries) {
            val font = typography.mono
            val layout = arc.graphics.g2d.GlyphLayout()
            val maxContentWidth = EditorMode.entries.maxOf { mode ->
                layout.setText(font, mode.title.uppercase())
                layout.width
            }
            maxOf(80f, maxContentWidth + spacing.md * 2f + 16f)
        }

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
                for (mode in EditorMode.entries) {
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .width(dynamicTabWidth)
                            .radius(shapes.sm)
                            .background(if (isSelected) colors.surfaceHighlight else Color.Clear)
                            .clickable { onSelectMode(mode) }
                            .pad(vertical = spacing.xs + 2f)
                    ) {
                        Row(
                            arrangement = Arrangement.Center,
                            alignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                source = mode.iconUrl,
                                modifier = Modifier.size(16f),
                                tint = if (isSelected) colors.blue else colors.textTertiary
                            )
                            Spacer(modifier = Modifier.width(spacing.xs + 2f))
                            MonoText(
                                text = mode.title,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary
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
                    icon = StudioIcons.REFRESH,
                    variant = ButtonVariant.PLAIN,
                    modifier = Modifier.pad(horizontal = spacing.md, vertical = spacing.xs + 1f),
                    onClick = onReload
                )

                Button(
                    text = "Preview",
                    icon = StudioIcons.PLAY,
                    variant = ButtonVariant.FILLED,
                    modifier = Modifier.radius(shapes.sm).pad(horizontal = spacing.lg, vertical = spacing.xs + 1f),
                    onClick = onPreview
                )
            }
        }
    }
}
