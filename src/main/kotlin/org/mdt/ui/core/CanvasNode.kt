package org.mdt.ui.core

import arc.util.Log
import org.mdt.ui.layout.GodotLayout
import org.mdt.ui.render.EngineRenderer

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

        GodotLayout.layoutAnchors(children, screenWidth, screenHeight)

        super.layout()
        isLayoutDirty = false

        Log.info("[CanvasNode Layout Tree]\n" + dumpTree())
    }

    override fun draw(renderer: EngineRenderer) {
        if (isLayoutDirty) {
            layout()
        }
        super.draw(renderer)
    }
}
