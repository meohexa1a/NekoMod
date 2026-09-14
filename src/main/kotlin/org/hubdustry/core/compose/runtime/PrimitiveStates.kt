@file:Suppress("NOTHING_TO_INLINE")

package org.hubdustry.core.compose.runtime

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import org.hubdustry.core.compose.input.IntSize
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.unit.Dp
import kotlin.reflect.KProperty

/**
 * Bộ ba State không boxing (The Zero-Boxing Trinity) cho NekoMod v3.
 *
 * Giải quyết triệt để bẫy boxing của generic `mutableStateOf<T>` đối với Kotlin `@JvmInline value class`.
 * Bằng cách đóng gói @JvmInline value class trên các Snapshot Primitive State nguyên thủy
 * ([MutableFloatState] và [MutableLongState]), giá trị được lưu trữ dưới dạng raw float/long trên CPU
 * registers và bytecode, triệt tiêu 100% việc cấp phát heap object (Zero-GC Rule 3.7).
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Dp State (Float 32-bit Primitive Delegation)
// ─────────────────────────────────────────────────────────────────────────────

@JvmInline
value class MutableDpState(val state: MutableFloatState) {
    var value: Dp
        inline get() = Dp(state.floatValue)
        inline set(newVal) { state.floatValue = newVal.value }
}

inline fun mutableDpStateOf(initialValue: Dp): MutableDpState =
    MutableDpState(mutableFloatStateOf(initialValue.value))

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableDpState.getValue(thisRef: Any?, property: KProperty<*>): Dp =
    value

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableDpState.setValue(thisRef: Any?, property: KProperty<*>, value: Dp) {
    this.value = value
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Offset State (Long 64-bit: Float x + Float y)
// ─────────────────────────────────────────────────────────────────────────────

@JvmInline
value class MutableOffsetState(val state: MutableLongState) {
    var value: Offset
        inline get() = Offset(state.longValue)
        inline set(newVal) { state.longValue = newVal.packedValue }
}

inline fun mutableOffsetStateOf(initialValue: Offset = Offset.Zero): MutableOffsetState =
    MutableOffsetState(mutableLongStateOf(initialValue.packedValue))

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableOffsetState.getValue(thisRef: Any?, property: KProperty<*>): Offset =
    value

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableOffsetState.setValue(thisRef: Any?, property: KProperty<*>, value: Offset) {
    this.value = value
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. IntSize State (Long 64-bit: Int w + Int h)
// ─────────────────────────────────────────────────────────────────────────────

@JvmInline
value class MutableIntSizeState(val state: MutableLongState) {
    var value: IntSize
        inline get() = IntSize(state.longValue)
        inline set(newVal) { state.longValue = newVal.packedValue }
}

inline fun mutableIntSizeStateOf(initialValue: IntSize = IntSize.Zero): MutableIntSizeState =
    MutableIntSizeState(mutableLongStateOf(initialValue.packedValue))

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableIntSizeState.getValue(thisRef: Any?, property: KProperty<*>): IntSize =
    value

@Suppress("NOTHING_TO_INLINE")
inline operator fun MutableIntSizeState.setValue(thisRef: Any?, property: KProperty<*>, value: IntSize) {
    this.value = value
}
