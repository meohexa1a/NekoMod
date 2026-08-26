@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.input

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## SegmentedControl
 *
 * Apple iOS-style sliding pill tab selector.
 * Features an inset translucent track containing discrete options with a prominent
 * highlighted glass pill for the selected segment.
 *
 * @param items List of string labels for each segment.
 * @param selectedIndex Currently selected segment index (0-based).
 * @param onSelect Callback invoked with the newly selected index.
 * @param modifier Chainable [UIModifier].
 */
@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    // Outer inset capsule track
    Box(
        modifier = Modifier
            .radius(shapes.md)
            .background(colors.surfacePrimary)
            .border(width = 1f, color = colors.borderHairline)
            .pad(2f)
            .then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(2f),
            alignment = Alignment.CenterStart
        ) {
            for ((index, label) in items.withIndex()) {
                val isSelected = index == selectedIndex

                val segmentModifier = if (isSelected) {
                    Modifier
                        .weight(1f)
                        .radius(shapes.md - 1f)
                        .background(colors.glassThick)
                        .border(width = 1f, color = colors.borderRegular)
                        .shadow(color = colors.shadowAmbient, blur = 4f, spread = 1f)
                        .pad(horizontal = 12f, vertical = 6f)
                        .clickable { onSelect(index) }
                } else {
                    Modifier
                        .weight(1f)
                        .radius(shapes.md - 1f)
                        .pad(horizontal = 12f, vertical = 6f)
                        .clickable { onSelect(index) }
                }

                Box(
                    modifier = segmentModifier
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.textPrimary else colors.textTertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
