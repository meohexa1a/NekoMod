package org.mdt.core.ui.graphics

import arc.util.Tmp

/**
 * ## Color (Zero-GC Inline Value Class)
 *
 * 64-bit packed immutable ARGB/sRGB color representation stored directly in CPU registers (Zero-GC).
 * Provides full math operations, linear color interpolation, alpha modification,
 * and seamless bridging to OpenGL raw floats and Arc engine primitives.
 *
 * Stored as a packed 64-bit unsigned long integer in ARGB order: `0xAARRGGBB_00000000UL`.
 *
 * See: docs/design-system/design_system_en.md
 */
@JvmInline
value class Color(val value: ULong) {

    /** Alpha channel component in 0.0f..1.0f range. */
    val a: Float get() = ((value shr 56) and 0xFFuL).toFloat() / 255.0f

    /** Red channel component in 0.0f..1.0f range. */
    val r: Float get() = ((value shr 48) and 0xFFuL).toFloat() / 255.0f

    /** Green channel component in 0.0f..1.0f range. */
    val g: Float get() = ((value shr 40) and 0xFFuL).toFloat() / 255.0f

    /** Blue channel component in 0.0f..1.0f range. */
    val b: Float get() = ((value shr 32) and 0xFFuL).toFloat() / 255.0f

    // Semantic alias properties
    val alpha: Float get() = a
    val red: Float get() = r
    val green: Float get() = g
    val blue: Float get() = b

    constructor(r: Float, g: Float, b: Float, a: Float = 1.0f) : this(
        pack(
            (a.coerceIn(0f, 1f) * 255.0f + 0.5f).toInt(),
            (r.coerceIn(0f, 1f) * 255.0f + 0.5f).toInt(),
            (g.coerceIn(0f, 1f) * 255.0f + 0.5f).toInt(),
            (b.coerceIn(0f, 1f) * 255.0f + 0.5f).toInt()
        )
    )

    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        pack(
            a.coerceIn(0, 255),
            r.coerceIn(0, 255),
            g.coerceIn(0, 255),
            b.coerceIn(0, 255)
        )
    )

    constructor(argbHex: UInt) : this((argbHex.toULong() and 0xFFFFFFFFuL) shl 32)

    /**
     * Returns a new Color with modified channel values (Zero-GC).
     */
    fun copy(
        r: Float = this.r,
        g: Float = this.g,
        b: Float = this.b,
        a: Float = this.a
    ): Color = Color(r, g, b, a)

    /**
     * Returns a copy with modified alpha channel (Zero-GC).
     */
    fun withAlpha(newAlpha: Float): Color = Color(r, g, b, newAlpha)

    /**
     * Smooth linear interpolation between this color and [target] with fraction [t] in 0.0f..1.0f (Zero-GC).
     */
    fun lerp(target: Color, t: Float): Color {
        val fraction = t.coerceIn(0f, 1f)
        val inv = 1.0f - fraction
        return Color(
            r = this.r * inv + target.r * fraction,
            g = this.g * inv + target.g * fraction,
            b = this.b * inv + target.b * fraction,
            a = this.a * inv + target.a * fraction
        )
    }

    /**
     * Multiplies RGB channels by [factor] (Zero-GC).
     */
    fun mul(factor: Float): Color = Color(
        r = (this.r * factor).coerceIn(0f, 1f),
        g = (this.g * factor).coerceIn(0f, 1f),
        b = (this.b * factor).coerceIn(0f, 1f),
        a = this.a
    )

    /**
     * Multiplies all RGBA channels by [target] color channels (Zero-GC).
     */
    fun mul(target: Color): Color = Color(
        r = (this.r * target.r).coerceIn(0f, 1f),
        g = (this.g * target.g).coerceIn(0f, 1f),
        b = (this.b * target.b).coerceIn(0f, 1f),
        a = (this.a * target.a).coerceIn(0f, 1f)
    )

    /**
     * Composite (alpha blend) this foreground color over [background] color (Zero-GC).
     */
    fun compositeOver(background: Color): Color {
        val srcA = this.a
        val dstA = background.a
        val outA = srcA + dstA * (1f - srcA)
        if (outA <= 0.0001f) return Clear

        val outR = (this.r * srcA + background.r * dstA * (1f - srcA)) / outA
        val outG = (this.g * srcA + background.g * dstA * (1f - srcA)) / outA
        val outB = (this.b * srcA + background.b * dstA * (1f - srcA)) / outA
        return Color(outR, outG, outB, outA)
    }

    /**
     * Computes relative luminance (0.0 = black, 1.0 = pure white).
     */
    fun luminance(): Float = 0.2126f * r + 0.7152f * g + 0.0722f * b

    /**
     * Converts to Arc color object using Anuke's pooled [arc.util.Tmp.c1] (Zero-GC).
     */
    fun toArcColor(target: arc.graphics.Color = Tmp.c1): arc.graphics.Color =
        target.set(r, g, b, a)

    override fun toString(): String =
        "Color(r=${(r * 255).toInt()}, g=${(g * 255).toInt()}, b=${(b * 255).toInt()}, a=${(a * 255).toInt()})"

    companion object {
        val Unspecified: Color = Color(0UL)
        val Transparent: Color = Color(0f, 0f, 0f, 0f)
        val Clear: Color = Color(0f, 0f, 0f, 0f)
        val White: Color = Color(1f, 1f, 1f, 1f)
        val Black: Color = Color(0f, 0f, 0f, 1f)
        val Red: Color = Color(1f, 0f, 0f, 1f)
        val Green: Color = Color(0f, 1f, 0f, 1f)
        val Blue: Color = Color(0f, 0f, 1f, 1f)
        val Yellow: Color = Color(1f, 1f, 0f, 1f)
        val Cyan: Color = Color(0f, 1f, 1f, 1f)
        val Magenta: Color = Color(1f, 0f, 1f, 1f)

        /** Creates a Compose [Color] from an Arc [arc.graphics.Color]. */
        fun fromArc(c: arc.graphics.Color): Color = Color(c.r, c.g, c.b, c.a)

        private fun pack(a: Int, r: Int, g: Int, b: Int): ULong =
            (((a and 0xFF).toULong() shl 56) or
             ((r and 0xFF).toULong() shl 48) or
             ((g and 0xFF).toULong() shl 40) or
             ((b and 0xFF).toULong() shl 32))

        /**
         * Parses a hex color string (#RGB, #RGBA, #RRGGBB, #RRGGBBAA, or raw hex without #).
         */
        fun parse(hex: String): Color {
            var s = hex.trim()
            if (s.startsWith("#")) s = s.substring(1)
            return try {
                when (s.length) {
                    3 -> { // RGB
                        val r = s.substring(0, 1).repeat(2).toInt(16)
                        val g = s.substring(1, 2).repeat(2).toInt(16)
                        val b = s.substring(2, 3).repeat(2).toInt(16)
                        Color(r, g, b, 255)
                    }
                    4 -> { // RGBA
                        val r = s.substring(0, 1).repeat(2).toInt(16)
                        val g = s.substring(1, 2).repeat(2).toInt(16)
                        val b = s.substring(2, 3).repeat(2).toInt(16)
                        val a = s.substring(3, 4).repeat(2).toInt(16)
                        Color(r, g, b, a)
                    }
                    6 -> { // RRGGBB
                        val r = s.substring(0, 2).toInt(16)
                        val g = s.substring(2, 4).toInt(16)
                        val b = s.substring(4, 6).toInt(16)
                        Color(r, g, b, 255)
                    }
                    8 -> { // RRGGBBAA
                        val r = s.substring(0, 2).toInt(16)
                        val g = s.substring(2, 4).toInt(16)
                        val b = s.substring(4, 6).toInt(16)
                        val a = s.substring(6, 8).toInt(16)
                        Color(r, g, b, a)
                    }
                    else -> White
                }
            } catch (_: Throwable) {
                White
            }
        }

        /**
         * ValueOf alias matching Arc/LibGDX syntax for seamless transition.
         */
        fun valueOf(hex: String): Color = parse(hex)
    }
}

/**
 * Top-level factory constructor for 64-bit Long hex values: `Color(0xFF0A84FFL)`.
 */
fun Color(argbHex: Long): Color = Color((argbHex.toULong() and 0xFFFFFFFFuL) shl 32)

/**
 * Extension bridging Arc mutable [arc.graphics.Color] to Compose immutable [Color].
 */
fun arc.graphics.Color.toComposeColor(): Color = Color(r, g, b, a)
