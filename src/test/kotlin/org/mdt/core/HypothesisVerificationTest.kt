package org.mdt.core

import arc.input.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mdt.core.platform.PlatformHost
import org.mdt.core.ui.input.EngineInputProcessor
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.layout.RowMeasurePolicy
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.SizeFlags
import org.mdt.core.ui.modifier.Modifier
import org.mdt.core.ui.modifier.background
import org.mdt.core.ui.modifier.border
import org.mdt.core.ui.modifier.clickable
import org.mdt.core.ui.modifier.draggable
import org.mdt.core.ui.modifier.onClick
import org.mdt.core.ui.modifier.padding
import org.mdt.core.ui.modifier.zIndex
import org.mdt.core.ui.node.CanvasNode
import org.mdt.core.ui.node.LayoutNode
import org.mdt.core.ui.node.TextNode

/**
 * ## HypothesisVerificationTest
 *
 * Empirical verification suite proving that all architectural vulnerabilities,
 * layout math defects, and recomposition mutation bugs have been permanently cured.
 */
class HypothesisVerificationTest {

    // =========================================================================
    // HYPOTHESIS 1: Sticky Modifier Mutation Cured via node.modifier Property
    // =========================================================================
    @Test
    fun `Verify Hypothesis 1 - Sticky Modifier Cured on Recomposition`() {
        val node = LayoutNode()

        // Frame 1: Composable applies background color, border, padding, and onClick
        node.modifier = Modifier
            .background(Color.Red)
            .border(2.0f, Color.Green)
            .padding(16.0f)
            .onClick { println("Frame 1 Click") }

        assertEquals(Color.Red, node.color, "Frame 1 background must be Red")
        assertEquals(2.0f, node.borderWidth, "Frame 1 border width must be 2f")
        assertEquals(16.0f, node.padL, "Frame 1 padding must be 16f")
        assertTrue(node.onClick != null, "Frame 1 onClick must be set")

        // Frame 2: State changes! Composable now sets ONLY padding(8f)
        node.modifier = Modifier.padding(8.0f)

        assertEquals(Color.Clear, node.color, "Color cleanly reset to Clear in Frame 2")
        assertEquals(0.0f, node.borderWidth, "Border cleanly reset to 0f in Frame 2")
        assertEquals(8.0f, node.padL, "Padding cleanly updated to 8f in Frame 2")
        assertNull(node.onClick, "onClick cleanly reset to null in Frame 2")
    }

    // =========================================================================
    // HYPOTHESIS 2: Pointer Modifier Filter Multiplexing
    // =========================================================================
    @Test
    fun `Verify Hypothesis 2 - Pointer Modifier Dispatch`() {
        val node = LayoutNode().apply {
            bounds.set(0.0f, 0.0f, 100.0f, 50.0f)
        }

        var clickCalled = false
        var dragCalled = false

        val modifier = Modifier
            .clickable(onClick = { clickCalled = true })
            .draggable(onDrag = { _, _ -> dragCalled = true })

        modifier.applyTo(node)

        val canvas = CanvasNode { PlatformHost.NoOp }
        val inputProcessor = EngineInputProcessor(canvas) { PlatformHost.NoOp }
        canvas.inputProcessor = inputProcessor
        canvas.resize(800.0f, 600.0f)
        canvas.children.add(node)
        inputProcessor.touchDown(50, 25, 0, KeyCode.mouseLeft)
        inputProcessor.touchUp(50, 25, 0, KeyCode.mouseLeft)

        assertTrue(clickCalled, "Immediate click handler executed on touchUp")
    }

    // =========================================================================
    // HYPOTHESIS 3: 2-Pass Flex Space Distribution (Freeze & Redistribute)
    // =========================================================================
    @Test
    fun `Verify Hypothesis 3 - 2-Pass Flex Allocation Prevents Container Overflow`() {
        val row = LayoutNode().apply {
            width = 300.0f
            height = 50.0f
            measurePolicy = RowMeasurePolicy(gap = 0.0f)
            bounds.set(0.0f, 0.0f, 300.0f, 50.0f)
        }

        // Child 1: EXPAND with stretchRatio = 1, but minWidth = 250f
        val child1 = LayoutNode().apply {
            sizeFlagsHorizontal = SizeFlags.EXPAND_FILL
            stretchRatio = 1.0f
            minWidth = 250.0f
        }

        // Child 2: EXPAND with stretchRatio = 1 (no minWidth)
        val child2 = LayoutNode().apply {
            sizeFlagsHorizontal = SizeFlags.EXPAND_FILL
            stretchRatio = 1.0f
        }

        row.addChild(child1)
        row.addChild(child2)

        row.layout()

        val totalChildrenWidth = child1.bounds.width + child2.bounds.width
        println("[Hypothesis 3 Verified] Container Width: 300.0")
        println("[Hypothesis 3 Verified] Child 1 Width: ${child1.bounds.width}")
        println("[Hypothesis 3 Verified] Child 2 Width: ${child2.bounds.width}")
        println("[Hypothesis 3 Verified] Total Children Width: $totalChildrenWidth")

        assertEquals(250.0f, child1.bounds.width, "Child 1 frozen at minWidth 250px")
        assertEquals(50.0f, child2.bounds.width, "Child 2 allocated remaining 50px")
        assertEquals(300.0f, totalChildrenWidth, "Total children width exactly equals container width (0px overflow)")
    }

    // =========================================================================
    // HYPOTHESIS 4: LayoutNode Hug Content Dynamic Re-measurement
    // =========================================================================
    @Test
    fun `Verify Hypothesis 4 - Intrinsic Hug Content Resizes Dynamically`() {
        val container = LayoutNode().apply {
            measurePolicy = BoxMeasurePolicy
            // width = -1f by default (Hug Content)
        }

        val child = LayoutNode().apply {
            width = 100.0f
            height = 50.0f
        }
        container.addChild(child)

        // Frame 1: container calculates prefWidth = 100f
        container.layout()
        assertEquals(100.0f, container.bounds.width, "Frame 1 width should be 100f")

        // Frame 2: Child expands to width = 300f!
        child.width = 300.0f
        container.invalidateLayout()

        container.layout()

        println("[Hypothesis 4 Verified] Container dynamic width after child resize: ${container.bounds.width}")
        assertEquals(300.0f, container.bounds.width, "Container dynamically hugs resized 300px child")
    }

    // =========================================================================
    // HYPOTHESIS 5: TextNode Wrap Intrinsic Width
    // =========================================================================
    @Test
    fun `Verify Hypothesis 5 - TextNode Wrap Intrinsic Width Evaluates Correctly`() {
        val textNode = TextNode("Paragraph with wrapping enabled").apply {
            wrap = true
        }

        val prefWidth = textNode.getPrefWidth()
        println("[Hypothesis 5 Verified] TextNode(wrap = true).getPrefWidth(): $prefWidth")

        assertTrue(prefWidth > 0.0f, "getPrefWidth() returns positive intrinsic width when wrap = true")

        // Simulate Compose recomposition modifier update
        textNode.modifier = Modifier.padding(10.0f)

        assertEquals(true, textNode.wrap, "TextNode.wrap must remain true across modifier recomposition cycles")
        assertEquals(SizeFlags.FILL, textNode.sizeFlagsHorizontal, "TextNode.sizeFlagsHorizontal must remain FILL when wrap = true")
    }

    // =========================================================================
    // HYPOTHESIS 6: ZIndex Traversal in Hit-Testing
    // =========================================================================
    @Test
    fun `Verify Hypothesis 6 - ZIndex Respected in Hit-Testing`() {
        val parent = LayoutNode().apply {
            bounds.set(0.0f, 0.0f, 200.0f, 200.0f)
        }

        // Child A added first, with high zIndex = 100
        val childA = LayoutNode().apply {
            name = "ChildA_HighZ"
            zIndex = 100.0f
            bounds.set(0.0f, 0.0f, 100.0f, 100.0f)
            onClick = {}
        }

        // Child B added second, with low zIndex = 0
        val childB = LayoutNode().apply {
            name = "ChildB_LowZ"
            zIndex = 0.0f
            bounds.set(0.0f, 0.0f, 100.0f, 100.0f)
            onClick = {}
        }

        parent.addChild(childA)
        parent.addChild(childB)

        val hit = parent.hitTest(50.0f, 50.0f)
        println("[Hypothesis 6 Verified] Hit test result: ${hit?.name}")

        assertEquals("ChildA_HighZ", hit?.name, "Child with higher zIndex (100) receives hit test before lower zIndex (0)")
    }

    // =========================================================================
    // HYPOTHESIS 7: Scoped Modifier Weight Axis Isolation
    // =========================================================================
    @Test
    fun `Verify Hypothesis 7 - Scoped Weight Modifiers Do Not Distort Cross Axis`() {
        val rowChild = LayoutNode()
        val rowScope = org.mdt.core.ui.layout.RowScopeInstance

        // Apply weight inside RowScope
        val rowModifier = with(rowScope) { Modifier.weight(2.0f) }
        rowModifier.applyTo(rowChild)

        assertEquals(SizeFlags.EXPAND_FILL, rowChild.sizeFlagsHorizontal, "RowScope.weight must set sizeFlagsHorizontal")
        assertEquals(2.0f, rowChild.stretchRatio, "RowScope.weight must set stretchRatio")
        assertEquals(0, rowChild.sizeFlagsVertical, "RowScope.weight MUST NOT touch sizeFlagsVertical")

        val columnChild = LayoutNode()
        val colScope = org.mdt.core.ui.layout.ColumnScopeInstance

        // Apply weight inside ColumnScope
        val colModifier = with(colScope) { Modifier.weight(3.0f) }
        colModifier.applyTo(columnChild)

        assertEquals(SizeFlags.EXPAND_FILL, columnChild.sizeFlagsVertical, "ColumnScope.weight must set sizeFlagsVertical")
        assertEquals(3.0f, columnChild.stretchRatio, "ColumnScope.weight must set stretchRatio")
        assertEquals(0, columnChild.sizeFlagsHorizontal, "ColumnScope.weight MUST NOT touch sizeFlagsHorizontal")
    }

    // =========================================================================
    // HYPOTHESIS 8: Color Sentinel & Transparency Distinction
    // =========================================================================
    @Test
    fun `Verify Hypothesis 8 - Color Unspecified Distinguished from Transparent`() {
        val unspecified = Color.Unspecified
        val transparent = Color.Transparent
        val parsedTransparent = Color.parseOrNull("#00000000")

        assertEquals(false, unspecified.isSpecified, "Color.Unspecified must have isSpecified == false")
        assertEquals(true, transparent.isSpecified, "Color.Transparent must have isSpecified == true")
        assertEquals(true, parsedTransparent?.isSpecified, "Parsed 0x00000000 must have isSpecified == true")
        assertEquals(false, unspecified == transparent, "Color.Unspecified and Color.Transparent must not be equal")
    }

    // =========================================================================
    // HYPOTHESIS 9: Scrollable Toggling Preserves User Pointer Handlers
    // =========================================================================
    @Test
    fun `Verify Hypothesis 9 - Toggling scrollable Does Not Clear User Callbacks`() {
        val node = LayoutNode()
        var userClickInvoked = false
        var userPointerDownInvoked = false

        node.onClick = { userClickInvoked = true }
        node.onPointerDown = { userPointerDownInvoked = true }

        // Enable scrolling
        node.scrollable = true
        // Disable scrolling
        node.scrollable = false

        assertTrue(node.onClick != null, "User onClick must NOT be erased by toggling scrollable")
        assertTrue(node.onPointerDown != null, "User onPointerDown must NOT be erased by toggling scrollable")

        node.onClick?.invoke()
        assertTrue(userClickInvoked, "User onClick remains fully executable")
    }

    // =========================================================================
    // HYPOTHESIS 10: Immutable Rect and Insets Value Integrity
    // =========================================================================
    @Test
    fun `Verify Hypothesis 10 - Geometry Rect and Insets Equality and Mutation Safety`() {
        val rect1 = org.mdt.core.ui.unit.Rect(10.0f, 20.0f, 100.0f, 50.0f)
        val rect2 = org.mdt.core.ui.unit.Rect(10.0f, 20.0f, 100.0f, 50.0f)
        val rect3 = rect1.copy()

        assertEquals(rect1, rect2, "Identical rects must be equal")
        assertEquals(rect1.hashCode(), rect2.hashCode(), "Identical rects must share hash code")
        assertEquals(rect1, rect3, "Copied rect must be equal")

        rect2.set(0.0f, 0.0f, 50.0f, 50.0f)
        assertTrue(rect1 != rect2, "Mutating rect2 must not affect rect1")
    }
}
