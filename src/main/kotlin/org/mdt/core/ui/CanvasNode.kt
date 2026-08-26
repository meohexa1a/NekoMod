package org.mdt.core.ui

import org.mdt.core.ui.layout.GodotLayout
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.display.tooltip.TooltipManager

/**
 * ## CanvasNode
 *
 * Root Virtual DOM container node representing the game's full window viewport.
 * Automatically distributes layout bounds to child UI trees using full-screen anchors and flex fitting.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
class CanvasNode : UINode() {
    var screenWidth: Float = 0f
        private set
    var screenHeight: Float = 0f
        private set

    fun resize(width: Float, height: Float) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
            bounds.set(0f, 0f, width, height)
            invalidateLayout()
        }
    }

    override fun layout() {
        if (!isLayoutDirty && bounds.width == screenWidth && bounds.height == screenHeight) return
        bounds.set(0f, 0f, screenWidth, screenHeight)

        for (child in children) {
            if (!child.visible) continue
            val anchor = child.anchorData
            val hasExplicitAnchor = anchor.anchorLeft != 0f || anchor.anchorRight != 0f || anchor.anchorTop != 0f || anchor.anchorBottom != 0f ||
                    anchor.offsetLeft != 0f || anchor.offsetRight != 0f || anchor.offsetTop != 0f || anchor.offsetBottom != 0f

            if (hasExplicitAnchor) {
                GodotLayout.layoutSingleAnchor(child, 0f, 0f, screenWidth, screenHeight)
            } else {
                // Root children without explicit anchors fill the full canvas viewport
                GodotLayout.fitChildInRect(
                    child = child,
                    rectX = 0f,
                    rectY = 0f,
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

    override fun draw(renderer: EngineRenderer) {
        if (isLayoutDirty) layout()
        super.draw(renderer)

        // Draw top-layer overlays (Tooltips, Modals, Popups) strictly above all children
        TooltipManager.drawTopLayer(renderer)
    }
}
