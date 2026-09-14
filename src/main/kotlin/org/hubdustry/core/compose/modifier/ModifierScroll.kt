package org.hubdustry.core.compose.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hubdustry.core.compose.foundation.ScrollState
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.gestures.VelocityTracker1D
import org.hubdustry.core.compose.input.gestures.detectOrientedDragGestures
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.Orientation
import kotlin.math.abs

// ── VẬT LÝ VÀ THỜI GIAN CUỘN (SCROLL & FLING CONSTANTS) ─────────────────────
private const val MOUSE_WHEEL_SCROLL_FACTOR = 32f
private const val MIN_FLING_VELOCITY = 300f
private const val FLING_STOP_VELOCITY = 15f
private const val FLING_FRICTION = 0.94f
private const val FLING_FRAME_DELTA_SECONDS = 0.016f
private const val FLING_FRAME_DELAY_MILLIS = 16L

/**
 * Modifier phần tử quản lý cấu hình cuộn trên [LayoutNode] (GEMINI.md Rule 14 Data Class Contract).
 *
 * Áp dụng trừu tượng hóa đối xứng (Rule 2.4) qua [Orientation] thay vì cờ Boolean.
 * Khi [enabled] = false, node vẫn duy trì scissor clip và gắn [state] để hỗ trợ cuộn
 * theo lập trình (programmatic scroll), chỉ tắt tương tác cử chỉ người dùng (Rule 0.2).
 */
data class ScrollModifier(
    val state: ScrollState,
    val orientation: Orientation = Orientation.VERTICAL,
    val enabled: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) = when (orientation) {
        Orientation.VERTICAL -> {
            node.clipVertical = true
            node.isScrollableVertical = true
            node.verticalScrollState = state
        }
        Orientation.HORIZONTAL -> {
            node.clipHorizontal = true
            node.isScrollableHorizontal = true
            node.horizontalScrollState = state
        }
    }
}

/**
 * Quản lý hoạt ảnh cuộn mượt và quán tính (Smooth Scrolling & Kinetic Fling Driver).
 * Chia sẻ Coroutine Job giữa chuột lăn và cử chỉ vuốt thả để triệt tiêu xung đột.
 */
private class ScrollAnimationDriver {
    var activeJob: Job? = null

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
    }
}

/**
 * Áp dụng hành vi cuộn định hướng cho container theo chuẩn Jetpack Compose Foundation.
 * Tự động cắt bỏ (Scissor Clip) nội dung tràn khỏi viewport và liên kết [ScrollState].
 *
 * KHI [enabled] = false:
 * - Vẫn gắn [ScrollModifier] (bật scissor clip và duy trì [ScrollState] để hỗ trợ cuộn lập trình programmatic scroll).
 * - Bỏ qua việc gắn các bộ lắng nghe sự kiện con trỏ và cử chỉ kéo trượt (GEMINI.md Rule 0.2 No Phantom APIs).
 */
@Composable
private fun Modifier.scroll(
    state: ScrollState,
    orientation: Orientation,
    enabled: Boolean,
    fling: Boolean
): Modifier {
    val base = this.then(ScrollModifier(state, orientation = orientation, enabled = enabled))
    if (!enabled) return base

    val scope = rememberCoroutineScope()
    val driver = remember(state) { ScrollAnimationDriver() }
    val tracker = remember(state) { VelocityTracker1D() }

    return base
        .pointerInput(state, orientation) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    when (event.type) {
                        PointerEventType.Press -> {
                            // Chạm tay xuống lập tức dừng hoạt ảnh cuộn đang chạy (Touch-to-Stop)
                            driver.cancel()
                        }
                        PointerEventType.Scroll -> {
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.isConsumed || change.scrollDelta == Offset.Zero) continue
                            val rawDelta = orientation.main(change.scrollDelta.x, change.scrollDelta.y)
                            val delta = rawDelta * MOUSE_WHEEL_SCROLL_FACTOR
                            val consumed = state.dispatchRawDelta(delta)
                            if (consumed != 0f) {
                                change.consume()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
        .pointerInput(state, orientation, fling) {
            detectOrientedDragGestures(
                orientation = orientation,
                onDragStart = {
                    driver.cancel()
                    tracker.reset()
                },
                onDrag = { change, dragAmount ->
                    if (!change.isConsumed) {
                        change.consume()
                        val orientedPosition = orientation.main(change.position.x, change.position.y)
                        tracker.addPosition(change.uptimeMillis, orientedPosition)
                        val delta = -dragAmount
                        state.dispatchRawDelta(delta)
                    }
                },
                onDragEnd = {
                    if (!fling) return@detectOrientedDragGestures
                    val scrollVelocity = -tracker.calculateVelocity()
                    if (abs(scrollVelocity) < MIN_FLING_VELOCITY) return@detectOrientedDragGestures

                    driver.cancel()
                    driver.activeJob = scope.launch {
                        var currentVelocity = scrollVelocity
                        while (abs(currentVelocity) > FLING_STOP_VELOCITY) {
                            val delta = currentVelocity * FLING_FRAME_DELTA_SECONDS
                            val consumed = state.dispatchRawDelta(delta)
                            if (consumed == 0f) break
                            currentVelocity *= FLING_FRICTION
                            delay(FLING_FRAME_DELAY_MILLIS)
                        }
                    }
                },
                onDragCancel = {
                    driver.cancel()
                    tracker.reset()
                }
            )
        }
}

/**
 * Áp dụng hành vi cuộn dọc cho container (Column, Box, etc.) theo chuẩn Jetpack Compose Foundation.
 * Tự động cắt bỏ (Scissor Clip) nội dung tràn khỏi viewport.
 * Hỗ trợ cuộn bánh xe chuột chuẩn xác và vuốt thả quán tính (Kinetic Fling).
 */
@Composable
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    fling: Boolean = true
): Modifier = scroll(state, Orientation.VERTICAL, enabled, fling)

/**
 * Áp dụng hành vi cuộn ngang cho container (Row, Box, etc.) theo chuẩn Jetpack Compose Foundation.
 */
@Composable
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    fling: Boolean = true
): Modifier = scroll(state, Orientation.HORIZONTAL, enabled, fling)
