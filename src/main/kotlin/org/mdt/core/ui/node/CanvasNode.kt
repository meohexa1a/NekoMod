package org.mdt.core.ui.node

import org.mdt.core.ui.layout.GodotLayout
import org.mdt.core.ui.layout.SizeFlags

/**
 * ## CanvasNode [Root Screen Node]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Root virtual DOM container representing the full game display surface / viewport.
 * - Manages window resizing and drives layout passes before rendering child nodes.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Full screen bounds start at OpenGL bottom-left `(0, 0)` with width/height matching viewport pixels.
 * - [x] Capped to a maximum of 3 layout passes per frame to prevent infinite layout loops.
 * - [x] `resize(width, height)` updates `bounds`, `screenWidth`, and `screenHeight` and invalidates layout.
 *
 * ### 3. Related Files
 * - Runtime Orchestrator: `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 * - Layout Engine: `src/main/kotlin/org/mdt/core/ui/layout/GodotLayout.kt`
 * - Input Processor: `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - GPU Batcher: `src/main/kotlin/org/mdt/core/platform/render/UIBatch.kt`
 */
class CanvasNode : UINode() {

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

    override fun draw() {
        var layoutPass = 0
        while (isLayoutDirty && layoutPass < 3) {
            layout()
            layoutPass++
        }
        super.draw()
    }
}
