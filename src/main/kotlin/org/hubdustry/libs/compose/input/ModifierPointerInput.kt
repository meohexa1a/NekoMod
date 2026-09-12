package org.hubdustry.libs.compose.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.hubdustry.libs.compose.Modifier
import org.hubdustry.libs.compose.input.gestures.detectTapGestures
import org.hubdustry.libs.layout.LayoutNode

/**
 * Modifier gắn bộ lọc [SuspendingPointerInputFilter] vào [LayoutNode].
 */
class PointerInputModifier(
    val filter: SuspendingPointerInputFilter
) : Modifier.Element {
    override fun applyTo(node: LayoutNode) {
        node.addPointerInputFilter(filter)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is PointerInputModifier && filter == other.filter)

    override fun hashCode(): Int = filter.hashCode()
}

/**
 * Composable extension khởi tạo [SuspendingPointerInputFilter] gắn vào node.
 * Quản lý CoroutineScope và vòng đời tự động qua [LaunchedEffect].
 */
@Composable
fun Modifier.pointerInput(
    key1: Any?,
    block: suspend PointerInputScope.() -> Unit
): Modifier {
    val filter = remember(key1) { SuspendingPointerInputFilter() }
    LaunchedEffect(filter, key1) {
        filter.coroutineScope = this
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
    block: suspend PointerInputScope.() -> Unit
): Modifier {
    val filter = remember(key1, key2) { SuspendingPointerInputFilter() }
    LaunchedEffect(filter, key1, key2) {
        filter.coroutineScope = this
        try {
            filter.block()
        } finally {
            filter.reset()
        }
    }
    return this.then(PointerInputModifier(filter))
}

/**
 * Composable extension thiết lập cử chỉ click/tap.
 * Phát các sự kiện [PressInteraction.Press], [PressInteraction.Release], [PressInteraction.Cancel]
 * vào [interactionSource] mà không gây đột biến trạng thái hay side-effect lên [LayoutNode].
 */
@Composable
fun Modifier.clickable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)

    return this.pointerInput(source, enabled) {
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                source.emit(press)
                val releaseOrCancel = if (tryAwaitRelease()) {
                    PressInteraction.Release(press)
                } else {
                    PressInteraction.Cancel(press)
                }
                source.emit(releaseOrCancel)
            },
            onTap = {
                currentOnClick()
            }
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
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val currentOnSecondaryClick by rememberUpdatedState(onSecondaryClick)

    return this.pointerInput(source, enabled) {
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                source.emit(press)
                val releaseOrCancel = if (tryAwaitRelease()) {
                    PressInteraction.Release(press)
                } else {
                    PressInteraction.Cancel(press)
                }
                source.emit(releaseOrCancel)
            },
            onLongPress = if (currentOnLongClick != null) { { currentOnLongClick?.invoke() } } else null,
            onDoubleTap = if (currentOnDoubleClick != null) { { currentOnDoubleClick?.invoke() } } else null,
            onSecondaryTap = if (currentOnSecondaryClick != null) { { currentOnSecondaryClick?.invoke() } } else null,
            onTap = { currentOnClick() }
        )
    }
}

/**
 * Composable extension lắng nghe sự kiện rê chuột (Hover) theo chuẩn Jetpack Compose.
 * Phát [HoverInteraction.Enter] khi chuột đi vào và [HoverInteraction.Exit] khi chuột rời đi.
 */
@Composable
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val source = interactionSource ?: remember { MutableInteractionSource() }

    return this.pointerInput(source, enabled) {
        var currentEnter: HoverInteraction.Enter? = null
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = if (event.changes.isNotEmpty()) event.changes[0] else null
                    if (change != null) {
                        val isInside = !change.isOutOfBounds(size, 0f)
                        if (isInside && currentEnter == null) {
                            val enter = HoverInteraction.Enter()
                            currentEnter = enter
                            source.tryEmit(enter)
                        } else if (!isInside && currentEnter != null) {
                            val exit = HoverInteraction.Exit(currentEnter!!)
                            currentEnter = null
                            source.tryEmit(exit)
                        }
                    }
                }
            }
        } finally {
            currentEnter?.let { enter ->
                source.tryEmit(HoverInteraction.Exit(enter))
                currentEnter = null
            }
        }
    }
}
