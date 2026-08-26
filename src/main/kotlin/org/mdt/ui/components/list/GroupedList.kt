@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.list

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.GlassMaterialPreset
import org.mdt.ui.theme.Theme
import org.mdt.ui.theme.glassMaterial

/**
 * ## GroupedList
 *
 * Apple iOS Inset Grouped List container.
 * Features an optional section header, frosted glass card body, and auto-spaced rows.
 *
 * @param header Optional section header label.
 * @param footer Optional section footer explanation.
 * @param modifier Chainable [UIModifier].
 * @param content Composable items block.
 */
@Composable
fun GroupedList(
    header: String? = null,
    footer: String? = null,
    modifier: UIModifier = UIModifier,
    content: @Composable () -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    Column(
        arrangement = Arrangement.spacedBy(6f),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth().then(modifier)
    ) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                color = colors.textTertiary,
                modifier = Modifier.pad(horizontal = 12f, vertical = 2f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassMaterial(
                    preset = GlassMaterialPreset.THIN,
                    radius = shapes.lg,
                    tint = colors.surfaceSecondary,
                    border = colors.borderHairline
                )
                .pad(vertical = 4f)
        ) {
            Column(
                arrangement = Arrangement.Start,
                alignment = Alignment.TopStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }

        if (footer != null) {
            Text(
                text = footer,
                color = colors.textQuaternary,
                modifier = Modifier.pad(horizontal = 12f, vertical = 2f)
            )
        }
    }
}
