package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.*
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
 * ## SettingsGpuTab
 *
 * Performance settings for GPU shaders, dual-pass Gaussian blur FBOs, and memory retention bounds.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsGpuTab() {
    var selectedBlurIterations by remember { mutableStateOf(2) }
    var selectedHistoryLimit by remember { mutableStateOf(50) }
    var selectedVramLimit by remember { mutableStateOf(64) }

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
            Text(text = "GPU Shaders & Memory Performance", font = typography.title, color = colors.textPrimary)
            Text(text = "Fine-tune hardware shader pipelines, dual-pass Gaussian blur passes, and memory retention limits.", color = colors.textSecondary)
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
            Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
                // Frosted Glass Blur Iterations
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "Frosted Glass Blur Quality", color = colors.textPrimary)
                        Text(text = "Number of dual-pass Gaussian blur ping-pong FBO downsample passes.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(4f)) {
                        listOf(
                            1 to "1x (Fast)",
                            2 to "2x (Optimal)",
                            3 to "3x (Ultra)"
                        ).forEach { (iter, label) ->
                            val isSel = selectedBlurIterations == iter
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable { selectedBlurIterations = iter }
                                    .pad(horizontal = spacing.md, vertical = 4f)
                            ) {
                                MonoText(text = label, color = if (isSel) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // Undo / Redo History Capacity
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "In-Memory Undo History Stack Limit", color = colors.textPrimary)
                        Text(text = "Maximum discrete in-memory mutation steps retained in history buffer.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(4f)) {
                        listOf(25, 50, 100, 200).forEach { limit ->
                            val isSel = selectedHistoryLimit == limit
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable { selectedHistoryLimit = limit }
                                    .pad(horizontal = spacing.md, vertical = 4f)
                            ) {
                                MonoText(text = "$limit steps", color = if (isSel) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth().height(1f))

                // VRAM Texture Cache Trim
                Row(
                    arrangement = Arrangement.spacedBy(spacing.md),
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                        MonoText(text = "VRAM Texture LRU Cache Limit", color = colors.textPrimary)
                        Text(text = "Maximum memory threshold before automatically evicting cached icon/image textures.", color = colors.textSecondary)
                    }
                    Row(arrangement = Arrangement.spacedBy(4f)) {
                        listOf(32, 64, 128, 256).forEach { mb ->
                            val isSel = selectedVramLimit == mb
                            Box(
                                modifier = Modifier
                                    .radius(shapes.xs)
                                    .background(if (isSel) colors.blue else colors.surfaceSecondary)
                                    .border(1f, if (isSel) colors.blue else colors.borderHairline)
                                    .clickable { selectedVramLimit = mb }
                                    .pad(horizontal = spacing.md, vertical = 4f)
                            ) {
                                MonoText(text = "$mb MB", color = if (isSel) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
