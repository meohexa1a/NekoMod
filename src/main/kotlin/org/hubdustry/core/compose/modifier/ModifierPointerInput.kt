package org.hubdustry.core.compose.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.hubdustry.core.compose.input.HoverInteraction
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerInputScope
import org.hubdustry.core.compose.input.PressInteraction
import org.hubdustry.core.compose.input.SuspendingPointerInputFilter
import org.hubdustry.core.compose.input.gestures.detectTapGestures
import org.hubdustry.core.layout.LayoutNode

// ─── Public Pointer Input DSL ─────────────────────────────────────

/**
 * Composable extension khởi tạo [SuspendingPointerInputFilter] gắn vào node.
 * Quản lý CoroutineScope và vòng đời tự động qua [LaunchedEffect].
 */
@Composable
fun Modifier.pointerInput(
    key1: Any?,
    block: suspend PointerInputScope.() -> Unit,
): Modifier {
    val filter = remember(key1) { SuspendingPointerInputFilter() }
    LaunchedEffect(filter, key1) {
        try {
            filter.block()
        } finally {
            filter.reset()
        }
    }
    return this.then(PointerInputModifier(filter))
}

@Composable
fun Modifier.pointerInput(
    block: suspend PointerInputScope.() -> Unit,
): Modifier = pointerInput(Unit, block)

@Composable
fun Modifier.pointerInput(
    key1: Any?,
    key2: Any?,
    block: suspend PointerInputScope.() -> Unit,
): Modifier {
    val filter = remember(key1, key2) { SuspendingPointerInputFilter() }
    LaunchedEffect(filter, key1, key2) {
        try {
            filter.block()
        } finally {
            filter.reset()
        }
    }
    return this.then(PointerInputModifier(filter))
}

@Composable
fun Modifier.pointerInput(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend PointerInputScope.() -> Unit,
): Modifier {
    val filter = remember(key1, key2, key3) { SuspendingPointerInputFilter() }
    LaunchedEffect(filter, key1, key2, key3) {
        try {
            filter.block()
        } finally {
            filter.reset()
        }
    }
    return this.then(PointerInputModifier(filter))
}

// ─── Clickable & Tap Modifiers ─────────────────────────────────────

/**
 * Composable extension thiết lập cử chỉ click/tap.
 * Phát các sự kiện [PressInteraction.Press], [PressInteraction.Release], [PressInteraction.Cancel]
 * vào [interactionSource] mà không gây đột biến trạng thái hay side-effect lên [LayoutNode].
 */
@Composable
fun Modifier.clickable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)

    return this.pointerInput(currentInteractionSource, enabled) {
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                currentInteractionSource.emit(press)
                val releaseOrCancel = if (tryAwaitRelease()) {
                    PressInteraction.Release(press)
                } else {
                    PressInteraction.Cancel(press)
                }
                currentInteractionSource.emit(releaseOrCancel)
            },
            onTap = {
                currentOnClick()
            },
        )
    }
}

/**
 * Composable extension cấu hình cử chỉ click nâng cao, hỗ trợ cả click chuột trái,
 * click đúp, long-press và click chuột phải (secondary click trên PC/Desktop).
 */
@Composable
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val currentOnSecondaryClick by rememberUpdatedState(onSecondaryClick)

    return this.pointerInput(currentInteractionSource, enabled) {
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                currentInteractionSource.emit(press)
                val releaseOrCancel = if (tryAwaitRelease()) {
                    PressInteraction.Release(press)
                } else {
                    PressInteraction.Cancel(press)
                }
                currentInteractionSource.emit(releaseOrCancel)
            },
            onLongPress = currentOnLongClick?.let { callback -> { callback() } },
            onDoubleTap = currentOnDoubleClick?.let { callback -> { callback() } },
            onSecondaryTap = currentOnSecondaryClick?.let { callback -> { callback() } },
            onTap = { currentOnClick() },
        )
    }
}

// ─── Hoverable Modifiers ──────────────────────────────────────────

/**
 * Composable extension lắng nghe sự kiện rê chuột (Hover) theo chuẩn Jetpack Compose.
 * Phát [HoverInteraction.Enter] khi chuột đi vào và [HoverInteraction.Exit] khi chuột rời đi.
 */
@Composable
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    return this.pointerInput(currentInteractionSource, enabled) {
        var currentEnter: HoverInteraction.Enter? = null
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.changes.isEmpty()) continue
                    val change = event.changes[0]
                    val isInside = !change.isOutOfBounds(size, 0f)
                    if (isInside && currentEnter == null) {
                        val enter = HoverInteraction.Enter()
                        currentEnter = enter
                        currentInteractionSource.tryEmit(enter)
                    } else if (!isInside) {
                        val enterToExit = currentEnter
                        if (enterToExit != null) {
                            currentEnter = null
                            currentInteractionSource.tryEmit(HoverInteraction.Exit(enterToExit))
                        }
                    }
                }
            }
        } finally {
            currentEnter?.let { enter ->
                currentInteractionSource.tryEmit(HoverInteraction.Exit(enter))
                currentEnter = null
            }
        }
    }
}

// ─── Internal Modifier Elements ────────────────────────────────────

/**
 * Modifier gắn bộ lọc [SuspendingPointerInputFilter] vào [LayoutNode].
 */
internal data class PointerInputModifier(
    val filter: SuspendingPointerInputFilter,
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) = node.addPointerInputFilter(filter)
}

