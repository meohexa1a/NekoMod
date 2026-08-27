package org.mdt.ui.components.list

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.GlassMaterialPreset
import org.mdt.ui.theme.Theme
import org.mdt.ui.theme.glassMaterial

/**
 * ## GroupedList
 *
 * Apple iOS/macOS Inset Grouped List container with Frosted Glass card background.
 * Supports both custom slot composables and text shortcuts for header and footer.
 *
 * @param modifier Chainable [UIModifier].
 * @param header Optional header composable slot.
 * @param footer Optional footer composable slot.
 * @param headerText Optional convenience header string label.
 * @param footerText Optional convenience footer string explanation.
 * @param content Declarative list items block.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun GroupedList(
    modifier: UIModifier = UIModifier,
    headerText: String? = null,
    footerText: String? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    Column(
        arrangement = Arrangement.spacedBy(6f),
        alignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth().then(modifier)
    ) {
        if (header != null) {
            Box(modifier = Modifier.pad(horizontal = 12f, vertical = 2f)) {
                header()
            }
        } else if (headerText != null) {
            Box(modifier = Modifier.pad(horizontal = 12f, vertical = 2f)) {
                MonoText(text = headerText, color = colors.textTertiary)
            }
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
            Box(modifier = Modifier.pad(horizontal = 12f, vertical = 2f)) {
                footer()
            }
        } else if (footerText != null) {
            Box(modifier = Modifier.pad(horizontal = 12f, vertical = 2f)) {
                Text(text = footerText, color = colors.textQuaternary)
            }
        }
    }
}

/**
 * Convenience String overload for [GroupedList].
 */
@Composable
fun GroupedList(
    header: String?,
    footer: String? = null,
    modifier: UIModifier = UIModifier,
    content: @Composable ColumnScope.() -> Unit
) = GroupedList(
    modifier = modifier,
    headerText = header,
    footerText = footer,
    content = content
)
