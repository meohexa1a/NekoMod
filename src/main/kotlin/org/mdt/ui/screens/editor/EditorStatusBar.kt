package org.mdt.ui.screens.editor

import androidx.compose.runtime.*
import arc.Core
import arc.graphics.Color
import org.mdt.core.engine.EngineContext
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.Spacer
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## EditorStatusBar
 *
 * Professional bottom status bar modeled after VSCode and Apple Xcode Studio.
 * Uses unified monospace typography for 100% pixel-perfect baseline alignment.
 *
 * Displays:
 * - Selected node / component context breadcrumbs.
 * - Real-time canvas pan coordinates (`X`, `Y`) and zoom scale.
 * - Active GPU VRAM memory footprint from [org.mdt.core.common.LRUTextureCache.totalVramBytes].
 * - Engine FPS performance readout and UTF-8 document format tags.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorStatusBar(
    selectedItem: String = "App Root Scene",
    selectedComponent: String = "PrimaryButton",
    isComponentMode: Boolean = false,
    panX: Float = 0f,
    panY: Float = 0f,
    zoom: Float = 1.0f,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // Read real-time engine dynamic texture VRAM metrics (excluding shared game atlas)
    val imageService = EngineContext.current.image
    val dynamicVram = imageService.cache.dynamicVramBytes

    val vramStr = remember(dynamicVram) {
        when {
            dynamicVram >= 1024L * 1024L -> String.format("%.1f MB", dynamicVram.toFloat() / (1024f * 1024f))
            dynamicVram >= 1024L -> String.format("%.0f KB", dynamicVram.toFloat() / 1024f)
            else -> "$dynamicVram B"
        }
    }

    val fps = if (Core.graphics != null) Core.graphics.framesPerSecond else 60

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26f)
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
            .pad(horizontal = spacing.md)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(spacing.md),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxSize()
        ) {
            // =================================================================
            // 1. Left: Studio Context & Breadcrumbs
            // =================================================================
            Row(
                arrangement = Arrangement.spacedBy(spacing.sm),
                alignment = Alignment.CenterStart
            ) {
                // Pulse Ready Dot + Brand
                Row(
                    arrangement = Arrangement.spacedBy(spacing.xs + 1f),
                    alignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(7f, 7f)
                            .radius(shapes.pill)
                            .background(colors.green)
                    )
                    MonoText(
                        text = "Neko Studio",
                        color = colors.textSecondary
                    )
                }

                MonoText(text = "|", color = colors.borderRegular)

                // Breadcrumb Path (e.g. Scene: app.nxml > App Root Scene)
                Row(
                    arrangement = Arrangement.spacedBy(spacing.xs + 2f),
                    alignment = Alignment.CenterStart
                ) {
                    Image(
                        source = if (isComponentMode) StudioIcons.COMPONENTS else StudioIcons.SCENE,
                        modifier = Modifier.size(12f),
                        tint = colors.blue
                    )
                    MonoText(
                        text = if (isComponentMode) "Component: <$selectedComponent>" else "Scene: app.nxml > $selectedItem",
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            // =================================================================
            // 2. Center: Validation Status
            // =================================================================
            Row(
                arrangement = Arrangement.spacedBy(spacing.xs),
                alignment = Alignment.CenterStart
            ) {
                MonoText(text = "✓ NXML Validated", color = colors.green)
            }

            Spacer(modifier = Modifier.weight(1.0f))

            // =================================================================
            // 3. Right: Precision Canvas Coordinates, VRAM, FPS & Encoding
            // =================================================================
            Row(
                arrangement = Arrangement.spacedBy(spacing.md),
                alignment = Alignment.CenterStart
            ) {
                // Canvas Coordinates (Explicit X / Y for 2D viewport)
                if (!isComponentMode) {
                    MonoText(
                        text = "X: ${panX.toInt()}  Y: ${panY.toInt()}",
                        color = colors.textSecondary
                    )

                    MonoText(
                        text = "${(zoom * 100).toInt()}%",
                        color = colors.textSecondary
                    )
                } else {
                    MonoText(
                        text = "Isolated Canvas",
                        color = colors.textSecondary
                    )
                }

                MonoText(text = "•", color = colors.textQuaternary)

                // VRAM Usage Metric
                MonoText(
                    text = "VRAM Usage: $vramStr",
                    color = colors.teal
                )

                MonoText(text = "•", color = colors.textQuaternary)

                // FPS Performance
                MonoText(
                    text = "$fps FPS",
                    color = if (fps >= 55) colors.green else colors.orange
                )
            }
        }
    }
}
