package org.hubdustry.core.compose.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Giao diện đánh dấu cho tất cả các tương tác người dùng (Press, Hover, Drag, Focus,...).
 */
interface Interaction

/**
 * Nhóm tương tác nhấn (Press) theo kiến trúc Jetpack Compose (AOSP).
 * Cho phép tách rời hoàn toàn trạng thái tương tác khỏi Virtual Layout Node.
 */
sealed interface PressInteraction : Interaction {
    /**
     * Phát ra khi con trỏ bắt đầu nhấn xuống tại vị trí [pressPosition].
     */
    class Press(val pressPosition: Offset) : PressInteraction

    /**
     * Phát ra khi con trỏ được nhả ra hợp lệ tương ứng với [press].
     */
    class Release(val press: Press) : PressInteraction

    /**
     * Phát ra khi cử chỉ nhấn bị hủy (ví dụ di chuyển quá slop hoặc ra ngoài bounds).
     */
    class Cancel(val press: Press) : PressInteraction
}

/**
 * Luồng tương tác chỉ đọc (Read-only Stream of Interactions).
 */
interface InteractionSource {
    val interactions: Flow<Interaction>
}

/**
 * Luồng tương tác có thể ghi (Mutable Stream of Interactions).
 */
interface MutableInteractionSource : InteractionSource {
    suspend fun emit(interaction: Interaction)
    fun tryEmit(interaction: Interaction): Boolean
}

/**
 * Factory khởi tạo một [MutableInteractionSource] chuẩn.
 */
fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

private class MutableInteractionSourceImpl : MutableInteractionSource {
    override val interactions = MutableSharedFlow<Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun emit(interaction: Interaction) {
        interactions.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        return interactions.tryEmit(interaction)
    }
}

/**
 * Nhóm tương tác rê chuột (Hover) theo kiến trúc Jetpack Compose.
 */
sealed interface HoverInteraction : Interaction {
    /**
     * Phát ra khi con trỏ chuột bắt đầu đi vào phạm vi của thành phần.
     */
    class Enter : HoverInteraction

    /**
     * Phát ra khi con trỏ chuột rời khỏi phạm vi của thành phần.
     */
    class Exit(val enter: Enter) : HoverInteraction
}

/**
 * Composable helper lắng nghe luồng [InteractionSource] và trả về [State] boolean
 * phản ánh xem thành phần có đang ở trạng thái bị nhấn hay không.
 * Cập nhật reactive mà không gây side-effect lên cây Virtual DOM.
 */
@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> {
    val isPressed = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val pressInteractions = ArrayList<PressInteraction.Press>()
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressInteractions.add(interaction)
                is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
            }
            isPressed.value = pressInteractions.isNotEmpty()
        }
    }
    return isPressed
}

/**
 * Composable helper lắng nghe luồng [InteractionSource] và trả về [State] boolean
 * phản ánh xem thành phần có đang được rê chuột (Hover) hay không.
 */
@Composable
fun InteractionSource.collectIsHoveredAsState(): State<Boolean> {
    val isHovered = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        var currentEnter: HoverInteraction.Enter? = null
        interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> {
                    currentEnter = interaction
                    isHovered.value = true
                }
                is HoverInteraction.Exit -> {
                    if (currentEnter == interaction.enter) {
                        currentEnter = null
                        isHovered.value = false
                    }
                }
            }
        }
    }
    return isHovered
}
