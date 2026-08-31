// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.render

import arc.util.Tmp

/**
 * ## Color
 *
 * Packed 64-bit unsigned integer color stored in CPU registers without heap allocations (Zero-GC).
 * Provides color math, linear interpolation (`lerp`), alpha blending, hex parsing, and OpenGL vertex packing.
 * Deeply integrates with Arc Graphics ([arc.graphics.Color], [Tmp.c1]) for maximum performance.
 *
 * @property value Packed 64-bit ARGB unsigned integer value.
 * @property alpha Alpha channel component in `0.0f..1.0f` range.
 * @property red Red channel component in `0.0f..1.0f` range.
 * @property green Green channel component in `0.0f..1.0f` range.
 * @property blue Blue channel component in `0.0f..1.0f` range.
 *
 * @see org.mdt.core.platform.render.UIBatch
 * @see arc.graphics.Color
 */
@JvmInline
value class Color(val value: ULong) {

    // --- PROPERTIES & CHANNEL ACCESSORS ---

    /** Alpha channel component in 0.0f..1.0f range. */
    val alpha: Float get() = ((value shr 56) and 0xFFuL).toFloat() / 255.0f

    /** Red channel component in 0.0f..1.0f range. */
    val red: Float get() = ((value shr 48) and 0xFFuL).toFloat() / 255.0f

    /** Green channel component in 0.0f..1.0f range. */
    val green: Float get() = ((value shr 40) and 0xFFuL).toFloat() / 255.0f

    /** Blue channel component in 0.0f..1.0f range. */
    val blue: Float get() = ((value shr 32) and 0xFFuL).toFloat() / 255.0f

    // Short property aliases for mathematical expressions
    val a: Float get() = alpha
    val r: Float get() = red
    val g: Float get() = green
    val b: Float get() = blue

    // --- CONSTRUCTORS ---

    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1.0f) : this(
        pack(
            (alpha.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt(),
            (red.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt(),
            (green.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt(),
            (blue.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt()
        )
    )

    constructor(red: Int, green: Int, blue: Int, alpha: Int = 255) : this(
        pack(
            alpha.coerceIn(0, 255),
            red.coerceIn(0, 255),
            green.coerceIn(0, 255),
            blue.coerceIn(0, 255)
        )
    )

    constructor(argbHex: UInt) : this((argbHex.toULong() and 0xFFFFFFFFuL) shl 32)

    // --- COLOR TRANSFORMATIONS & MATH ---

    /**
     * Returns a new Color with modified channel values (Zero-GC).
     */
    fun copy(
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
        alpha: Float = this.alpha
    ): Color = Color(red, green, blue, alpha)

    /**
     * Returns a copy with modified alpha channel (Zero-GC).
     */
    fun withAlpha(newAlpha: Float): Color = Color(red, green, blue, newAlpha)

    /**
     * Smooth linear interpolation between this color and [target] with [fraction] in 0.0f..1.0f (Zero-GC).
     */
    fun lerp(target: Color, fraction: Float): Color {
        val clampedFraction = fraction.coerceIn(0.0f, 1.0f)
        val inverseFraction = 1.0f - clampedFraction
        return Color(
            red = this.red * inverseFraction + target.red * clampedFraction,
            green = this.green * inverseFraction + target.green * clampedFraction,
            blue = this.blue * inverseFraction + target.blue * clampedFraction,
            alpha = this.alpha * inverseFraction + target.alpha * clampedFraction
        )
    }

    /**
     * Multiplies RGB channels by [factor] (Zero-GC).
     */
    fun mul(factor: Float): Color = Color(
        red = (this.red * factor).coerceIn(0.0f, 1.0f),
        green = (this.green * factor).coerceIn(0.0f, 1.0f),
        blue = (this.blue * factor).coerceIn(0.0f, 1.0f),
        alpha = this.alpha
    )

    /**
     * Multiplies all RGBA channels by [target] color channels (Zero-GC).
     */
    fun mul(target: Color): Color = Color(
        red = (this.red * target.red).coerceIn(0.0f, 1.0f),
        green = (this.green * target.green).coerceIn(0.0f, 1.0f),
        blue = (this.blue * target.blue).coerceIn(0.0f, 1.0f),
        alpha = (this.alpha * target.alpha).coerceIn(0.0f, 1.0f)
    )

    /**
     * Composite (alpha blend) this foreground color over [background] color (Zero-GC).
     */
    fun compositeOver(background: Color): Color {
        val sourceAlpha = this.alpha
        val destinationAlpha = background.alpha
        val outputAlpha = sourceAlpha + destinationAlpha * (1.0f - sourceAlpha)
        if (outputAlpha <= 0.0001f) return Clear

        val outputRed = (this.red * sourceAlpha + background.red * destinationAlpha * (1.0f - sourceAlpha)) / outputAlpha
        val outputGreen = (this.green * sourceAlpha + background.green * destinationAlpha * (1.0f - sourceAlpha)) / outputAlpha
        val outputBlue = (this.blue * sourceAlpha + background.blue * destinationAlpha * (1.0f - sourceAlpha)) / outputAlpha
        return Color(outputRed, outputGreen, outputBlue, outputAlpha)
    }

    /**
     * Computes relative luminance (0.0 = black, 1.0 = pure white).
     */
    fun luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

    // --- OPENGL & ARC INTEROP ---

    /**
     * Converts to Arc color object using Anuke's pooled [Tmp.c1] (Zero-GC).
     */
    fun toArcColor(target: arc.graphics.Color = Tmp.c1): arc.graphics.Color =
        target.set(red, green, blue, alpha)

    /**
     * Packs RGBA channels into a single 32-bit float in ABGR format for direct OpenGL vertex packing.
     * Compatible with [arc.graphics.Color.toFloatBits] and `GL_UNSIGNED_BYTE` normalized vertex attributes.
     */
    fun toGLPackedFloat(): Float {
        val alphaByte = ((value shr 56) and 0xFFuL).toInt()
        val redByte = ((value shr 48) and 0xFFuL).toInt()
        val greenByte = ((value shr 40) and 0xFFuL).toInt()
        val blueByte = ((value shr 32) and 0xFFuL).toInt()
        val abgrPacked = (alphaByte shl 24) or (blueByte shl 16) or (greenByte shl 8) or redByte
        return java.lang.Float.intBitsToFloat(abgrPacked and 0xFEFFFFFF.toInt())
    }

    override fun toString(): String =
        "Color(red=${(red * 255).toInt()}, green=${(green * 255).toInt()}, blue=${(blue * 255).toInt()}, alpha=${(alpha * 255).toInt()})"

    // --- COMPANION OBJECT & FACTORIES ---

    companion object {
        val Unspecified: Color = Color(0UL)
        val Transparent: Color = Color(0.0f, 0.0f, 0.0f, 0.0f)
        val Clear: Color = Color(0.0f, 0.0f, 0.0f, 0.0f)
        val White: Color = Color(1.0f, 1.0f, 1.0f, 1.0f)
        val Black: Color = Color(0.0f, 0.0f, 0.0f, 1.0f)
        val Red: Color = Color(1.0f, 0.0f, 0.0f, 1.0f)
        val Green: Color = Color(0.0f, 1.0f, 0.0f, 1.0f)
        val Blue: Color = Color(0.0f, 0.0f, 1.0f, 1.0f)
        val Yellow: Color = Color(1.0f, 1.0f, 0.0f, 1.0f)
        val Cyan: Color = Color(0.0f, 1.0f, 1.0f, 1.0f)
        val Magenta: Color = Color(1.0f, 0.0f, 1.0f, 1.0f)

        /** Creates a Compose [Color] from an Arc [arc.graphics.Color]. */
        fun fromArc(arcColor: arc.graphics.Color): Color = Color(arcColor.r, arcColor.g, arcColor.b, arcColor.a)

        private fun pack(alpha: Int, red: Int, green: Int, blue: Int): ULong =
            (((alpha and 0xFF).toULong() shl 56) or
             ((red and 0xFF).toULong() shl 48) or
             ((green and 0xFF).toULong() shl 40) or
             ((blue and 0xFF).toULong() shl 32))

        /**
         * Parses a hex color string (#RGB, #RGBA, #RRGGBB, #RRGGBBAA, or raw hex without #).
         */
        fun parse(hex: String): Color {
            var trimmedHex = hex.trim()
            if (trimmedHex.startsWith("#")) {
                trimmedHex = trimmedHex.substring(1)
            }

            return try {
                when (trimmedHex.length) {
                    3 -> { // RGB
                        val parsedRed = trimmedHex.substring(0, 1).repeat(2).toInt(16)
                        val parsedGreen = trimmedHex.substring(1, 2).repeat(2).toInt(16)
                        val parsedBlue = trimmedHex.substring(2, 3).repeat(2).toInt(16)
                        Color(parsedRed, parsedGreen, parsedBlue, 255)
                    }
                    4 -> { // RGBA
                        val parsedRed = trimmedHex.substring(0, 1).repeat(2).toInt(16)
                        val parsedGreen = trimmedHex.substring(1, 2).repeat(2).toInt(16)
                        val parsedBlue = trimmedHex.substring(2, 3).repeat(2).toInt(16)
                        val parsedAlpha = trimmedHex.substring(3, 4).repeat(2).toInt(16)
                        Color(parsedRed, parsedGreen, parsedBlue, parsedAlpha)
                    }
                    6 -> { // RRGGBB
                        val parsedRed = trimmedHex.substring(0, 2).toInt(16)
                        val parsedGreen = trimmedHex.substring(2, 4).toInt(16)
                        val parsedBlue = trimmedHex.substring(4, 6).toInt(16)
                        Color(parsedRed, parsedGreen, parsedBlue, 255)
                    }
                    8 -> { // RRGGBBAA
                        val parsedRed = trimmedHex.substring(0, 2).toInt(16)
                        val parsedGreen = trimmedHex.substring(2, 4).toInt(16)
                        val parsedBlue = trimmedHex.substring(4, 6).toInt(16)
                        val parsedAlpha = trimmedHex.substring(6, 8).toInt(16)
                        Color(parsedRed, parsedGreen, parsedBlue, parsedAlpha)
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

// --- TOP-LEVEL EXTENSIONS ---

/**
 * Top-level factory constructor for 64-bit Long hex values: `Color(0xFF0A84FFL)`.
 */
fun Color(argbHex: Long): Color = Color((argbHex.toULong() and 0xFFFFFFFFuL) shl 32)

/**
 * Extension bridging Arc mutable [arc.graphics.Color] to Compose immutable [Color].
 */
fun arc.graphics.Color.toComposeColor(): Color = Color(r, g, b, a)

/**
 * Extension property parsing a hex color string: `"#85c1dc".color` or `"85c1dc".color`.
 */
inline val String.color: Color get() = Color.parse(this)

/**
 * Alias for [Color.withAlpha].
 */
fun Color.alpha(alpha: Float): Color = withAlpha(alpha)

/**
 * Formats this color as a hex string `#RRGGBBAA`.
 */
val Color.hex: String
    get() {
        val redByte = (red * 255.0f).toInt().coerceIn(0, 255)
        val greenByte = (green * 255.0f).toInt().coerceIn(0, 255)
        val blueByte = (blue * 255.0f).toInt().coerceIn(0, 255)
        val alphaByte = (alpha * 255.0f).toInt().coerceIn(0, 255)
        return String.format("#%02x%02x%02x%02x", redByte, greenByte, blueByte, alphaByte)
    }
