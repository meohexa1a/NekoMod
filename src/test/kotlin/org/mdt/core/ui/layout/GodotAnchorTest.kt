package org.mdt.core.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mdt.core.ui.unit.AnchorData
import org.mdt.core.ui.unit.GrowDirection
import org.mdt.core.ui.unit.LayoutPreset

/**
 * ## GodotAnchorTest [Pillar 1: Complete Anchor & Offset Matrix Unit Tests]
 *
 * Exhaustively validates all 16 Godot 4-aligned Anchor Presets, the full 3x3 GrowDirection matrix,
 * custom normalized ratios, pixel offsets, margin compounding, and multi-stage parent resizing.
 */
class GodotAnchorTest {

    // --- ALL 16 ANCHOR PRESETS ---

    @Test
    fun `Preset 1 - TOP_LEFT places child at top-left edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 100.0f, height = 50.0f).apply {
            anchorData.setPreset(LayoutPreset.TOP_LEFT)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 550.0f, expectedW = 100.0f, expectedH = 50.0f)
    }

    @Test
    fun `Preset 2 - TOP_RIGHT places child at top-right edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 120.0f, height = 60.0f).apply {
            anchorData.setPreset(LayoutPreset.TOP_RIGHT)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 680.0f, expectedY = 540.0f, expectedW = 120.0f, expectedH = 60.0f)
    }

    @Test
    fun `Preset 3 - BOTTOM_LEFT places child at bottom-left corner`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 150.0f, height = 40.0f).apply {
            anchorData.setPreset(LayoutPreset.BOTTOM_LEFT)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 150.0f, expectedH = 40.0f)
    }

    @Test
    fun `Preset 4 - BOTTOM_RIGHT places child at bottom-right corner`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 110.0f, height = 45.0f).apply {
            anchorData.setPreset(LayoutPreset.BOTTOM_RIGHT)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 690.0f, expectedY = 0.0f, expectedW = 110.0f, expectedH = 45.0f)
    }

    @Test
    fun `Preset 5 - CENTER_LEFT centers vertically along left edge`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(width = 80.0f, height = 100.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER_LEFT)
        }
        parent.children.add(child)
        parent.layout()

        // X = 0, Y = (400 - 100) / 2 = 150
        child.assertBounds(expectedX = 0.0f, expectedY = 150.0f, expectedW = 80.0f, expectedH = 100.0f)
    }

    @Test
    fun `Preset 6 - CENTER_TOP centers horizontally along top edge`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(width = 120.0f, height = 50.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER_TOP)
        }
        parent.children.add(child)
        parent.layout()

        // X = (600 - 120) / 2 = 240, Y = 400 - 50 = 350
        child.assertBounds(expectedX = 240.0f, expectedY = 350.0f, expectedW = 120.0f, expectedH = 50.0f)
    }

    @Test
    fun `Preset 7 - CENTER_RIGHT centers vertically along right edge`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(width = 90.0f, height = 120.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER_RIGHT)
        }
        parent.children.add(child)
        parent.layout()

        // X = 600 - 90 = 510, Y = (400 - 120) / 2 = 140
        child.assertBounds(expectedX = 510.0f, expectedY = 140.0f, expectedW = 90.0f, expectedH = 120.0f)
    }

    @Test
    fun `Preset 8 - CENTER_BOTTOM centers horizontally along bottom edge`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(width = 140.0f, height = 60.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER_BOTTOM)
        }
        parent.children.add(child)
        parent.layout()

        // X = (600 - 140) / 2 = 230, Y = 0
        child.assertBounds(expectedX = 230.0f, expectedY = 0.0f, expectedW = 140.0f, expectedH = 60.0f)
    }

    @Test
    fun `Preset 9 - CENTER positions child in the absolute middle`() {
        val parent = createBox(width = 500.0f, height = 300.0f)
        val child = createNode(width = 100.0f, height = 80.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 200.0f, expectedY = 110.0f, expectedW = 100.0f, expectedH = 80.0f)
    }

    @Test
    fun `Preset 10 - LEFT_WIDE stretches vertically along left edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 75.0f).apply {
            anchorData.setPreset(LayoutPreset.LEFT_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 75.0f, expectedH = 600.0f)
    }

    @Test
    fun `Preset 11 - TOP_WIDE stretches horizontally along top edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(height = 55.0f).apply {
            anchorData.setPreset(LayoutPreset.TOP_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 545.0f, expectedW = 800.0f, expectedH = 55.0f)
    }

    @Test
    fun `Preset 12 - RIGHT_WIDE stretches vertically along right edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(width = 85.0f).apply {
            anchorData.setPreset(LayoutPreset.RIGHT_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 715.0f, expectedY = 0.0f, expectedW = 85.0f, expectedH = 600.0f)
    }

    @Test
    fun `Preset 13 - BOTTOM_WIDE stretches horizontally along bottom edge`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(height = 65.0f).apply {
            anchorData.setPreset(LayoutPreset.BOTTOM_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 800.0f, expectedH = 65.0f)
    }

    @Test
    fun `Preset 14 - VCENTER_WIDE stretches vertically and centers horizontally`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(width = 120.0f).apply {
            anchorData.setPreset(LayoutPreset.VCENTER_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        // X = (600 - 120) / 2 = 240, Y = 0, H = 400
        child.assertBounds(expectedX = 240.0f, expectedY = 0.0f, expectedW = 120.0f, expectedH = 400.0f)
    }

    @Test
    fun `Preset 15 - HCENTER_WIDE stretches horizontally and centers vertically`() {
        val parent = createBox(width = 600.0f, height = 400.0f)
        val child = createNode(height = 80.0f).apply {
            anchorData.setPreset(LayoutPreset.HCENTER_WIDE)
        }
        parent.children.add(child)
        parent.layout()

        // X = 0, W = 600, Y = (400 - 80) / 2 = 160, H = 80
        child.assertBounds(expectedX = 0.0f, expectedY = 160.0f, expectedW = 600.0f, expectedH = 80.0f)
    }

    @Test
    fun `Preset 16 - FULL_RECT fills the entire parent area`() {
        val parent = createBox(width = 1024.0f, height = 768.0f)
        val child = createNode().apply {
            anchorData.setPreset(LayoutPreset.FULL_RECT)
        }
        parent.children.add(child)
        parent.layout()

        child.assertBounds(expectedX = 0.0f, expectedY = 0.0f, expectedW = 1024.0f, expectedH = 768.0f)
    }

    // --- GROW DIRECTION MATRIX (3x3) ---

    @Test
    fun `GrowDirection - BEGIN x BEGIN grows towards left and upwards`() {
        val parent = createBox(width = 400.0f, height = 400.0f)
        val node = createNode(width = 60.0f, height = 40.0f).apply {
            anchorData.isEnabled = true
            anchorData.setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
            anchorData.growHorizontal = GrowDirection.BEGIN
            anchorData.growVertical = GrowDirection.BEGIN
        }
        parent.children.add(node)
        parent.layout()

        // Center is (200, 200).
        // growH = BEGIN: X goes left from 200 -> 200 - 60 = 140
        // growV = BEGIN: Y goes upwards in OpenGL Y from 200 -> 200
        node.assertBounds(expectedX = 140.0f, expectedY = 200.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `GrowDirection - END x END grows towards right and downwards`() {
        val parent = createBox(width = 400.0f, height = 400.0f)
        val node = createNode(width = 60.0f, height = 40.0f).apply {
            anchorData.isEnabled = true
            anchorData.setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
            anchorData.growHorizontal = GrowDirection.END
            anchorData.growVertical = GrowDirection.END
        }
        parent.children.add(node)
        parent.layout()

        // Center is (200, 200).
        // growH = END: X goes right from 200 -> 200
        // growV = END: Y goes downwards in OpenGL Y from 200 -> 200 - 40 = 160
        node.assertBounds(expectedX = 200.0f, expectedY = 160.0f, expectedW = 60.0f, expectedH = 40.0f)
    }

    @Test
    fun `GrowDirection - BOTH x BOTH centers equally around anchor point`() {
        val parent = createBox(width = 400.0f, height = 400.0f)
        val node = createNode(width = 80.0f, height = 60.0f).apply {
            anchorData.isEnabled = true
            anchorData.setAnchors(0.5f, 0.5f, 0.5f, 0.5f)
            anchorData.growHorizontal = GrowDirection.BOTH
            anchorData.growVertical = GrowDirection.BOTH
        }
        parent.children.add(node)
        parent.layout()

        // Center is (200, 200). Size (80, 60) -> X: 200 - 40 = 160, Y: 200 - 30 = 170
        node.assertBounds(expectedX = 160.0f, expectedY = 170.0f, expectedW = 80.0f, expectedH = 60.0f)
    }

    // --- CUSTOM RATIOS & MULTI-STAGE PARENT RESIZE ---

    @Test
    fun `Custom anchor quadrant with offsets and margins`() {
        val parent = createBox(width = 800.0f, height = 600.0f)
        val child = createNode(marginL = 10.0f, marginR = 10.0f, marginT = 5.0f, marginB = 5.0f).apply {
            anchorData.isEnabled = true
            anchorData.setAnchors(0.25f, 0.25f, 0.75f, 0.75f)
            anchorData.offsetLeft = 20.0f
            anchorData.offsetRight = -20.0f
            anchorData.offsetTop = 10.0f
            anchorData.offsetBottom = -10.0f
        }
        parent.children.add(child)
        parent.layout()

        // Parent W=800, H=600
        // X1 = 800*0.25 + 20 + 10 = 230
        // X2 = 800*0.75 - 20 - 10 = 570 -> Width = 570 - 230 = 340
        // Y1 (bottom-left): parentH*(1-0.75) + (-10) + 5 = 600*0.25 - 5 = 145
        // Y2: parentH*(1-0.25) - 10 - 5 = 600*0.75 - 15 = 435 -> Height = 435 - 145 = 290
        child.assertBounds(expectedX = 230.0f, expectedY = 145.0f, expectedW = 340.0f, expectedH = 290.0f)
    }

    @Test
    fun `Multi-stage parent resize propagates continuously without drift`() {
        val parent = createBox(width = 100.0f, height = 100.0f)
        val card = createNode(width = 60.0f, height = 40.0f).apply {
            anchorData.setPreset(LayoutPreset.CENTER)
        }
        parent.children.add(card)

        // Stage 1: 100x100 -> (100-60)/2 = 20, (100-40)/2 = 30
        parent.layout()
        card.assertBounds(expectedX = 20.0f, expectedY = 30.0f, expectedW = 60.0f, expectedH = 40.0f)

        // Stage 2: 800x600 -> (800-60)/2 = 370, (600-40)/2 = 280
        parent.width = 800.0f
        parent.height = 600.0f
        parent.layout()
        card.assertBounds(expectedX = 370.0f, expectedY = 280.0f, expectedW = 60.0f, expectedH = 40.0f)

        // Stage 3: 1920x1080 -> (1920-60)/2 = 930, (1080-40)/2 = 520
        parent.width = 1920.0f
        parent.height = 1080.0f
        parent.layout()
        card.assertBounds(expectedX = 930.0f, expectedY = 520.0f, expectedW = 60.0f, expectedH = 40.0f)

        // Stage 4: 3840x2160 (4K) -> (3840-60)/2 = 1890, (2160-40)/2 = 1060
        parent.width = 3840.0f
        parent.height = 2160.0f
        parent.layout()
        card.assertBounds(expectedX = 1890.0f, expectedY = 1060.0f, expectedW = 60.0f, expectedH = 40.0f)
    }
}
