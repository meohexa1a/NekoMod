package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.theme.Theme

/**
 * ## ResizeGripHandle
 *
 * Professional 6-dot grip matrix displayed in the center of resizable pane splitters.
 * Highlights with tactile visual feedback upon hover and drag.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun ResizeGripHandle(
    isHovered: Boolean,
    isDragging: Boolean,
    modifier: UIModifier = UIModifier
) {
    if (!isHovered && !isDragging) return

    val colors = Theme.colors
    val shapes = Theme.shapes

    val pillBackground = if (isDragging) colors.blue else colors.surfaceElevated
    val dotColor = if (isDragging) Color.White else colors.textSecondary

    Box(
        modifier = Modifier
            .anchor(LayoutPreset.FULL_RECT)
            .then(modifier)
    ) {
        // 1. Vertical Accent Seam Line
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .width(2f)
                .fillMaxHeight()
                .background(if (isDragging) colors.blue else colors.borderActive)
        )

        // 2. Centered 6-Dot Grip Pill (2 columns × 3 rows)
        Box(
            modifier = Modifier
                .anchor(LayoutPreset.CENTER)
                .size(10f, 24f)
                .radius(shapes.xs)
                .background(pillBackground)
                .border(1f, if (isDragging) colors.blue else colors.borderHairline)
                .pad(horizontal = 2f, vertical = 3f)
        ) {
            Column(
                arrangement = Arrangement.spacedBy(3f),
                alignment = Alignment.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                for (row in 0..2) {
                    Row(
                        arrangement = Arrangement.spacedBy(2f),
                        alignment = Alignment.Center
                    ) {
                        for (col in 0..1) {
                            Box(
                                modifier = Modifier
                                    .size(2f, 2f)
                                    .radius(999f)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
