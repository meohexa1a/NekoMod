package org.mdt.core.ui.node

import org.mdt.core.ui.layout.GodotLayout
import org.mdt.core.ui.layout.SizeFlags

/**
 * ## CanvasNode [Root Viewport Virtual DOM Node]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Root container Virtual DOM node representing the full game display surface / window viewport.
 * - Automatically distributes layout bounds across root children using full-screen anchors or flex fill fitting.
 * - Manages display resizing and drives iterative layout passes prior to frame rendering.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (OpenGL Bottom-Left Origin):** Full screen bounds map from `(x=0, y=0)` with width and height matching viewport pixels.
 * - **Rule 2 (Layout Pass Cap):** Guarded to maximum 3 layout passes to prevent infinite invalidation loops.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - ⚙️ **Runtime Host:** `src/main/kotlin/org/mdt/core/ui/EngineRuntime.kt`
 * - 📐 **Layout Engine:** `src/main/kotlin/org/mdt/core/ui/layout/GodotLayout.kt`
 * - 🎮 **Input Processor:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] `resize(width, height)` updates `bounds`, `screenWidth`, and `screenHeight` and invalidates layout.
 * - [x] Root children fill full screen width/height when explicit anchors are omitted.
 * - [x] Iterative layout pass stabilizes layout before calling `super.draw()`.
 */
class CanvasNode : UINode() {

    // --- PROPERTIES ---

    var screenWidth: Float = 0.0f
        private set
    var screenHeight: Float = 0.0f
        private set

    // --- VIEWPORT & RESIZE ---

    fun resize(width: Float, height: Float) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
            bounds.set(0.0f, 0.0f, width, height)
            invalidateLayout()
        }
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
    }
}
