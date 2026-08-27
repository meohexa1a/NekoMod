package org.mdt.ui.screens.editor.components

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.Theme
import kotlin.math.abs

/**
 * ## EditorOptionGroup
 *
 * Generic, type-safe option button group for Studio Inspector and Settings panels.
 * Renders compact tactile chips with dynamic selection highlighting and border accents.
 * Includes built-in fuzzy float tolerance to maintain active highlights during live canvas dragging.
 *
 * @param options List of candidate values.
 * @param selected Currently selected candidate value.
 * @param onSelect Callback invoked on click.
 * @param labelSelector Custom string mapper for rendering each option's label.
 * @param isSelectedMatcher Optional custom matcher lambda for complex equality checks.
 * @param modifier Chainable [UIModifier].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun <T> EditorOptionGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelSelector: (T) -> String = { it.toString() },
    isSelectedMatcher: ((option: T, selected: T) -> Boolean)? = null,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    Row(
        arrangement = Arrangement.spacedBy(3f),
        alignment = Alignment.CenterStart,
        modifier = modifier
    ) {
        for (option in options) {
            val isSelected = when {
                isSelectedMatcher != null -> isSelectedMatcher(option, selected)
                option is Float && selected is Float -> abs(option - selected) < 1.0f
                option is Double && selected is Double -> abs(option - selected) < 1.0
                else -> option == selected
            }

            Box(
                modifier = Modifier
                    .radius(shapes.xs)
                    .background(if (isSelected) colors.surfaceHighlight else colors.surfaceSecondary)
                    .border(1f, if (isSelected) colors.blue else colors.borderHairline)
                    .clickable { onSelect(option) }
                    .pad(horizontal = 6f, vertical = 2f)
            ) {
                MonoText(
                    text = labelSelector(option),
                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}

