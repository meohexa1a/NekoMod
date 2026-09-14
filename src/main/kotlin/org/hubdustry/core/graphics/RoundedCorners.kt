package org.hubdustry.core.graphics

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
        topStart = if (radius.isNaN() || radius < 0f) 0f else radius,
        topEnd = if (radius.isNaN() || radius < 0f) 0f else radius,
        bottomEnd = if (radius.isNaN() || radius < 0f) 0f else radius,
        bottomStart = if (radius.isNaN() || radius < 0f) 0f else radius
    )

    /** Bo đối xứng theo 2 nửa trên và dưới. */
    constructor(top: Float, bottom: Float) : this(
        topStart = if (top.isNaN() || top < 0f) 0f else top,
        topEnd = if (top.isNaN() || top < 0f) 0f else top,
        bottomEnd = if (bottom.isNaN() || bottom < 0f) 0f else bottom,
        bottomStart = if (bottom.isNaN() || bottom < 0f) 0f else bottom
    )

    /** Bán kính bo góc Top-Start đã chuẩn hóa (chống NaN và số âm). */
    val safeTopStart: Float get() = if (topStart.isNaN() || topStart < 0f) 0f else topStart

    /** Bán kính bo góc Top-End đã chuẩn hóa (chống NaN và số âm). */
    val safeTopEnd: Float get() = if (topEnd.isNaN() || topEnd < 0f) 0f else topEnd

    /** Bán kính bo góc Bottom-End đã chuẩn hóa (chống NaN và số âm). */
    val safeBottomEnd: Float get() = if (bottomEnd.isNaN() || bottomEnd < 0f) 0f else bottomEnd

    /** Bán kính bo góc Bottom-Start đã chuẩn hóa (chống NaN và số âm). */
    val safeBottomStart: Float get() = if (bottomStart.isNaN() || bottomStart < 0f) 0f else bottomStart

    /** Kiểm tra xem cả 4 góc có đều là góc vuông sắc nhọn (bán kính xấp xỉ 0) không. */
    val isZero: Boolean
        get() = safeTopStart <= 0.001f && safeTopEnd <= 0.001f && safeBottomEnd <= 0.001f && safeBottomStart <= 0.001f

    companion object {
        /** Hình chữ nhật tiêu chuẩn 4 góc vuông (radius = 0). */
        val None = RoundedCorners(0f)

        /** Bo góc cực đại tạo hình viên thuốc (Pill) hoặc hình tròn. */
        val Circle = RoundedCorners(9999f)

        /** Khởi tạo [RoundedCorners] với 4 góc tùy biến và khử độc toán học ngay tại cửa khẩu. */
        fun of(
            topStart: Float = 0f,
            topEnd: Float = 0f,
            bottomEnd: Float = 0f,
            bottomStart: Float = 0f
        ): RoundedCorners = RoundedCorners(
            topStart = if (topStart.isNaN() || topStart < 0f) 0f else topStart,
            topEnd = if (topEnd.isNaN() || topEnd < 0f) 0f else topEnd,
            bottomEnd = if (bottomEnd.isNaN() || bottomEnd < 0f) 0f else bottomEnd,
            bottomStart = if (bottomStart.isNaN() || bottomStart < 0f) 0f else bottomStart
        )
    }
}
