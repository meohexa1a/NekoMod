package org.hubdustry.core.compose.foundation.text

/**
 * ## TextRange
 *
 * Biểu diễn một khoảng ký tự văn bản hoặc vị trí con trỏ (khi `start == end`).
 * Đóng gói 2 số nguyên 32-bit (`start` và `end`) vào một số nguyên 64-bit [Long] thông qua
 * `@JvmInline value class` nhằm triệt tiêu hoàn toàn việc cấp phát heap object (Zero-GC).
 */
@JvmInline
value class TextRange(val packedValue: Long) {

    /** Vị trí bắt đầu của khoảng chọn (có thể lớn hơn [end] nếu kéo chọn ngược). */
    val start: Int
        get() = (packedValue shr 32).toInt()

    /** Vị trí kết thúc của khoảng chọn. */
    val end: Int
        get() = (packedValue and 0xFFFFFFFFL).toInt()

    /** Chỉ số nhỏ nhất giữa [start] và [end]. */
    val min: Int
        get() = if (start <= end) start else end

    /** Chỉ số lớn nhất giữa [start] và [end]. */
    val max: Int
        get() = if (start > end) start else end

    /** Độ dài của khoảng chọn ký tự. */
    val length: Int
        get() = max - min

    /** True nếu không có ký tự nào được chọn (con trỏ đứng yên tại 1 vị trí). */
    val collapsed: Boolean
        get() = start == end

    /** True nếu vùng chọn được kéo ngược từ phải sang trái. */
    val reversed: Boolean
        get() = start > end

    /**
     * Giới hạn khoảng chọn nằm trong phạm vi từ [minimumValue] đến [maximumValue].
     */
    fun coerceIn(minimumValue: Int, maximumValue: Int): TextRange {
        val minVal = minOf(minimumValue, maximumValue)
        val maxVal = maxOf(minimumValue, maximumValue)
        val newStart = start.coerceIn(minVal, maxVal)
        val newEnd = end.coerceIn(minVal, maxVal)
        return if (newStart == start && newEnd == end) this else TextRange(newStart, newEnd)
    }

    override fun toString(): String = "TextRange($start, $end)"

    companion object {
        val Zero: TextRange = TextRange(0, 0)
    }
}

/**
 * Hàm khởi tạo tiện ích đóng gói [start] và [end] thành [TextRange].
 */
inline fun TextRange(start: Int, end: Int = start): TextRange {
    val startBits = start.toLong() and 0xFFFFFFFFL
    val endBits = end.toLong() and 0xFFFFFFFFL
    return TextRange((startBits shl 32) or endBits)
}

// ─── Primitive State Delegation (Zero-GC) ────────────────────────────────────

/**
 * Đóng gói snapshot state [MutableLongState] để lưu trữ [TextRange] không boxing (Zero-GC).
 */
@JvmInline
value class MutableTextRangeState(val state: androidx.compose.runtime.MutableLongState) {
    var value: TextRange
        inline get() = TextRange(state.longValue)
        inline set(newVal) { state.longValue = newVal.packedValue }
}

inline fun mutableTextRangeStateOf(initialValue: TextRange = TextRange.Zero): MutableTextRangeState =
    MutableTextRangeState(androidx.compose.runtime.mutableLongStateOf(initialValue.packedValue))

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableTextRangeState.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): TextRange =
    value

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableTextRangeState.setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: TextRange) {
    this.value = value
}

