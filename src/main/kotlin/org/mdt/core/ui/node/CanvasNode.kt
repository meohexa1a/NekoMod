package org.mdt.core.ui.node

import org.mdt.core.ui.layout.GodotLayout
import org.mdt.core.ui.layout.SizeFlags

/**
 * ## CanvasNode
 *
 * Root Virtual DOM container node representing the game's full window viewport.
 * Automatically distributes layout bounds to child UI trees using full-screen anchors and flex fitting,
 * and hosts top-layer overlay elements rendered in the final draw pass.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class CanvasNode : UINode(), OverlayHost {

    // --- PROPERTIES & OVERLAYS ---

    var screenWidth: Float = 0.0f
        private set
    var screenHeight: Float = 0.0f
        private set

    private val overlayNodes = ArrayList<UINode>()

    // --- VIEWPORT & RESIZE ---

    fun resize(width: Float, height: Float) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
            bounds.set(0.0f, 0.0f, width, height)
            invalidateLayout()
        }
    }

    // --- OVERLAY HOST IMPLEMENTATION ---

    override fun registerOverlay(node: UINode) {
        if (!overlayNodes.contains(node)) {
            overlayNodes.add(node)
        }
    }

    override fun unregisterOverlay(node: UINode) {
        overlayNodes.remove(node)
    }

    override fun clearOverlays() {
        overlayNodes.clear()
    }

    // --- LAYOUT & DRAW ---

    override fun layout() {
        if (!isLayoutDirty && bounds.width == screenWidth && bounds.height == screenHeight) return

        bounds.set(0.0f, 0.0f, screenWidth, screenHeight)

        for (i in children.indices) {
            val child = children[i]
            if (!child.visible) continue

            val anchor = child.anchorData
            val hasExplicitAnchor = anchor.isEnabled ||
                    anchor.anchorLeft != 0.0f || anchor.anchorRight != 0.0f || anchor.anchorTop != 0.0f || anchor.anchorBottom != 0.0f ||
                    anchor.offsetLeft != 0.0f || anchor.offsetRight != 0.0f || anchor.offsetTop != 0.0f || anchor.offsetBottom != 0.0f

            if (hasExplicitAnchor) {
                GodotLayout.layoutSingleAnchor(child, 0.0f, 0.0f, screenWidth, screenHeight)
            } else {
                // Root children without explicit anchors fill the full canvas viewport
                GodotLayout.fitChildInRect(
                    child = child,
                    rectX = 0.0f,
                    rectY = 0.0f,
                    rectWidth = screenWidth,
                    rectHeight = screenHeight,
                    horizontalFlags = child.sizeFlagsHorizontal or SizeFlags.FILL,
                    verticalFlags = child.sizeFlagsVertical or SizeFlags.FILL
                )
            }
        }

        super.layout()
        isLayoutDirty = false
    }

    override fun draw() {
        var layoutPass = 0
        while (isLayoutDirty && layoutPass < 3) {
            layout()
            layoutPass++
        }
        super.draw()

        // Draw top-layer overlays (Tooltips, Modals, Popups) strictly above all children in the same UIBatch pass
        for (i in overlayNodes.indices) {
            val node = overlayNodes[i]
            if (node.visible) {
                node.draw()
            }
        }
    }
}
