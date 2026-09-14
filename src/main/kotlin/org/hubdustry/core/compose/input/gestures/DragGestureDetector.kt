package org.hubdustry.core.compose.input.gestures

import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.coroutineScope
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.PointerInputScope

/**
 * Nhận diện cử chỉ kéo trượt (Drag Gestures) theo chuẩn Jetpack Compose AOSP.
 * Tuân thủ nghiêm ngặt kỷ luật Hot-Path Zero-GC:
 * - So sánh khoảng cách bằng bình phương: `dx * dx + dy * dy > slop * slop` (GEMINI.md Rule 3.3).
 * - Dùng vòng lặp chỉ mục thuần túy cho danh sách changes.
 */
suspend fun PointerInputScope.detectDragGestures(
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val touchSlop = viewConfiguration.touchSlop
        val slopSquared = touchSlop * touchSlop

        var dragStarted = false
        val pointerId = down.id
        var previousPos = down.position
        var totalDx = 0f
        var totalDy = 0f

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val matched = event.changes.fastFirstOrNull { it.id == pointerId }

            if (matched == null || matched.isConsumed) {
                if (dragStarted) onDragCancel?.invoke()
                break
            }

            if (matched.changedToUp) {
                if (dragStarted) onDragEnd?.invoke()
                break
            }

            val currentPos = matched.position
            val deltaX = currentPos.x - previousPos.x
            val deltaY = currentPos.y - previousPos.y

            if (!dragStarted) {
                totalDx += deltaX
                totalDy += deltaY
                if (totalDx * totalDx + totalDy * totalDy > slopSquared) {
                    dragStarted = true
                    onDragStart?.invoke(currentPos)
                    onDrag(matched, Offset(totalDx, totalDy))
                    matched.consume()
                }
            } else if (deltaX != 0f || deltaY != 0f) {
                onDrag(matched, Offset(deltaX, deltaY))
                matched.consume()
            }

            previousPos = currentPos
        }

        if (currentEvent.hasPressed) {
            awaitAllPointersUp()
        }
    }
}

/**
 * Nhận diện cử chỉ kéo trượt theo trục dọc (Vertical Drag).
 * Độc lập trục (Axis Isolation): chuyển động ngang không kích hoạt kéo dọc và không bị nuốt sự kiện.
 */
suspend fun PointerInputScope.detectVerticalDragGestures(
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val touchSlop = viewConfiguration.touchSlop
        val slopSquared = touchSlop * touchSlop

        var dragStarted = false
        val pointerId = down.id
        var previousPos = down.position
        var totalDx = 0f
        var totalDy = 0f

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val matched = event.changes.fastFirstOrNull { it.id == pointerId }

            if (matched == null || matched.isConsumed) {
                if (dragStarted) onDragCancel?.invoke()
                break
            }

            if (matched.changedToUp) {
                if (dragStarted) onDragEnd?.invoke()
                break
            }

            val currentPos = matched.position
            val deltaX = currentPos.x - previousPos.x
            val deltaY = currentPos.y - previousPos.y

            if (!dragStarted) {
                totalDx += deltaX
                totalDy += deltaY
                val distYSquared = totalDy * totalDy
                val distXSquared = totalDx * totalDx
                if (distYSquared > slopSquared && distYSquared >= distXSquared) {
                    dragStarted = true
                    onDragStart?.invoke(currentPos)
                    onVerticalDrag(matched, totalDy)
                    matched.consume()
                }
            } else if (deltaY != 0f) {
                onVerticalDrag(matched, deltaY)
                matched.consume()
            }

            previousPos = currentPos
        }

        if (currentEvent.hasPressed) {
            awaitAllPointersUp()
        }
    }
}

/**
 * Nhận diện cử chỉ kéo trượt theo trục ngang (Horizontal Drag).
 * Độc lập trục (Axis Isolation): chuyển động dọc không kích hoạt kéo ngang và không bị nuốt sự kiện.
 */
suspend fun PointerInputScope.detectHorizontalDragGestures(
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val touchSlop = viewConfiguration.touchSlop
        val slopSquared = touchSlop * touchSlop

        var dragStarted = false
        val pointerId = down.id
        var previousPos = down.position
        var totalDx = 0f
        var totalDy = 0f

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val matched = event.changes.fastFirstOrNull { it.id == pointerId }

            if (matched == null || matched.isConsumed) {
                if (dragStarted) onDragCancel?.invoke()
                break
            }

            if (matched.changedToUp) {
                if (dragStarted) onDragEnd?.invoke()
                break
            }

            val currentPos = matched.position
            val deltaX = currentPos.x - previousPos.x
            val deltaY = currentPos.y - previousPos.y

            if (!dragStarted) {
                totalDx += deltaX
                totalDy += deltaY
                val distYSquared = totalDy * totalDy
                val distXSquared = totalDx * totalDx
                if (distXSquared > slopSquared && distXSquared >= distYSquared) {
                    dragStarted = true
                    onDragStart?.invoke(currentPos)
                    onHorizontalDrag(matched, totalDx)
                    matched.consume()
                }
            } else if (deltaX != 0f) {
                onHorizontalDrag(matched, deltaX)
                matched.consume()
            }

            previousPos = currentPos
        }

        if (currentEvent.hasPressed) {
            awaitAllPointersUp()
        }
    }
}
