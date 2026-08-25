@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import arc.graphics.Color
import org.mdt.ui.components.layout.Box
import org.mdt.ui.compose.UIModifier
import org.mdt.ui.compose.anchor
import org.mdt.ui.compose.background
import org.mdt.ui.compose.clickable
import org.mdt.ui.compose.cornerRadius
import org.mdt.ui.compose.size
import org.mdt.ui.layout.LayoutPreset

/**
 * ## Toggle
 *
 * Modern smooth pill switch toggle component.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
    activeColor: Color = Color.valueOf("2563eb"),
    inactiveColor: Color = Color.valueOf("363a4f"),
    thumbColor: Color = Color.white
) {
    val trackBg = if (checked) activeColor else inactiveColor

    Box(
        modifier = UIModifier
            .size(44f, 24f)
            .cornerRadius(12f)
            .background(trackBg)
            .clickable { onToggle(!checked) }
            .then(modifier)
    ) {
        val knobAnchor = if (checked) LayoutPreset.RIGHT_WIDE else LayoutPreset.LEFT_WIDE

        Box(
            modifier = UIModifier
                .size(18f, 18f)
                .anchor(knobAnchor)
                .cornerRadius(9f)
                .background(thumbColor)
        )
    }
}
