package org.mdt.ui.screens.editor.settings

import androidx.compose.runtime.Composable
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
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## SettingsAboutTab
 *
 * Engine architecture details, OpenGL rendering pipeline specifications, and runtime version info.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SettingsAboutTab() {
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
            Text(text = "About NekoMod Studio", font = typography.title, color = colors.textPrimary)
            Text(text = "Next-generation declarative UI composer and design system for Mindustry.", color = colors.textSecondary)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        // Hero Badge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .radius(shapes.md)
                .background(colors.surfaceElevated)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.md),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44f, 44f)
                        .radius(shapes.md)
                        .background(colors.blue)
                        .pad(10f)
                ) {
                    Image(
                        source = StudioIcons.STUDIO_LOGO,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }
                Column(arrangement = Arrangement.spacedBy(2f)) {
                    MonoText(text = "NekoMod Virtual Studio Engine • v2.4.0", color = colors.textPrimary)
                    Text(text = "Pure In-Memory Virtual DOM • 100% Zero-GC 60 FPS • Dual-pass Gaussian Glassmorphism", color = colors.textTertiary)
                }
            }
        }

        // Specs Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .radius(shapes.md)
                .background(colors.surfacePrimary)
                .border(1f, colors.borderHairline)
                .pad(spacing.lg)
        ) {
            Column(arrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Layout Engine" to "Godot-inspired Box Model with Flex Weights & 9-Anchor Presets",
                    "Rendering Pipeline" to "OpenGL 2D SDF Shaders with Ping-Pong Dual FBO Glassmorphism",
                    "State Architecture" to "Reactive In-Memory Scene Graph Single Source of Truth (SSOT)",
                    "History Engine" to "Micro-Node Snapshotting with 60 FPS Drag Coalescing"
                ).forEach { (spec, detail) ->
                    Row(
                        arrangement = Arrangement.spacedBy(spacing.md),
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.width(180f)) {
                            MonoText(text = spec, color = colors.blue)
                        }
                        MonoText(text = detail, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
