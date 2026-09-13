package org.hubdustry.core.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.hubdustry.core.compose.input.InteractionSource
import org.hubdustry.core.compose.input.MutableInteractionSource

/**
 * Quản lý trạng thái cuộn của một viewport theo chuẩn Jetpack Compose Foundation.
 * Tách rời hoàn toàn khỏi Virtual DOM Node, tự động đồng bộ qua Snapshot State.
 */
@Stable
class ScrollState(initial: Float = 0f) {
    /**
     * Vị trí cuộn hiện tại tính theo pixel (từ 0f đến maxValue).
     */
    var value: Float by mutableFloatStateOf(initial)
        internal set

    /**
     * Giới hạn cuộn tối đa cho phép ([contentSize] - [viewportSize]).
     */
    var maxValue: Float by mutableFloatStateOf(Float.MAX_VALUE)
        internal set

    /**
     * Kích thước khung nhìn (viewport) theo trục cuộn.
     */
    var viewportSize: Float by mutableFloatStateOf(0f)
        internal set

    val interactionSource: InteractionSource = MutableInteractionSource()

    /**
     * Cập nhật delta cuộn tương đối (từ con lăn chuột hoặc cử chỉ kéo trượt).
     * Trả về lượng delta thực tế đã được tiêu thụ.
     */
    fun dispatchRawDelta(delta: Float): Float {
        val target = value + delta
        val limit = if (maxValue == Float.MAX_VALUE) Float.MAX_VALUE else maxValue
        val clamped = target.coerceIn(0f, limit)
        val consumed = clamped - value
        value = clamped
        return consumed
    }

    /**
     * Cuộn tới vị trí xác định.
     */
    suspend fun scrollTo(target: Float) {
        val limit = if (maxValue == Float.MAX_VALUE) Float.MAX_VALUE else maxValue
        value = target.coerceIn(0f, limit)
    }
}

/**
 * Khởi tạo và ghi nhớ [ScrollState] trong cây Compose.
 */
@Composable
fun rememberScrollState(initial: Float = 0f): ScrollState {
    return remember { ScrollState(initial) }
}
