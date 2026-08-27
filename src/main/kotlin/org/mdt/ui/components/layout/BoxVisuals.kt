package org.mdt.ui.components.layout

import arc.graphics.Color
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import org.mdt.ui.components.display.image.ScaleMode

/**
 * ## CornerRadii
 *
 * Geometric corner radii standards for UI rectangular shapes (CSS `border-radius`).
 * Follows top-left, top-right, bottom-right, bottom-left clockwise ordering.
 */
data class CornerRadii(
    var topStart: Float = 0f,
    var topEnd: Float = 0f,
    var bottomEnd: Float = 0f,
    var bottomStart: Float = 0f
) {
    val topLeft: Float get() = topStart
    val topRight: Float get() = topEnd
    val bottomRight: Float get() = bottomEnd
    val bottomLeft: Float get() = bottomStart

    val isZero: Boolean get() = topStart <= 0.001f && topEnd <= 0.001f && bottomEnd <= 0.001f && bottomStart <= 0.001f

    fun set(uniform: Float) {
        topStart = uniform
        topEnd = uniform
        bottomEnd = uniform
        bottomStart = uniform
    }

    fun set(ts: Float, te: Float, be: Float, bs: Float) {
        topStart = ts
        topEnd = te
        bottomEnd = be
        bottomStart = bs
    }

    companion object {
        fun uniform(radius: Float) = CornerRadii(radius, radius, radius, radius)
    }
}

/**
 * ## Border
 *
 * Perimeter outline configuration (CSS `border`).
 */
data class Border(
    var width: Float = 0f,
    val color: Color = Color(Color.clear),
    var style: Style = Style.SOLID,
    var dashLength: Float = 8f,
    var dashRatio: Float = 0.5f
) {
    enum class Style(val value: Int) {
        SOLID(0),
        DASHED(1),
        DOTTED(2)
    }

    val isVisible: Boolean get() = width > 0.001f && color.a > 0.001f

    fun set(width: Float, color: Color, style: Style = Style.SOLID, dashLength: Float = 8f, dashRatio: Float = 0.5f) {
        this.width = width
        this.color.set(color)
        this.style = style
        this.dashLength = dashLength
        this.dashRatio = dashRatio
    }
}

/**
 * ## BoxShadow
 *
 * Diffuse ambient drop shadow, inset shadow, or radial glow (CSS `box-shadow`).
 */
data class BoxShadow(
    val color: Color = Color(Color.clear),
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var blur: Float = 0f,
    var spread: Float = 0f,
    var isInset: Boolean = false
) {
    val isVisible: Boolean get() = color.a > 0.001f && (blur > 0.001f || spread > 0.001f || offsetX != 0f || offsetY != 0f)

    fun set(color: Color, offsetX: Float = 0f, offsetY: Float = 0f, blur: Float = 8f, spread: Float = 0f, isInset: Boolean = false) {
        this.color.set(color)
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.blur = blur
        this.spread = spread
        this.isInset = isInset
    }
}

/**
 * ## BackdropFilter
 *
 * Multi-pass Gaussian backdrop blur and vibrancy blending (CSS `backdrop-filter`).
 */
data class BackdropFilter(
    var enabled: Boolean = false,
    var blurRadius: Float = 12f,
    var iterations: Int = 2,
    val tint: Color = Color(Color.white),
    var weight: Float = 1.0f,
    var blend: Float = 0.5f,
    var minAlpha: Float = 0.1f
) {
    val isVisible: Boolean get() = enabled && (blurRadius > 0.001f || weight > 0.001f)
}

/**
 * ## BackgroundFill
 *
 * Solid color, image texture, or backdrop source fill (CSS `background`).
 */
class BackgroundFill {
    enum class Mode(val fillMode: Int) {
        COLOR(0),
        TEXTURE(1),
        BACKDROP(2)
    }

    var mode: Mode = Mode.COLOR
    val color: Color = Color(Color.clear)
    var texture: Texture? = null
    var region: TextureRegion? = null
    var scaleMode: ScaleMode = ScaleMode.FIT
    var uvScaleX: Float = 1f
    var uvScaleY: Float = 1f
    var uvOffsetX: Float = 0f
    var uvOffsetY: Float = 0f
    var uvMinX: Float = 0f
    var uvMinY: Float = 0f
    var uvMaxX: Float = 1f
    var uvMaxY: Float = 1f

    val isVisible: Boolean get() = when (mode) {
        Mode.COLOR -> color.a > 0.001f
        Mode.TEXTURE -> region != null || texture != null
        Mode.BACKDROP -> true
    }

    fun computeUVs(w: Float, h: Float) {
        val reg = region
        if (reg == null) {
            uvScaleX = 1f
            uvScaleY = 1f
            uvOffsetX = 0f
            uvOffsetY = 0f
            uvMinX = 0f
            uvMinY = 0f
            uvMaxX = 1f
            uvMaxY = 1f
            return
        }

        val rw = reg.width.toFloat()
        val rh = reg.height.toFloat()
        if (rw <= 0f || rh <= 0f || w <= 0f || h <= 0f) return

        val u1 = reg.u
        val u2 = reg.u2
        val vTop = reg.v
        val vBottom = reg.v2

        uvMinX = minOf(u1, u2)
        uvMaxX = maxOf(u1, u2)
        uvMinY = minOf(vTop, vBottom)
        uvMaxY = maxOf(vTop, vBottom)

        val du = u2 - u1
        val dv = vTop - vBottom

        when (scaleMode) {
            ScaleMode.STRETCH -> {
                uvScaleX = du
                uvScaleY = dv
                uvOffsetX = u1
                uvOffsetY = vBottom
            }
            ScaleMode.CROP -> {
                val boxRatio = w / h
                val texRatio = rw / rh
                if (boxRatio > texRatio) {
                    val s = (h / w) / (rh / rw)
                    val cropV = dv * s
                    val offsetV = vBottom + dv * (1f - s) * 0.5f
                    uvScaleX = du
                    uvScaleY = cropV
                    uvOffsetX = u1
                    uvOffsetY = offsetV
                } else {
                    val s = (w / h) / (rw / rh)
                    val cropU = du * s
                    val offsetU = u1 + du * (1f - s) * 0.5f
                    uvScaleX = cropU
                    uvScaleY = dv
                    uvOffsetX = offsetU
                    uvOffsetY = vBottom
                }
            }
            ScaleMode.FIT -> {
                val boxRatio = w / h
                val texRatio = rw / rh
                if (boxRatio > texRatio) {
                    val s = (w / h) / (rw / rh)
                    val scaledU = du * s
                    val offsetU = u1 - du * (s - 1f) * 0.5f
                    uvScaleX = scaledU
                    uvScaleY = dv
                    uvOffsetX = offsetU
                    uvOffsetY = vBottom
                } else {
                    val s = (h / w) / (rh / rw)
                    val scaledV = dv * s
                    val offsetV = vBottom - dv * (s - 1f) * 0.5f
                    uvScaleX = du
                    uvScaleY = scaledV
                    uvOffsetX = u1
                    uvOffsetY = offsetV
                }
            }
            ScaleMode.CENTER -> {
                val scaleX = w / rw
                val scaleY = h / rh
                val scaledU = du * scaleX
                val scaledV = dv * scaleY
                val offsetU = u1 - du * (scaleX - 1f) * 0.5f
                val offsetV = vBottom - dv * (scaleY - 1f) * 0.5f
                uvScaleX = scaledU
                uvScaleY = scaledV
                uvOffsetX = offsetU
                uvOffsetY = offsetV
            }
        }
    }
}

/**
 * ## ProgressFill
 *
 * SDF linear progress bar filling state.
 */
data class ProgressFill(
    var fraction: Float = 0f,
    val color: Color = Color(Color.clear)
) {
    val isVisible: Boolean get() = fraction > 0.001f && color.a > 0.001f
}

/**
 * ## ColorFilter
 *
 * Post-processing color filter and noise adjustments.
 */
data class ColorFilter(
    var mode: Mode = Mode.NONE,
    var amount: Float = 0f,
    var noiseAmount: Float = 0f
) {
    enum class Mode(val value: Int) {
        NONE(0),
        GRAYSCALE(1),
        SEPIA(2),
        INVERT(3)
    }

    val isVisible: Boolean get() = (mode != Mode.NONE && amount > 0.001f) || noiseAmount > 0.001f
}

/**
 * ## BoxVisuals
 *
 * Structured visual presentation model inspired by the CSS Box Model and modern design tokens.
 * Encapsulates [BackgroundFill], [CornerRadii], [Border], [BoxShadow], [BackdropFilter], and [ProgressFill].
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxVisuals {

    // =========================================================================
    // I. Visual Sub-Models (CSS Box Model Architecture)
    // =========================================================================

    val background: BackgroundFill = BackgroundFill()
    val radii: CornerRadii = CornerRadii()
    val border: Border = Border()
    val shadow: BoxShadow = BoxShadow()
    val innerShadow: BoxShadow = BoxShadow(isInset = true)
    val glow: BoxShadow = BoxShadow()
    val backdrop: BackdropFilter = BackdropFilter()
    val progress: ProgressFill = ProgressFill()
    val filter: ColorFilter = ColorFilter()

    /** Master opacity multiplier for all visual passes (0.0f..1.0f). */
    var opacity: Float = 1.0f

    // =========================================================================
    // II. Visibility & Invalidation
    // =========================================================================

    fun isVisible(): Boolean {
        if (opacity <= 0.001f) return false
        return background.isVisible ||
                border.isVisible ||
                shadow.isVisible ||
                innerShadow.isVisible ||
                glow.isVisible ||
                backdrop.isVisible ||
                progress.isVisible
    }

    // =========================================================================
    // III. Fluent Zero-GC Mutation Helpers
    // =========================================================================

    fun radius(uniform: Float): BoxVisuals {
        radii.set(uniform)
        return this
    }

    fun radius(topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float): BoxVisuals {
        radii.set(topStart, topEnd, bottomEnd, bottomStart)
        return this
    }

    fun border(width: Float, color: Color, style: Border.Style = Border.Style.SOLID): BoxVisuals {
        border.set(width, color, style)
        return this
    }

    fun shadow(
        color: Color,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        blur: Float = 8f,
        spread: Float = 0f
    ): BoxVisuals {
        shadow.set(color, offsetX, offsetY, blur, spread, isInset = false)
        return this
    }

    fun innerShadow(
        color: Color,
        blur: Float = 8f,
        spread: Float = 0f
    ): BoxVisuals {
        innerShadow.set(color, 0f, 0f, blur, spread, isInset = true)
        return this
    }

    fun glow(color: Color, spread: Float = 6f, blur: Float = 12f): BoxVisuals {
        glow.set(color, 0f, 0f, blur, spread, isInset = false)
        return this
    }

    fun backdrop(blur: Boolean = true, blurRadius: Float = 12f): BoxVisuals {
        backdrop.enabled = blur
        backdrop.blurRadius = blurRadius
        background.mode = BackgroundFill.Mode.BACKDROP
        return this
    }

    fun texture(
        region: TextureRegion,
        scaleMode: ScaleMode = ScaleMode.FIT,
        tint: Color = Color.white
    ): BoxVisuals {
        background.mode = BackgroundFill.Mode.TEXTURE
        background.region = region
        background.texture = region.texture
        background.scaleMode = scaleMode
        background.color.set(tint)
        return this
    }

    fun texture(
        tex: Texture,
        scaleMode: ScaleMode = ScaleMode.FIT,
        tint: Color = Color.white
    ): BoxVisuals = texture(TextureRegion(tex), scaleMode, tint)

    fun progress(fraction: Float, color: Color): BoxVisuals {
        progress.fraction = fraction.coerceIn(0f, 1f)
        progress.color.set(color)
        return this
    }
}
