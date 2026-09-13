package org.hubdustry.core.compose.input

import androidx.compose.ui.util.fastAny
import kotlin.math.sqrt

/**
 * Biểu diễn tọa độ hoặc vector dịch chuyển 2D (Top-Left Y-down).
 * Đóng gói hai số thực 32-bit (x, y) vào một số nguyên 64-bit Long theo chuẩn Jetpack Compose (AOSP),
 * triệt tiêu hoàn toàn việc cấp phát heap object (Zero-GC).
 */
@JvmInline
value class Offset internal constructor(val packedValue: Long) {
    val x: Float
        get() = Float.fromBits((packedValue ushr 32).toInt())

    val y: Float
        get() = Float.fromBits((packedValue and 0xFFFFFFFFL).toInt())

    operator fun component1(): Float = x
    operator fun component2(): Float = y

    fun copy(x: Float = this.x, y: Float = this.y): Offset = Offset(x, y)

    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(operand: Float): Offset = Offset(x * operand, y * operand)
    operator fun div(operand: Float): Offset = Offset(x / operand, y / operand)

    fun getDistance(): Float = sqrt(x * x + y * y)
    fun getDistanceSquared(): Float = x * x + y * y

    val isSpecified: Boolean get() = !x.isNaN() && !y.isNaN()

    override fun toString(): String = if (isSpecified) "Offset(%.1f, %.1f)".format(x, y) else "Offset.Unspecified"

    companion object {
        val Zero = Offset(0f, 0f)
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }
}

/** Factory function tạo [Offset] từ 2 số thực [x] và [y]. */
fun Offset(x: Float, y: Float): Offset {
    val xBits = x.toBits().toLong()
    val yBits = y.toBits().toLong()
    return Offset((xBits shl 32) or (yBits and 0xFFFFFFFFL))
}

/**
 * Kích thước nguyên tính theo pixel của một node hoặc viewport.
 * Đóng gói hai số nguyên 32-bit (width, height) vào một số nguyên 64-bit Long (Zero-GC).
 */
@JvmInline
value class IntSize internal constructor(val packedValue: Long) {
    val width: Int
        get() = (packedValue ushr 32).toInt()

    val height: Int
        get() = (packedValue and 0xFFFFFFFFL).toInt()

    operator fun component1(): Int = width
    operator fun component2(): Int = height

    fun copy(width: Int = this.width, height: Int = this.height): IntSize = IntSize(width, height)

    override fun toString(): String = "$width x $height"

    companion object {
        val Zero = IntSize(0, 0)
    }
}

/** Factory function tạo [IntSize] từ 2 số nguyên [width] và [height]. */
fun IntSize(width: Int, height: Int): IntSize {
    val wBits = width.toLong()
    val hBits = height.toLong()
    return IntSize((wBits shl 32) or (hBits and 0xFFFFFFFFL))
}

/**
 * Định danh con trỏ / ngón tay cảm ứng (Zero-GC Value Class).
 */
@JvmInline
value class PointerId(val value: Long)

/**
 * Loại thiết bị con trỏ theo chuẩn Jetpack Compose AOSP.
 */
enum class PointerType {
    Touch,
    Mouse,
    Stylus,
    Unknown
}

/**
 * Nút bấm chuột trên môi trường Desktop (Mindustry PC).
 */
enum class PointerButton {
    Primary,    // Chuột trái (Left)
    Secondary,  // Chuột phải (Right)
    Tertiary    // Chuột giữa (Middle)
}

/**
 * Thứ tự các lượt duyệt qua cây Virtual Node trong một sự kiện con trỏ.
 * Tuân thủ mô hình 3-pass của Jetpack Compose (AOSP):
 * - Initial: Tunneling từ Root xuống Leaf (cho phép cha đánh chặn trước).
 * - Main: Bubbling từ Leaf lên Root (lượt xử lý chính của widget con).
 * - Final: Hậu xử lý (dọn dẹp hoặc theo dõi trạng thái cuối).
 */
enum class PointerEventPass {
    Initial,
    Main,
    Final
}

/**
 * Loại hành vi của sự kiện con trỏ.
 */
enum class PointerEventType {
    Press,
    Release,
    Move,
    Enter,
    Exit,
    Scroll,
    Unknown
}

/**
 * Lưu trữ trạng thái tiêu thụ của sự kiện con trỏ, cho phép lan truyền giữa các tầng node trong cây.
 */
class ConsumedData(
    var isConsumed: Boolean = false
)

/**
 * Đại diện cho sự thay đổi trạng thái của một con trỏ tại một thời điểm cụ thể.
 */
class PointerInputChange(
    val id: PointerId,
    val uptimeMillis: Long,
    val position: Offset,
    val pressed: Boolean,
    val previousUptimeMillis: Long,
    val previousPosition: Offset,
    val previousPressed: Boolean,
    val consumed: ConsumedData = ConsumedData(),
    val type: PointerType = PointerType.Touch,
    val button: PointerButton? = null,
    val scrollDelta: Offset = Offset.Zero
) {
    var isConsumed: Boolean
        get() = consumed.isConsumed
        set(value) {
            consumed.isConsumed = value
        }

    fun consume() {
        consumed.isConsumed = true
    }

    fun positionChange(): Offset = position - previousPosition

    fun isOutOfBounds(size: IntSize, slop: Float = 0f): Boolean {
        val x = position.x
        val y = position.y
        return x < -slop || x > size.width + slop || y < -slop || y > size.height + slop
    }

    fun isOutOfBounds(width: Float, height: Float, slop: Float = 0f): Boolean {
        val x = position.x
        val y = position.y
        return x < -slop || x > width + slop || y < -slop || y > height + slop
    }

    val changedToDown: Boolean
        get() = !isConsumed && !previousPressed && pressed

    val changedToUp: Boolean
        get() = !isConsumed && previousPressed && !pressed

    val changedToDownIgnoreConsumed: Boolean
        get() = !previousPressed && pressed

    val changedToUpIgnoreConsumed: Boolean
        get() = previousPressed && !pressed
}

/**
 * Gói sự kiện con trỏ chứa danh sách các thay đổi của tất cả các con trỏ đang hoạt động.
 */
class PointerEvent(
    val changes: List<PointerInputChange>,
    val type: PointerEventType = PointerEventType.Unknown
) {
    /**
     * Hot-path Zero-GC kiểm tra xem có bất kỳ con trỏ nào đang được nhấn không.
     */
    val hasPressed: Boolean
        get() = changes.fastAny { it.pressed }

    val button: PointerButton?
        get() = if (changes.isNotEmpty()) changes[0].button else null

    fun isButtonPressed(targetButton: PointerButton): Boolean =
        changes.fastAny { it.pressed && it.button == targetButton }
}
