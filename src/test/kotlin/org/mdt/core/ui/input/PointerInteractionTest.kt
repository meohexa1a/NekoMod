// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.input

import arc.input.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.modifier.Modifier
import org.mdt.core.ui.modifier.clickable
import org.mdt.core.ui.modifier.draggable
import org.mdt.core.ui.modifier.hoverable
import org.mdt.core.ui.modifier.onClick
import org.mdt.core.ui.modifier.pad
import org.mdt.core.ui.modifier.padding
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.node.LayoutNode

/**
 * ## PointerInteractionTest
 *
 * Validates that pointer events, click resolution, and modifiers execute exactly once
 * and remain idempotent across repeated recompositions.
 */
class PointerInteractionTest {

    @Test
    fun `ClickableModifier triggers onClick exactly once per click gesture`() {
        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)

        var clickCount = 0
        var pressCount = 0
        var releaseCount = 0

        val buttonNode = LayoutNode().apply {
            width = 100.0f
            height = 50.0f
            bounds.set(50.0f, 50.0f, 100.0f, 50.0f)
        }

        val clickableModifier = Modifier.clickable(
            onClick = { clickCount++ },
            onPressStateChanged = { isPressed ->
                if (isPressed) pressCount++ else releaseCount++
            }
        )

        clickableModifier.applyTo(buttonNode)
        canvas.children.add(buttonNode)

        val screenY = 75

        // Perform click gesture at (75, 75)
        inputProcessor.touchDown(75, screenY, 0, KeyCode.mouseLeft)
        assertEquals(1, pressCount, "Press state should be entered once")
        assertEquals(0, releaseCount, "Release state not yet entered")
        assertEquals(0, clickCount, "Click should not fire until touchUp")

        inputProcessor.touchUp(75, screenY, 0, KeyCode.mouseLeft)
        assertEquals(1, releaseCount, "Release state entered once")
        assertEquals(1, clickCount, "onClick must be called exactly once")
    }

    @Test
    fun `Repeated modifier applications do not cause duplicate click invocations`() {
        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)

        var clickCount = 0

        val buttonNode = LayoutNode().apply {
            width = 100.0f
            height = 50.0f
            bounds.set(50.0f, 50.0f, 100.0f, 50.0f)
        }
        canvas.children.add(buttonNode)

        // Simulate 10 recompositions applying new modifier instances
        for (i in 1..10) {
            val modifier = Modifier.onClick { clickCount++ }
            modifier.applyTo(buttonNode)
        }

        val screenY = 75

        // Perform single click gesture
        inputProcessor.touchDown(75, screenY, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(75, screenY, 0, KeyCode.mouseLeft)

        assertEquals(1, clickCount, "onClick must execute exactly once even after 10 recomposition cycles")
    }

    @Test
    fun `HoverableModifier responds cleanly to mouse move events`() {
        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)

        var isHoveredState = false

        val boxNode = LayoutNode().apply {
            width = 100.0f
            height = 50.0f
            bounds.set(50.0f, 50.0f, 100.0f, 50.0f)
        }
        Modifier.hoverable { isHoveredState = it }.applyTo(boxNode)
        canvas.children.add(boxNode)

        val screenYInside = 75
        val screenYOutside = 200

        // Move cursor inside box
        inputProcessor.mouseMoved(75, screenYInside)
        assertEquals(true, isHoveredState, "Should enter hover state when cursor moves inside")

        // Move cursor outside box
        inputProcessor.mouseMoved(200, screenYOutside)
        assertEquals(false, isHoveredState, "Should exit hover state when cursor moves outside")
    }

    @Test
    fun `Modifier chaining preserves both clickable and draggable callbacks`() {
        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)

        var clicked = false
        var dragged = false

        val node = LayoutNode().apply {
            width = 100.0f
            height = 50.0f
            bounds.set(50.0f, 50.0f, 100.0f, 50.0f)
        }

        val chain = Modifier
            .clickable(onClick = { clicked = true })
            .draggable(onDrag = { _, _ -> dragged = true })

        chain.applyTo(node)
        canvas.children.add(node)

        val screenY = 75

        // Perform drag
        inputProcessor.touchDown(75, screenY, 0, KeyCode.mouseLeft)
        inputProcessor.touchDragged(85, screenY, 0)
        assertEquals(true, dragged, "Drag event should be received")
        inputProcessor.touchUp(85, screenY, 0, KeyCode.mouseLeft)

        // Perform tap (click without drag)
        inputProcessor.touchDown(75, screenY, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(75, screenY, 0, KeyCode.mouseLeft)
        assertEquals(true, clicked, "Click event should be received on tap")
    }

    @Test
    fun `InputNode complex events remain active and responsive across recomposition and modifier updates`() {
        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)

        var lastEmittedText = ""
        val inputNode = org.mdt.core.ui.node.InputNode { PlatformHost.NoOp }.apply {
            width = 200.0f
            height = 40.0f
            bounds.set(50.0f, 50.0f, 200.0f, 40.0f)
            onValueChange = { lastEmittedText = it }
        }

        // Apply modifiers repeatedly (simulating recomposition)
        for (i in 1..5) {
            inputNode.modifier = Modifier.pad(8.0f)
        }
        canvas.children.add(inputNode)

        // 1. Focus input node via click at (60, 60)
        inputProcessor.touchDown(60, 60, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(60, 60, 0, KeyCode.mouseLeft)
        assertEquals(inputNode, inputProcessor.focusedNode, "InputNode must gain focus on click")

        // 2. Type characters "Hello"
        for (char in "Hello") {
            inputProcessor.keyTyped(char)
        }
        assertEquals("Hello", inputNode.editState.text, "InputNode must receive and commit typed characters")
        assertEquals("Hello", lastEmittedText, "onValueChange must emit typed text")

        // 3. Press Backspace
        inputProcessor.keyDown(KeyCode.backspace)
        assertEquals("Hell", inputNode.editState.text, "Backspace must delete previous character")
        assertEquals("Hell", lastEmittedText)

        // 4. Select word via double click
        inputProcessor.touchDown(60, 60, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(60, 60, 0, KeyCode.mouseLeft)
        inputProcessor.touchDown(60, 60, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(60, 60, 0, KeyCode.mouseLeft)
        assertEquals(true, inputNode.editState.hasSelection(), "Double click must select text")
    }
}
