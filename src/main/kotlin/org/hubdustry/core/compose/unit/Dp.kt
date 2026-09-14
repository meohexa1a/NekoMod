@file:Suppress("NOTHING_TO_INLINE")

package org.hubdustry.core.compose.unit

import arc.Core

/**
 * Đơn vị đo kích thước độc lập với mật độ điểm ảnh (Density-independent Pixels - Dp).
 * Đóng gói số thực 32-bit (value) vào một value class theo chuẩn Jetpack Compose AOSP,
 * triệt tiêu hoàn toàn việc cấp phát heap object (Zero-GC).
 *
 * Tích hợp trực tiếp với bộ điều phối tỉ lệ giao diện qua [toPx] và [uiScaleProvider].
 */
@JvmInline
value class Dp(val value: Float) : Comparable<Dp> {

    /**
     * Chuyển đổi sang pixel thực tế dựa trên tỉ lệ UI hiện tại.
     * Áp dụng Gateway Sanitization (Rule 1.1 & Rule 8.2 NEVER-THROW):
     * - Tự động phòng vệ: fallback về 1.0f nếu chạy trong headless test hoặc scale không hợp lệ (NaN, <= 0).
     * - Loại bỏ hoàn toàn NaN hoặc giá trị âm thành 0f.
     * - Bảo vệ Float.MAX_VALUE không bị tràn số thành POSITIVE_INFINITY.
     */
    inline val toPx: Float
        get() {
            if (value == Float.MAX_VALUE) return Float.MAX_VALUE
            val scale = try {
                val rawScale = uiScaleProvider()
                if (rawScale.isNaN() || rawScale <= 0f) 1f else rawScale
            } catch (_: Throwable) {
                1f
            }
            val px = value * scale
            return if (px.isNaN()) 0f else px
        }

    val isSpecified: Boolean get() = !value.isNaN()
    val isUnspecified: Boolean get() = value.isNaN()

    inline operator fun plus(other: Dp): Dp = Dp(value + other.value)
    inline operator fun minus(other: Dp): Dp = Dp(value - other.value)
    inline operator fun times(factor: Float): Dp = Dp(value * factor)
    inline operator fun times(factor: Int): Dp = Dp(value * factor)
    inline operator fun div(factor: Float): Dp = Dp(value / factor)
    inline operator fun div(factor: Int): Dp = Dp(value / factor)
    inline operator fun div(other: Dp): Float = value / other.value
    inline operator fun unaryMinus(): Dp = Dp(-value)

    override fun compareTo(other: Dp): Int = value.compareTo(other.value)

    override fun toString(): String = if (isSpecified) "${value}.dp" else "Dp.Unspecified"

    companion object {
        val Zero: Dp = Dp(0f)
        val Hairline: Dp = Dp(0f)
        val Infinity: Dp = Dp(Float.MAX_VALUE)
        val Unspecified: Dp = Dp(Float.NaN)

        /**
         * Hàm cung cấp tỉ lệ UI (UI Scale). Mặc định đọc từ Mindustry settings/scene nếu khả dụng,
         * tự động fallback về 1.0f khi chạy headless unit test.
         */
        var uiScaleProvider: () -> Float = { defaultScale() }

        private fun defaultScale(): Float = try {
            val settings = Core.settings
            if (settings != null) {
                val userScalePercent = settings.getInt("uiscale", 100)
                if (userScalePercent > 0) userScalePercent / 100f else 1f
            } else 1f
        } catch (_: Throwable) {
            1f
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DSL CONVENIENCE EXTENSIONS & OPERATORS
// ─────────────────────────────────────────────────────────────────────────────

inline val Number.dp: Dp get() = Dp(this.toFloat())
inline val Float.dp: Dp get() = Dp(this)
inline val Int.dp: Dp get() = Dp(this.toFloat())
inline val Double.dp: Dp get() = Dp(this.toFloat())

inline operator fun Float.times(dp: Dp): Dp = Dp(this * dp.value)
inline operator fun Int.times(dp: Dp): Dp = Dp(this * dp.value)

inline fun Dp.coerceIn(min: Dp, max: Dp): Dp = Dp(value.coerceIn(min.value, max.value))
inline fun Dp.coerceAtLeast(min: Dp): Dp = Dp(maxOf(value, min.value))
inline fun Dp.coerceAtMost(max: Dp): Dp = Dp(minOf(value, max.value))

inline fun maxOf(a: Dp, b: Dp): Dp = Dp(maxOf(a.value, b.value))
inline fun minOf(a: Dp, b: Dp): Dp = Dp(minOf(a.value, b.value))
