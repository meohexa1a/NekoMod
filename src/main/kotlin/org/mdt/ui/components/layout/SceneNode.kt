package org.mdt.ui.components.layout

import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.LayoutPreset

/**
 * ## SceneNode
 *
 * Virtual DOM root container representing an explicit Artboard Scene Frame on the Canvas.
 * Supports standard device presets, custom dimensions, background fill, and infinite height mode.
 *
 * See: docs/layout-engine/layout_engine_en.md
 */
class SceneNode(
    name: String = "MainScene",
    var preset: String = "desktop_720p",
    var artboardWidth: Float = 1280f,
    var artboardHeight: Float = 720f,
    var isInfiniteHeight: Boolean = false,
    var backgroundColor: Color = Color.valueOf("181926")
) : LayoutNode() {

    init {
        this.id = "scene_root"
        this.name = name
        this.width = artboardWidth
        this.height = artboardHeight
        this.anchorData.setPreset(LayoutPreset.FULL_RECT)

        val vis = ensureVisuals()
        vis.background.mode = BackgroundFill.Mode.COLOR
        vis.background.color = backgroundColor
        vis.border.width = 1f
        vis.border.color = Color(1f, 1f, 1f, 0.08f)
    }

    /**
     * Updates the Artboard dimensions and refreshes the layout.
     */
    fun setArtboardSize(w: Float, h: Float, presetName: String = "custom") {
        this.artboardWidth = w
        this.artboardHeight = h
        this.preset = presetName
        this.width = w
        this.height = if (isInfiniteHeight) -1f else h
        invalidateLayout()
    }

    /**
     * Applies a standard device preset.
     */
    fun applyPreset(presetName: String) {
        this.preset = presetName
        when (presetName) {
            "desktop_1080p" -> setArtboardSize(1920f, 1080f, presetName)
            "desktop_720p" -> setArtboardSize(1280f, 720f, presetName)
            "mobile_portrait" -> setArtboardSize(390f, 844f, presetName)
            "mobile_landscape" -> setArtboardSize(844f, 390f, presetName)
            "dialog_modal" -> setArtboardSize(600f, 400f, presetName)
            "fluid" -> {
                this.width = -1f
                this.height = -1f
                invalidateLayout()
            }
        }
    }
}
