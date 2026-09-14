package org.hubdustry.core.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Quản lý trạng thái cuộn của một viewport theo chuẩn Jetpack Compose Foundation.
 * Tách rời hoàn toàn khỏi Virtual DOM Node, tự động đồng bộ qua Snapshot State.
 */
@Stable
class ScrollState(initial: Float = 0f) {
    /**
     * Vị trí cuộn hiện tại tính theo pixel (từ 0f đến maxValue).
     */
    var value: Float by mutableFloatStateOf(if (initial.isNaN() || initial < 0f) 0f else initial)
        internal set

    private var _maxValue by mutableFloatStateOf(Float.MAX_VALUE)

    /**
     * Giới hạn cuộn tối đa cho phép ([contentSize] - [viewportSize]).
     * Tự động chuẩn hóa biên và kẹp trần [value] khi [maxValue] thay đổi (Gateway Sanitization).
     */
    var maxValue: Float
        get() = _maxValue
        internal set(v) {
            val safe = if (v.isNaN() || v < 0f) 0f else v
            _maxValue = safe
            if (value > safe) {
                value = safe
            }
        }

    private var _viewportSize by mutableFloatStateOf(0f)

    /**
     * Kích thước khung nhìn (viewport) theo trục cuộn.
     */
    var viewportSize: Float
        get() = _viewportSize
        internal set(v) {
            _viewportSize = if (v.isNaN() || v < 0f) 0f else v
        }

    /**
     * Cập nhật delta cuộn tương đối (từ con lăn chuột hoặc cử chỉ kéo trượt).
     * Trả về lượng delta thực tế đã được tiêu thụ.
     */
    fun dispatchRawDelta(delta: Float): Float {
        if (delta.isNaN() || delta == 0f) return 0f
        val target = (value + delta).coerceIn(0f, maxValue)
        val consumed = target - value
        value = target
        return consumed
    }

    /**
     * Nhảy lập tức (snap) tới vị trí xác định mà không có hoạt ảnh chuyển tiếp.
     */
    fun snapTo(target: Float) {
        if (target.isNaN()) return
        value = target.coerceIn(0f, maxValue)
    }

    /**
     * Cuộn mượt tới vị trí [target] trong khoảng thời gian [durationMillis] ms.
     * Sử dụng đường cong làm chậm bậc 3 (Cubic Ease-Out).
     *
     * Đây là hàm coroutine đích thực (Suspend Function), tự động cập nhật qua từng frame
     * và chỉ hoàn thành khi đã đến đích hoặc bị hủy bởi cử chỉ mới.
     */
    suspend fun animateScrollTo(
        target: Float,
        durationMillis: Int = 200
    ) {
        if (target.isNaN()) return
        val clampedTarget = target.coerceIn(0f, maxValue)
        val initialValue = value
        val totalDelta = clampedTarget - initialValue
        if (kotlin.math.abs(totalDelta) < 0.5f) {
            snapTo(clampedTarget)
            return
        }

        val effectiveDuration = durationMillis.coerceAtLeast(16)
        val startTime = System.currentTimeMillis()

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / effectiveDuration).coerceIn(0f, 1f)
            // Cubic Ease-Out: f(t) = 1 - (1 - t)^3
            val inv = 1f - progress
            val eased = 1f - inv * inv * inv
            val current = initialValue + totalDelta * eased
            snapTo(current)

            if (progress >= 1f) break
            kotlinx.coroutines.delay(16.milliseconds)
        }
    }

    /**
     * Cuộn mượt tương đối một khoảng cách [delta] trong khoảng thời gian [durationMillis] ms.
     */
    suspend fun animateScrollBy(
        delta: Float,
        durationMillis: Int = 200
    ) {
        if (delta.isNaN() || delta == 0f) return
        animateScrollTo(value + delta, durationMillis)
    }
}

/**
 * Khởi tạo và ghi nhớ [ScrollState] trong cây Compose.
 */
@Composable
fun rememberScrollState(initial: Float = 0f): ScrollState =
    remember { ScrollState(initial) }
