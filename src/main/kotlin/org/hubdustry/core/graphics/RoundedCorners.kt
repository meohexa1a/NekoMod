package org.hubdustry.core.graphics

/** Dung sai so sánh xấp xỉ 0 cho bán kính bo góc SDF. */
private const val ZERO_TOLERANCE = 0.001f

/** Bán kính bo góc cực đại để tạo hình viên thuốc (Pill/Capsule) hoặc hình tròn. */
const val PILL_RADIUS: Float = 9999f

/** Khử độc toán học tại cửa khẩu: loại bỏ NaN và kẹp số âm về 0f. */
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun sanitizeCornerRadius(radius: Float): Float =
    if (radius.isNaN() || radius < 0f) 0f else radius

/**
 * Cấu hình bán kính bo 4 góc của hình hộp chữ nhật trong Shader SDF giải tích của NekoMod.
 *
 * Phản ánh trung thực 100% năng lực dựng hình của engine (Inigo Quilez Rounded Box SDF),
 * triệt tiêu hoàn toàn interface Shape và các lớp bọc trừu tượng hóa rỗng.
 *
 * Thứ tự 4 góc: [topStart], [topEnd], [bottomEnd], [bottomStart].
 */
data class RoundedCorners(
    val topStart: Float = 0f,
    val topEnd: Float = 0f,
    val bottomEnd: Float = 0f,
    val bottomStart: Float = 0f
) {
    /** Bo đều cả 4 góc với cùng bán kính [radius]. */
    constructor(radius: Float) : this(
        topStart = sanitizeCornerRadius(radius),
        topEnd = sanitizeCornerRadius(radius),
        bottomEnd = sanitizeCornerRadius(radius),
        bottomStart = sanitizeCornerRadius(radius)
    )

    /** Bo đối xứng theo 2 nửa trên và dưới. */
    constructor(top: Float, bottom: Float) : this(
        topStart = sanitizeCornerRadius(top),
        topEnd = sanitizeCornerRadius(top),
        bottomEnd = sanitizeCornerRadius(bottom),
        bottomStart = sanitizeCornerRadius(bottom)
    )

    /** Bán kính bo góc Top-Start đã chuẩn hóa (chống NaN và số âm). */
    val safeTopStart: Float get() = sanitizeCornerRadius(topStart)

    /** Bán kính bo góc Top-End đã chuẩn hóa (chống NaN và số âm). */
    val safeTopEnd: Float get() = sanitizeCornerRadius(topEnd)

    /** Bán kính bo góc Bottom-End đã chuẩn hóa (chống NaN và số âm). */
    val safeBottomEnd: Float get() = sanitizeCornerRadius(bottomEnd)

    /** Bán kính bo góc Bottom-Start đã chuẩn hóa (chống NaN và số âm). */
    val safeBottomStart: Float get() = sanitizeCornerRadius(bottomStart)

    /** Kiểm tra xem cả 4 góc có đều là góc vuông sắc nhọn (bán kính xấp xỉ 0) không. */
    val isZero: Boolean
        get() = safeTopStart <= ZERO_TOLERANCE &&
                safeTopEnd <= ZERO_TOLERANCE &&
                safeBottomEnd <= ZERO_TOLERANCE &&
                safeBottomStart <= ZERO_TOLERANCE

    companion object {
        /** Hình chữ nhật tiêu chuẩn 4 góc vuông (radius = 0). */
        val None = RoundedCorners(0f)

        /** Bo góc cực đại tạo hình viên thuốc (Pill) hoặc hình tròn. */
        val Pill = RoundedCorners(PILL_RADIUS)

        /** Tương thích ngữ nghĩa với Pill. */
        val Circle = Pill

        /** Khởi tạo [RoundedCorners] với 4 góc tùy biến và khử độc toán học ngay tại cửa khẩu. */
        fun of(
            topStart: Float = 0f,
            topEnd: Float = 0f,
            bottomEnd: Float = 0f,
            bottomStart: Float = 0f
        ): RoundedCorners = RoundedCorners(
            topStart = sanitizeCornerRadius(topStart),
            topEnd = sanitizeCornerRadius(topEnd),
            bottomEnd = sanitizeCornerRadius(bottomEnd),
            bottomStart = sanitizeCornerRadius(bottomStart)
        )
    }
}
