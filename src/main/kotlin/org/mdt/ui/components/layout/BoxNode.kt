package org.mdt.ui.components.layout

/**
 * ## BoxNode
 *
 * Rectangular container node inheriting [LayoutNode] with pre-initialized [BoxVisuals].
 *
 * See: docs/architecture/architecture_en.md
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
open class BoxNode : LayoutNode() {
    init {
        ensureVisuals()
    }
}
