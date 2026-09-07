// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui

import arc.input.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.compose.ComposePipeline
import org.mdt.core.ui.compose.UIComposition
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.node.TextNode
import org.mdt.core.ui.node.UINode

class NekoAppClickTest {

    @Test
    fun `Verify clicking buttons in NekoApp works on first click`() {
        val host = PlatformHost.NoOp
        val canvas = CanvasNode { host }
        val inputProcessor = EngineInputProcessor(canvas) { host }
        canvas.inputProcessor = inputProcessor

        val pipeline = ComposePipeline { host }
        val composition = UIComposition(canvas, pipeline.recomposer)

        composition.setContent {
            NekoApp()
        }

        // Frame 1
        pipeline.frame()
        canvas.resize(1920.0f, 1080.0f)
        canvas.layout()

        fun findNodeWithText(node: UINode, substring: String): UINode? {
            if (node is TextNode && node.text.contains(substring)) return node
            for (child in node.children) {
                val found = findNodeWithText(child, substring)
                if (found != null) return found
            }
            return null
        }

        fun findClickableAncestor(node: UINode): UINode? {
            var curr: UINode? = node
            while (curr != null && curr !== canvas) {
                if (curr.onClick != null) return curr
                curr = curr.parent
            }
            return null
        }

        val textNode = findNodeWithText(canvas, "So lan bam:")
        assertTrue(textNode != null, "Text node with 'So lan bam:' should exist")

        val buttonNode = findClickableAncestor(textNode)
        assertTrue(buttonNode != null, "Clickable button ancestor should exist")

        println("Button bounds: ${buttonNode.bounds}")
        val clickX = (buttonNode.bounds.x + buttonNode.bounds.width * 0.5f).toInt()
        val clickY = (buttonNode.bounds.y + buttonNode.bounds.height * 0.5f).toInt()

        println("Clicking button at ($clickX, $clickY)")

        // 1. First click on button
        inputProcessor.touchDown(clickX, clickY, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(clickX, clickY, 0, KeyCode.mouseLeft)

        // Process frame
        pipeline.frame()
        canvas.layout()

        val updatedTextNode = findNodeWithText(canvas, "So lan bam: 1")
        println("After 1st click, found 'So lan bam: 1': ${updatedTextNode != null}")

        assertEquals(true, updatedTextNode != null, "Button click count should increment to 1 on first click")
    }
}
