package org.hubdustry.core.compose.input.gestures

import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.coroutineScope
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.PointerInputScope
import org.hubdustry.core.layout.Orientation

// ─── Public Drag Gesture APIs ─────────────────────────────────────

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
        val initialDown = awaitFirstDown(requireUnconsumed = false)
        val touchSlop = viewConfiguration.touchSlop
        val slopSquared = touchSlop * touchSlop

        var dragStarted = false
        val pointerId = initialDown.id
        var previousPosition = initialDown.position
        var totalDeltaX = 0f
        var totalDeltaY = 0f

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

            val currentPosition = matched.position
            val deltaX = currentPosition.x - previousPosition.x
            val deltaY = currentPosition.y - previousPosition.y

            if (!dragStarted) {
                totalDeltaX += deltaX
                totalDeltaY += deltaY
                if (totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY > slopSquared) {
                    dragStarted = true
                    onDragStart?.invoke(currentPosition)
                    onDrag(matched, Offset(totalDeltaX, totalDeltaY))
                    matched.consume()
                }
            } else if (deltaX != 0f || deltaY != 0f) {
                onDrag(matched, Offset(deltaX, deltaY))
                matched.consume()
            }

            previousPosition = currentPosition
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
) = detectOrientedDragGestures(
    orientation = Orientation.VERTICAL,
    onDragStart = onDragStart,
    onDragEnd = onDragEnd,
    onDragCancel = onDragCancel,
    onDrag = onVerticalDrag
)

/**
 * Nhận diện cử chỉ kéo trượt theo trục ngang (Horizontal Drag).
 * Độc lập trục (Axis Isolation): chuyển động dọc không kích hoạt kéo ngang và không bị nuốt sự kiện.
 */
suspend fun PointerInputScope.detectHorizontalDragGestures(
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) = detectOrientedDragGestures(
    orientation = Orientation.HORIZONTAL,
    onDragStart = onDragStart,
    onDragEnd = onDragEnd,
    onDragCancel = onDragCancel,
    onDrag = onHorizontalDrag
)

// ─── Internal Oriented Drag Helpers ───────────────────────────────

/**
 * Nhận diện cử chỉ kéo trượt định hướng theo trục chính [orientation].
 * Áp dụng trừu tượng hóa đối xứng (GEMINI.md Rule 2.4 Symmetry Abstraction),
 * triệt tiêu hoàn toàn sự nhân đôi logic giữa kéo dọc và kéo ngang.
 */
internal suspend fun PointerInputScope.detectOrientedDragGestures(
    orientation: Orientation,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onDrag: (change: PointerInputChange, dragAmount: Float) -> Unit
) = coroutineScope {
    awaitEachGesture {
        val initialDown = awaitFirstDown(requireUnconsumed = false)
        val touchSlop = viewConfiguration.touchSlop
        val slopSquared = touchSlop * touchSlop

        var dragStarted = false
        val pointerId = initialDown.id
        var previousPosition = initialDown.position
        var totalDeltaX = 0f
        var totalDeltaY = 0f

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

            val currentPosition = matched.position
            val deltaX = currentPosition.x - previousPosition.x
            val deltaY = currentPosition.y - previousPosition.y

            if (!dragStarted) {
                totalDeltaX += deltaX
                totalDeltaY += deltaY
                val distMain = orientation.main(totalDeltaX, totalDeltaY)
                val distCross = orientation.cross(totalDeltaX, totalDeltaY)
                val distMainSquared = distMain * distMain
                val distCrossSquared = distCross * distCross
                if (distMainSquared > slopSquared && distMainSquared >= distCrossSquared) {
                    dragStarted = true
                    onDragStart?.invoke(currentPosition)
                    onDrag(matched, distMain)
                    matched.consume()
                }
            } else {
                val deltaMain = orientation.main(deltaX, deltaY)
                if (deltaMain != 0f) {
                    onDrag(matched, deltaMain)
                    matched.consume()
                }
            }

            previousPosition = currentPosition
        }

        if (currentEvent.hasPressed) {
            awaitAllPointersUp()
        }
    }
}

