package org.mdt.ui.widgets

import arc.graphics.Color
import arc.graphics.Texture
import mindustry.graphics.Pal

/**
 * ## BoxVisuals
 *
 * Visual styling data container for [BoxNode].
 * Manages background colors, textures, borders, shadows, glow, and parameterized backdrop blur.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxVisuals {
    enum class BackgroundMode(val fillMode: Int) {
        SOLID(0), TEXTURE(1), BACKDROP(2)
    }

    enum class BorderStyle(val value: Int) {
        SOLID(0), DASHED(1), DOTTED(2)
    }

    enum class FilterMode(val value: Int) {
        NONE(0), GRAYSCALE(1), SEPIA(2), BRIGHTNESS(3), INVERT(4)
    }

    /** Visual opacity (0.0f = transparent, 1.0f = fully opaque). */
    var opacity: Float = 1.0f

    /** Top-left corner radius in pixels. */
    var topLeftRadius: Float = 8f
    /** Top-right corner radius in pixels. */
    var topRightRadius: Float = 8f
    /** Bottom-right corner radius in pixels. */
    var bottomRightRadius: Float = 8f
    /** Bottom-left corner radius in pixels. */
    var bottomLeftRadius: Float = 8f

    /** Border stroke width in pixels. */
    var borderWidth: Float = 0f
    /** Border stroke color. */
    var borderColor: Color = Color(Color.white)
    /** Border stroke pattern style. */
    var borderStyle: BorderStyle = BorderStyle.SOLID
    /** Dash length for dashed border style. */
    var dashLength: Float = 10f
    /** Dash to gap ratio for dashed border style. */
    var dashRatio: Float = 0.5f

    // ==========================================
    // BACKDROP BLUR PARAMETERS
    // ==========================================

    /** Whether backdrop blur is enabled for this box. */
    var blur: Boolean = false
    /** Blur kernel radius spread (pixels). */
    var blurRadius: Float = 4f
    /** Number of ping-pong blur convolution passes (1 = subtle, 4 = heavy frosted glass). */
    var blurIterations: Int = 2
    /** Alpha blending weight of the blurred backdrop over background color (0.0f to 1.0f). */
    var backdropWeight: Float = 0.8f
    /** Blending curve exponent (0.0f to 1.0f). */
    var backdropBlend: Float = 0.8f
    /** Minimum alpha threshold for backdrop rendering. */
    var backdropMinAlpha: Float = 0.8f
    /** Tint overlay color applied to the blurred backdrop texture. */
    var backdropTint: Color = Color(Color.white)

    // ==========================================
    // INNER SHADOW & GLOW
    // ==========================================

    var innerShadowColor: Color = Color(Pal.shadow)
    var innerShadowSpread: Float = 0f
    var innerShadowBlur: Float = 0f

    var glowColor: Color = Color(Pal.shadow)
    var glowSpread: Float = 0f
    var glowBlur: Float = 0f

    var filterMode: FilterMode = FilterMode.NONE
    var filterAmount: Float = 0f
    var noiseAmount: Float = 0f

    var uvScaleX: Float = 1.0f
    var uvScaleY: Float = 1.0f
    var uvOffsetX: Float = 0f
    var uvOffsetY: Float = 0f

    var fillColor: Color = Color(Color.darkGray)
    var fillTexture: Texture? = null
    var backgroundMode: BackgroundMode = BackgroundMode.SOLID

    // ==========================================
    // FLUENT BUILDER HELPERS
    // ==========================================

    fun radius(all: Float) {
        topLeftRadius = all; topRightRadius = all
        bottomLeftRadius = all; bottomRightRadius = all
    }

    fun radius(tl: Float, tr: Float, br: Float, bl: Float) {
        topLeftRadius = tl; topRightRadius = tr
        bottomRightRadius = br; bottomLeftRadius = bl
    }

    fun color(color: Color) {
        fillColor.set(color)
        backgroundMode = BackgroundMode.SOLID
    }

    fun border(width: Float, color: Color = Color.white, style: BorderStyle = BorderStyle.SOLID) {
        borderWidth = width
        borderColor.set(color)
        borderStyle = style
    }

    fun shadow(color: Color = Pal.shadow, spread: Float = 4f, blur: Float = 8f) {
        innerShadowColor.set(color)
        innerShadowSpread = spread
        innerShadowBlur = blur
    }

    fun glow(color: Color, spread: Float = 6f, blur: Float = 12f) {
        glowColor.set(color)
        glowSpread = spread
        glowBlur = blur
    }

    fun backdrop(
        blur: Boolean = true,
        radius: Float = 4f,
        weight: Float = 0.8f,
        blend: Float = 0.8f,
        tint: Color = Color.white,
        iterations: Int = 2
    ) {
        this.blur = blur
        this.blurRadius = radius
        this.blurIterations = iterations
        this.backdropWeight = weight
        this.backdropBlend = blend
        this.backdropTint.set(tint)
        this.backgroundMode = BackgroundMode.BACKDROP
    }

    fun texture(tex: Texture, scaleX: Float = 1.0f, scaleY: Float = 1.0f) {
        fillTexture = tex
        uvScaleX = scaleX
        uvScaleY = scaleY
        backgroundMode = BackgroundMode.TEXTURE
    }

    fun copyFrom(other: BoxVisuals) {
        opacity = other.opacity
        topLeftRadius = other.topLeftRadius
        topRightRadius = other.topRightRadius
        bottomRightRadius = other.bottomRightRadius
        bottomLeftRadius = other.bottomLeftRadius
        borderWidth = other.borderWidth
        borderColor.set(other.borderColor)
        borderStyle = other.borderStyle
        dashLength = other.dashLength
        dashRatio = other.dashRatio
        blur = other.blur
        blurRadius = other.blurRadius
        blurIterations = other.blurIterations
        backdropWeight = other.backdropWeight
        backdropBlend = other.backdropBlend
        backdropMinAlpha = other.backdropMinAlpha
        backdropTint.set(other.backdropTint)
        innerShadowColor.set(other.innerShadowColor)
        innerShadowSpread = other.innerShadowSpread
        innerShadowBlur = other.innerShadowBlur
        glowColor.set(other.glowColor)
        glowSpread = other.glowSpread
        glowBlur = other.glowBlur
        filterMode = other.filterMode
        filterAmount = other.filterAmount
        noiseAmount = other.noiseAmount
        uvScaleX = other.uvScaleX
        uvScaleY = other.uvScaleY
        uvOffsetX = other.uvOffsetX
        uvOffsetY = other.uvOffsetY
        fillColor.set(other.fillColor)
        fillTexture = other.fillTexture
        backgroundMode = other.backgroundMode
    }
}
