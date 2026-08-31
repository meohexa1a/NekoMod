// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.theme

import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.Insets

/**
 * ## ThemeTokens
 *
 * Centralized design tokens and default color palettes for UI components and surfaces.
 *
 * @property background Global background base color.
 * @property surface Standard card/panel surface color.
 * @property surfaceVariant Elevated surface layer color.
 * @property surfaceModal High-opacity scrim surface color for dialogs.
 * @property primary Primary brand accent color.
 * @property primaryVariant Darker shade of primary brand color.
 * @property accent Secondary highlight / accent color.
 * @property border Standard surface border stroke color.
 * @property borderSubtle Muted border stroke color for nested elements.
 * @property borderFocus Highlighted border stroke color for active focus states.
 * @property textPrimary High-emphasis primary text color.
 * @property textSecondary Medium-emphasis secondary text color.
 * @property textMuted Low-emphasis disabled or hint text color.
 *
 * @see CardDefaults
 * @see ButtonDefaults
 * @see TextFieldDefaults
 */
object ThemeTokens {
    val background: Color = Color(0.06f, 0.06f, 0.09f, 0.95f)
    val surface: Color = Color(0.12f, 0.12f, 0.18f, 0.45f)
    val surfaceVariant: Color = Color(0.16f, 0.16f, 0.24f, 0.55f)
    val surfaceModal: Color = Color(0.10f, 0.10f, 0.15f, 0.85f)

    val primary: Color = Color(0.38f, 0.50f, 0.95f, 1.0f)
    val primaryVariant: Color = Color(0.30f, 0.40f, 0.85f, 1.0f)
    val accent: Color = Color(0.95f, 0.45f, 0.65f, 1.0f)

    val border: Color = Color(1.0f, 1.0f, 1.0f, 0.18f)
    val borderSubtle: Color = Color(1.0f, 1.0f, 1.0f, 0.08f)
    val borderFocus: Color = Color(0.45f, 0.60f, 1.0f, 0.60f)

    val textPrimary: Color = Color(1.0f, 1.0f, 1.0f, 1.0f)
    val textSecondary: Color = Color(0.70f, 0.70f, 0.78f, 1.0f)
    val textMuted: Color = Color(0.45f, 0.45f, 0.55f, 1.0f)
}

/**
 * ## CardDefaults
 *
 * Default visual styling configuration for [org.mdt.ui.components.surface.Card].
 *
 * @property color Default card surface background color.
 * @property borderColor Default card outline border color.
 * @property radius Default corner radius in pixels.
 * @property borderWidth Default outline border stroke thickness in pixels.
 */
object CardDefaults {
    val color: Color get() = ThemeTokens.surface
    val borderColor: Color get() = ThemeTokens.border
    const val radius: Float = 16.0f
    const val borderWidth: Float = 1.0f
}

/**
 * ## ButtonDefaults
 *
 * Default visual styling and layout insets for [org.mdt.ui.components.surface.Button].
 *
 * @property backgroundColor Default button background fill color.
 * @property borderColor Default button outline border stroke color.
 * @property contentPadding Default inner padding insets.
 * @property radius Default corner radius in pixels.
 * @property borderWidth Default outline border stroke thickness in pixels.
 */
object ButtonDefaults {
    val backgroundColor: Color get() = ThemeTokens.surfaceVariant
    val borderColor: Color get() = ThemeTokens.border
    val contentPadding: Insets get() = Insets(left = 16.0f, top = 8.0f, right = 16.0f, bottom = 8.0f)
    const val radius: Float = 8.0f
    const val borderWidth: Float = 1.0f
}

/**
 * ## TextFieldDefaults
 *
 * Default visual styling configuration for [org.mdt.ui.components.input.TextField].
 *
 * @property backgroundColor Default text input box container background color.
 * @property borderColor Default text input box outline border color.
 * @property borderWidth Default outline border stroke thickness in pixels.
 * @property radius Default corner radius in pixels.
 */
object TextFieldDefaults {
    val backgroundColor: Color get() = Color(0.08f, 0.08f, 0.12f, 0.70f)
    val borderColor: Color get() = Color(1.0f, 1.0f, 1.0f, 0.15f)
    const val borderWidth: Float = 1.0f
    const val radius: Float = 8.0f
}
