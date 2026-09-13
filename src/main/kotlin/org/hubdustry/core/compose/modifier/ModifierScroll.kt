package org.hubdustry.core.compose.modifier

import androidx.compose.runtime.Composable
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.foundation.ScrollState
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerEventType
import org.hubdustry.core.compose.input.gestures.detectHorizontalDragGestures
import org.hubdustry.core.compose.input.gestures.detectVerticalDragGestures
import org.hubdustry.core.compose.input.pointerInput
import org.hubdustry.core.layout.LayoutNode

/**
 * Modifier phần tử quản lý cấu hình cuộn trên [LayoutNode] (GEMINI.md Rule 14 Data Class Contract).
 */
data class ScrollModifier(
    val state: ScrollState,
    val isVertical: Boolean = true,
    val enabled: Boolean = true
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.clip = true
        if (isVertical) {
            node.isScrollableVertical = enabled
            node.verticalScrollState = if (enabled) state else null
        } else {
            node.isScrollableHorizontal = enabled
            node.horizontalScrollState = if (enabled) state else null
        }
    }
}

/**
 * Áp dụng hành vi cuộn dọc cho container (Column, Box, etc.) theo chuẩn Jetpack Compose Foundation.
 * Tự động cắt bỏ (Scissor Clip) nội dung tràn khỏi viewport, hỗ trợ cuộn con lăn chuột và vuốt kéo màn hình cảm ứng.
 */
@Composable
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    return this
        .then(ScrollModifier(state, isVertical = true, enabled = enabled))
        .pointerInput(state, enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type == PointerEventType.Scroll) {
                        val changes = event.changes
                        if (changes.isNotEmpty()) {
                            val change = changes[0]
                            if (!change.isConsumed && change.scrollDelta != Offset.Zero) {
                                val delta = change.scrollDelta.y * 32f
                                val consumed = state.dispatchRawDelta(delta)
                                if (consumed != 0f) {
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
        }
        .pointerInput(state, enabled) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (!change.isConsumed) {
                        val delta = -dragAmount
                        val consumed = state.dispatchRawDelta(delta)
                        if (consumed != 0f) {
                            change.consume()
                        }
                    }
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
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    return this
        .then(ScrollModifier(state, isVertical = false, enabled = enabled))
        .pointerInput(state, enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type == PointerEventType.Scroll) {
                        val changes = event.changes
                        if (changes.isNotEmpty()) {
                            val change = changes[0]
                            if (!change.isConsumed && change.scrollDelta != Offset.Zero) {
                                val delta = change.scrollDelta.x * 32f
                                val consumed = state.dispatchRawDelta(delta)
                                if (consumed != 0f) {
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
        }
        .pointerInput(state, enabled) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    if (!change.isConsumed) {
                        val delta = -dragAmount
                        val consumed = state.dispatchRawDelta(delta)
                        if (consumed != 0f) {
                            change.consume()
                        }
                    }
                }
            )
        }
}
