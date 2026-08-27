package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.layout.SceneNode
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.Theme

/**
 * ## InspectorSceneSection
 *
 * Inspector controls for configuring Scene Artboard properties:
 * - Device Presets (1080p, 720p, Mobile, Modal)
 * - Artboard Dimensions (Width & Height in px)
 * - Artboard Background Fill Palette
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorSceneSection(node: SceneNode) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing

    // 1. Device Presets
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Device Presets", color = colors.textSecondary)
        Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "1080p" to "desktop_1080p",
                "720p" to "desktop_720p",
                "Mobile" to "mobile_portrait",
                "Modal" to "dialog_modal"
            ).forEach { (label, preset) ->
                val isSel = node.preset == preset
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .radius(shapes.xs)
                        .background(if (isSel) colors.blue else colors.surfaceSecondary)
                        .clickable { node.applyPreset(preset) }
                        .pad(vertical = 4f)
                ) {
                    MonoText(
                        text = label,
                        color = if (isSel) Color.White else colors.textSecondary
                    )
                }
            }
        }
    }

    // 2. Artboard Dimensions
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Artboard Dimensions", color = colors.textSecondary)
        Row(arrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "W: ${node.artboardWidth.toInt()} px", color = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1.0f), arrangement = Arrangement.spacedBy(2f)) {
                MonoText(text = "H: ${node.artboardHeight.toInt()} px", color = colors.textPrimary)
            }
        }
    }

    // 3. Background Fill Palette
    Column(arrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
        MonoText(text = "Artboard Background", color = colors.textSecondary)
        Row(arrangement = Arrangement.spacedBy(4f), modifier = Modifier.fillMaxWidth()) {
            listOf("#181926", "#0f1015", "#000000", "#1c1d22", "#2c3e55", "#ffffff").forEach { hex ->
                val col = Color.valueOf(hex)
                val isSel = node.backgroundColor == col
                Box(
                    modifier = Modifier
                        .size(24f, 24f)
                        .radius(shapes.xs)
                        .background(col)
                        .border(if (isSel) 2f else 1f, if (isSel) colors.blue else colors.borderHairline)
                        .clickable {
                            node.backgroundColor = col
                            node.ensureVisuals().background.color = col
                            node.invalidateLayout()
                        }
                )
            }
        }
    }
}
