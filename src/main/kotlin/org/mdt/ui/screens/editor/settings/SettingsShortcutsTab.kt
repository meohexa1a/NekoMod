package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## SettingsShortcutsTab
 *
 * Comprehensive keyboard shortcuts cheat sheet and modifier reference for Neko Studio.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsShortcutsTab() {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.lg),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(arrangement = Arrangement.spacedBy(4f)) {
            Text(text = "Global Keyboard Shortcuts", font = typography.title, color = colors.textPrimary)
            Text(text = "Quick reference for hardware hotkeys, multi-tool toggles, and viewport gestures.", color = colors.textSecondary)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .radius(shapes.md)
                .background(colors.surfacePrimary)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Column(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Ctrl + Z" to "Undo the last in-memory mutation step",
                    "Ctrl + Y / Ctrl + Shift + Z" to "Redo the previously undone mutation step",
                    "Ctrl + 0" to "Reset viewport zoom to exactly 100% and center canvas",
                    "V" to "Switch to Select & Transform tool",
                    "R" to "Switch to Draw Box / Frame tool",
                    "T" to "Switch to Place Text Node tool",
                    "Del / Backspace" to "Delete the currently selected virtual node",
                    "Mouse Wheel" to "Focal-anchored canvas zoom (25% - 400%)",
                    "Mouse Drag (Empty Void)" to "Pan the 2D canvas viewport smoothly"
                ).forEach { (shortcut, description) ->
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.md),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(220f)
                                .radius(shapes.xs)
                                .background(colors.surfaceElevated)
                                .border(1f, colors.borderHairline)
                                .pad(horizontal = spacing.sm, vertical = 3f)
                        ) {
                            MonoText(text = shortcut, color = colors.blue)
                        }
                        MonoText(text = description, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
