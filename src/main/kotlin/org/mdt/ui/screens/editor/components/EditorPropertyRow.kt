package org.mdt.ui.screens.editor.components

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.Theme

/**
 * ## EditorPropertyRow
 *
 * Standardized Inspector and Settings property row widget.
 * Formats a clean mono label, optional secondary value indicator, and interactive control slot.
 *
 * @param label Primary descriptor text.
 * @param subtitle Optional secondary value or unit text.
 * @param modifier Chainable [UIModifier].
 * @param content Interactive control composable content.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorPropertyRow(
    label: String,
    subtitle: String? = null,
    modifier: UIModifier = UIModifier,
    content: @Composable () -> Unit
) {
    val colors = Theme.colors
    val spacing = Theme.spacing

    Column(
        arrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier.fillMaxWidth().then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(spacing.sm),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            MonoText(
                text = label,
                color = colors.textSecondary
            )
            if (subtitle != null) {
                MonoText(
                    text = subtitle,
                    color = colors.textPrimary
                )
            }
        }
        content()
    }
}
