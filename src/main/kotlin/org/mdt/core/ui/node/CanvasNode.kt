// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Root Screen Virtual DOM Node & Viewport Container.
// - Operating Mechanism: Full-screen root container; receives window resizing events; drives up to 3 layout passes per frame.
// - Invariants: Full screen bounds start at OpenGL bottom-left (0, 0); capped at max 3 layout iterations per frame.
// - Dependencies: [EngineRuntime], [GodotLayout], [EngineInputProcessor], [UINode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.ui.layout.GodotLayout
import org.mdt.core.ui.layout.SizeFlags

/**
 * ## CanvasNode
 *
 * Root virtual DOM container representing the full display surface / viewport.
 * Manages window resizing and drives multi-pass layout recalculation before rendering child nodes.
 *
 * @property screenWidth Current viewport width in pixels.
 * @property screenHeight Current viewport height in pixels.
 *
 * @see UINode
 * @see org.mdt.core.ui.EngineRuntime
 * @see GodotLayout
 */
class CanvasNode(
    val hostProvider: () -> PlatformHost = { PlatformHost.NoOp }
) : UINode() {

    var inputProcessor: org.mdt.core.ui.input.EngineInputProcessor? = null

    // --- PROPERTIES ---

    var screenWidth: Float = 0.0f
        private set
    var screenHeight: Float = 0.0f
        private set

    // --- VIEWPORT & RESIZE ---

    fun resize(width: Float, height: Float) {
        if (screenWidth == width && screenHeight == height) return

        screenWidth = width
        screenHeight = height
        bounds.set(0.0f, 0.0f, width, height)
        invalidateLayout()
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

            when {
                hasExplicitAnchor -> {
                    GodotLayout.layoutSingleAnchor(child, 0.0f, 0.0f, screenWidth, screenHeight)
                }
                else -> {
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
        }

        super.layout()
        isLayoutDirty = false
    }

    /**
     * Overrides [UINode.buildHitPath] to skip the root-level bounds check.
     * The canvas represents the full viewport and should always pass children for hit-path resolution.
     */
    override fun buildHitPath(pointX: Float, pointY: Float, path: ArrayList<UINode>): Boolean {
        for (i in children.indices.reversed()) {
            if (children[i].buildHitPath(pointX, pointY, path)) return true
        }
        return false
    }

    /**
     * Overrides [UINode.hitTest] to skip the root-level bounds check.
     */
    override fun hitTest(pointX: Float, pointY: Float): UINode? {
        for (i in children.indices.reversed()) {
            val hit = children[i].hitTest(pointX, pointY)
            if (hit != null) return hit
        }
        return null
    }

    override fun draw(batch: UIBatch) {
        var layoutPass = 0
        while (isLayoutDirty && layoutPass < 3) {
            layout()
            layoutPass++
        }
        super.draw(batch)
    }
}
