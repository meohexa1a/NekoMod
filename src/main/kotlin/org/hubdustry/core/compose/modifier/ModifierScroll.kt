package org.hubdustry.core.compose.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.foundation.ScrollState
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.gestures.VelocityTracker1D
import org.hubdustry.core.compose.input.gestures.detectHorizontalDragGestures
import org.hubdustry.core.compose.input.gestures.detectVerticalDragGestures
import org.hubdustry.core.compose.input.pointerInput
import org.hubdustry.core.layout.LayoutNode
import kotlin.math.abs

/**
 * Modifier phần tử quản lý cấu hình cuộn trên [LayoutNode] (GEMINI.md Rule 14 Data Class Contract).
 */
data class ScrollModifier(
    val state: ScrollState,
    val isVertical: Boolean = true,
    val enabled: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        if (isVertical) {
            node.clipVertical = true
            node.isScrollableVertical = enabled
            node.verticalScrollState = if (enabled) state else null
        } else {
            node.clipHorizontal = true
            node.isScrollableHorizontal = enabled
            node.horizontalScrollState = if (enabled) state else null
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
 * Áp dụng hành vi cuộn dọc cho container (Column, Box, etc.) theo chuẩn Jetpack Compose Foundation.
 * Tự động cắt bỏ (Scissor Clip) nội dung tràn khỏi viewport.
 * Hỗ trợ cuộn bánh xe chuột chuẩn xác và vuốt thả quán tính (Kinetic Fling).
 */
@Composable
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    fling: Boolean = true
): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val driver = remember(state) { ScrollAnimationDriver() }
    val tracker = remember(state) { VelocityTracker1D() }

    return this
        .then(ScrollModifier(state, isVertical = true, enabled = enabled))
        .pointerInput(state) {
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
                            val delta = change.scrollDelta.y * 32f
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
        .pointerInput(state, fling) {
            detectVerticalDragGestures(
                onDragStart = {
                    driver.cancel()
                    tracker.reset()
                },
                onVerticalDrag = { change, dragAmount ->
                    if (change.isConsumed) return@detectVerticalDragGestures
                    tracker.addPosition(change.uptimeMillis, change.position.y)
                    val delta = -dragAmount
                    val consumed = state.dispatchRawDelta(delta)
                    if (consumed != 0f) {
                        change.consume()
                    }
                },
                onDragEnd = {
                    if (!fling) return@detectVerticalDragGestures
                    val velocity = tracker.calculateVelocity()
                    val scrollVelocity = -velocity
                    if (abs(scrollVelocity) < 300f) return@detectVerticalDragGestures

                    driver.cancel()
                    driver.activeJob = scope.launch {
                        var currentV = scrollVelocity
                        val dt = 0.016f
                        val friction = 0.94f
                        while (abs(currentV) > 15f) {
                            val delta = currentV * dt
                            val consumed = state.dispatchRawDelta(delta)
                            if (consumed == 0f) break
                            currentV *= friction
                            delay(16)
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
 * Áp dụng hành vi cuộn ngang cho container (Row, Box, etc.) theo chuẩn Jetpack Compose Foundation.
 */
@Composable
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    fling: Boolean = true
): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val driver = remember(state) { ScrollAnimationDriver() }
    val tracker = remember(state) { VelocityTracker1D() }

    return this
        .then(ScrollModifier(state, isVertical = false, enabled = enabled))
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    when (event.type) {
                        PointerEventType.Press -> {
                            driver.cancel()
                        }
                        PointerEventType.Scroll -> {
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.isConsumed || change.scrollDelta == Offset.Zero) continue
                            val delta = change.scrollDelta.x * 32f
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
        .pointerInput(state, fling) {
            detectHorizontalDragGestures(
                onDragStart = {
                    driver.cancel()
                    tracker.reset()
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (change.isConsumed) return@detectHorizontalDragGestures
                    tracker.addPosition(change.uptimeMillis, change.position.x)
                    val delta = -dragAmount
                    val consumed = state.dispatchRawDelta(delta)
                    if (consumed != 0f) {
                        change.consume()
                    }
                },
                onDragEnd = {
                    if (!fling) return@detectHorizontalDragGestures
                    val velocity = tracker.calculateVelocity()
                    val scrollVelocity = -velocity
                    if (abs(scrollVelocity) < 300f) return@detectHorizontalDragGestures

                    driver.cancel()
                    driver.activeJob = scope.launch {
                        var currentV = scrollVelocity
                        val dt = 0.016f
                        val friction = 0.94f
                        while (abs(currentV) > 15f) {
                            val delta = currentV * dt
                            val consumed = state.dispatchRawDelta(delta)
                            if (consumed == 0f) break
                            currentV *= friction
                            delay(16)
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
