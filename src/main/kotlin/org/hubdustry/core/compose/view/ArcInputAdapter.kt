package org.hubdustry.core.compose.view

import arc.Core
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerType

/**
 * [ArcInputAdapter] — Cầu nối chuyển tiếp sự kiện thô từ Arc Scene2D sang [InputDispatcher].
 *
 * TRÁCH NHIỆM (Phân Tách Module Đợt 3):
 * 1. Bức tường Berlin: Lật trục tọa độ Arc Scene2D (Y-up) sang Compose (Y-down) qua [toComposeY].
 * 2. Quản lý Arc [arc.scene.Scene.scrollFocus]: Tự động xin quyền cuộn khi con trỏ rê vào hoặc chạm nhấn,
 *    giải phóng quyền cuộn khi con trỏ rời khỏi view.
 * 3. Chuyển đổi mã phím [KeyCode] sang [PointerButton] và [PointerType].
 */
internal class ArcInputAdapter(
    private val view: ComposeView,
    private val dispatcher: InputDispatcher
) : InputListener() {

    internal fun toComposeY(arcY: Float): Float =
        (if (view.height > 0f) view.height else view.rootLayoutNode.height) - arcY

    private fun KeyCode?.toPointerButton(): PointerButton = when (this) {
        KeyCode.mouseLeft -> PointerButton.Primary
        KeyCode.mouseRight -> PointerButton.Secondary
        KeyCode.mouseMiddle -> PointerButton.Tertiary
        else -> PointerButton.Primary
    }

    private fun KeyCode?.toPointerType(): PointerType =
        if (this != null) PointerType.Mouse else PointerType.Touch

    internal fun grabScrollFocus() {
        val activeScene = view.scene ?: Core.scene
        if (activeScene != null && activeScene.scrollFocus !== view) {
            activeScene.scrollFocus = view
        }
    }

    internal fun releaseScrollFocus() {
        val activeScene = view.scene ?: Core.scene
        if (activeScene != null && activeScene.scrollFocus === view) {
            activeScene.scrollFocus = null
        }
    }

    override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
        grabScrollFocus()
        return dispatcher.touchDown(
            composeX = x,
            composeY = toComposeY(y),
            pointer = pointer,
            uptime = System.currentTimeMillis(),
            button = button.toPointerButton(),
            pointerType = button.toPointerType()
        )
    }

    override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        dispatcher.touchMove(
            composeX = x,
            composeY = toComposeY(y),
            pointer = pointer,
            uptime = System.currentTimeMillis()
        )
    }

    override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
        dispatcher.touchUp(
            composeX = x,
            composeY = toComposeY(y),
            pointer = pointer,
            uptime = System.currentTimeMillis(),
            button = button.toPointerButton(),
            pointerType = button.toPointerType()
        )
    }

    override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
        grabScrollFocus()
        return dispatcher.mouseMove(
            composeX = x,
            composeY = toComposeY(y),
            uptime = System.currentTimeMillis()
        )
    }

    override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Element?) {
        if (pointer == -1) {
            grabScrollFocus()
        }
    }

    override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Element?) {
        if (pointer == -1) {
            releaseScrollFocus()
            dispatcher.mouseExit(System.currentTimeMillis())
        }
    }

    override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
        val isShift = try { Core.input?.shift() == true } catch (_: Throwable) { false }
        val delta = if (isShift) Offset(amountY, 0f) else Offset(amountX, amountY)
        return dispatcher.scroll(
            composeX = x,
            composeY = toComposeY(y),
            scrollDelta = delta,
            uptime = System.currentTimeMillis()
        )
    }

    override fun keyDown(event: InputEvent?, keycode: KeyCode?): Boolean =
        dispatcher.keyDown(keycode)

    override fun keyUp(event: InputEvent?, keycode: KeyCode?): Boolean =
        dispatcher.keyUp(keycode)

    override fun keyTyped(event: InputEvent?, character: Char): Boolean =
        dispatcher.keyTyped(character)
}
