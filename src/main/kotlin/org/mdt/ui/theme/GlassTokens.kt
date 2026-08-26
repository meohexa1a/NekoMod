package org.mdt.ui.theme

import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## GlassMaterialPreset
 *
 * Predefined multi-pass Gaussian backdrop blur and vibrancy tints matching Apple iOS & Modern Web design.
 * Scaled gently around CSS `backdrop-filter: blur(2px .. 6px)`.
 */
enum class GlassMaterialPreset(
    val blurRadius: Float,
    val weight: Float,
    val blend: Float,
    val tintAlpha: Float,
    val iterations: Int
) {
    /** Ultra-subtle gentle blur for navigation bars (~2px web blur). */
    ULTRA_THIN(blurRadius = 2.0f, weight = 0.7f, blend = 0.7f, tintAlpha = 0.08f, iterations = 1),

    /** Standard subtle glass for segmented controls (~3.5px web blur). */
    THIN(blurRadius = 3.5f, weight = 0.85f, blend = 0.80f, tintAlpha = 0.14f, iterations = 1),

    /** Regular crisp frosted glass for cards and panels (~5.0px web blur). */
    REGULAR(blurRadius = 5.0f, weight = 0.95f, blend = 0.88f, tintAlpha = 0.20f, iterations = 1),

    /** Prominent glass for modal dialogs and alert popups (~8.0px web blur). */
    THICK(blurRadius = 8.0f, weight = 1.0f, blend = 0.95f, tintAlpha = 0.28f, iterations = 1)
}

/**
 * Applies Apple-style Frosted Glass backdrop blur with hairline specular border.
 *
 * @param preset Material blur and weight preset.
 * @param radius Corner radius in pixels (default 16f).
 * @param tint Custom glass tint color (defaults to Apple dark glass tone).
 * @param border Custom specular highlight border color.
 * @param shadow Ambient drop shadow color.
 */
fun UIModifier.glassMaterial(
    preset: GlassMaterialPreset = GlassMaterialPreset.REGULAR,
    radius: Float = 16f,
    tint: Color = Color(0.08f, 0.09f, 0.13f, preset.tintAlpha),
    border: Color = Color(1f, 1f, 1f, 0.12f),
    shadow: Color = Color(0f, 0f, 0f, 0.35f)
): UIModifier = this
    .radius(radius)
    .background(tint)
    .backdrop(
        blur = true,
        blurRadius = preset.blurRadius,
        weight = preset.weight,
        blend = preset.blend,
        tint = tint,
        iterations = preset.iterations
    )
    .border(width = 1f, color = border, style = org.mdt.ui.components.layout.Border.Style.SOLID)
    .shadow(
        color = shadow,
        offsetX = 0f,
        offsetY = -2f,
        blur = 12f,
        spread = 2f
    )
