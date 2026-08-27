package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.screens.editor.components.EditorToggleRow
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SettingsGeneralTab
 *
 * General studio settings for UI accents, typography preferences, and workspace behaviors.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsGeneralTab() {
    var selectedAccent by remember { mutableStateOf("#0a84ff") }
    var enableVramMonitor by remember { mutableStateOf(true) }
    var enableFpsCounter by remember { mutableStateOf(true) }

    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.lg),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Column(arrangement = Arrangement.spacedBy(4f)) {
            Text(text = "General Preferences", font = typography.title, color = colors.textPrimary)
            Text(text = "Customize studio appearance, telemetry displays, and workspace theme.", color = colors.textSecondary)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        // Accent Color Palette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .radius(shapes.md)
                .background(colors.surfacePrimary)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "Studio System Accent", color = colors.textPrimary)
                        Text(text = "Primary highlight color for gizmo bounds, selections, and active buttons.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(6f)) {
                        listOf(
                            "#0a84ff" to "Blue",
                            "#bf5af2" to "Purple",
                            "#30d158" to "Green",
                            "#ff9f0a" to "Orange",
                            "#ff375f" to "Pink"
                        ).forEach { (hex, _) ->
                            val col = Color.valueOf(hex)
                            val isSel = selectedAccent == hex
                            Box(
                                modifier = Modifier
                                    .size(28f, 28f)
                                    .radius(shapes.xs)
                                    .background(col)
                                    .border(if (isSel) 2f else 1f, if (isSel) Color.White else colors.borderHairline)
                                    .clickable { selectedAccent = hex }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Telemetry & Monitoring
                EditorToggleRow(
                    title = "Real-Time FPS Telemetry",
                    description = "Display live frames-per-second monitor on the bottom status bar.",
                    checked = enableFpsCounter,
                    onToggle = { enableFpsCounter = it }
                )

                EditorToggleRow(
                    title = "VRAM Memory Telemetry",
                    description = "Display GPU VRAM and texture memory allocations on the bottom status bar.",
                    checked = enableVramMonitor,
                    onToggle = { enableVramMonitor = it }
                )
            }
        }
    }
}
