package org.hubdustry.core.graphics

/**
 * Khuôn mẫu định hình hình học (Shape) cho các container và widget trong NekoMod Compose.
 */
sealed interface Shape

/**
 * Hình chữ nhật tiêu chuẩn với 4 góc sắc nhọn (radius = 0).
 */
data object RectangleShape : Shape

/**
 * Hình chữ nhật bo góc với khả năng tinh chỉnh độc lập 4 bán kính góc.
 * Tuân theo thứ tự CSS/Compose: topStart, topEnd, bottomEnd, bottomStart.
 */
data class RoundedCornerShape(
    val topStart: Float = 0f,
    val topEnd: Float = 0f,
    val bottomEnd: Float = 0f,
    val bottomStart: Float = 0f
) : Shape {
    constructor(radius: Float) : this(radius, radius, radius, radius)
    constructor(top: Float, bottom: Float) : this(top, top, bottom, bottom)
}

/**
 * Shape hình tròn / viên thuốc (Pill) với bán kính bo góc cực đại.
 */
val CircleShape: RoundedCornerShape = RoundedCornerShape(9999f)
