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
    constructor(radius: Float) : this(radius, radius, radius, radius)

    /** Bo đối xứng theo 2 nửa trên và dưới. */
    constructor(top: Float, bottom: Float) : this(top, top, bottom, bottom)

    val isZero: Boolean
        get() = topStart <= 0.001f && topEnd <= 0.001f && bottomEnd <= 0.001f && bottomStart <= 0.001f

    companion object {
        /** Hình chữ nhật tiêu chuẩn 4 góc vuông (radius = 0). */
        val None = RoundedCorners(0f)

        /** Bo góc cực đại tạo hình viên thuốc (Pill) hoặc hình tròn. */
        val Circle = RoundedCorners(9999f)
    }
}
