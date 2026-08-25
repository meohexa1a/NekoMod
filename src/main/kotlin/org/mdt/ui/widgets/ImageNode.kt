package org.mdt.ui.widgets

import arc.graphics.Color
import arc.graphics.g2d.Draw
import org.mdt.core.cache.TextureHandle
import org.mdt.core.image.ImageLoader
import org.mdt.core.image.ImageSource
import org.mdt.ui.core.UINode
import org.mdt.ui.render.EngineRenderer

/**
 * Image aspect scaling behavior.
 */
enum class ScaleMode {
    /** Fits entire image within bounds while preserving aspect ratio. */
    FIT,
    /** Crops edges to fill the entire bounding box without distortion. */
    CROP,
    /** Stretches image to completely fill bounding box. */
    STRETCH
}

/**
 * ## ImageNode
 *
 * Virtual UI node rendering remote, local, or asset images with lifecycle-safe
 * texture reference management.
 */
open class ImageNode : UINode() {

    var scaleMode: ScaleMode = ScaleMode.FIT
    var tint: Color = Color(Color.white)

    private var activeHandle: TextureHandle? = null
    var isLoading: Boolean = false
        private set
    var hasError: Boolean = false
        private set

    var source: ImageSource? = null
        set(value) {
            if (field != value) {
                field = value
                reloadImage()
            }
        }

    private fun reloadImage() {
        activeHandle?.release()
        activeHandle = null

        val currentSource = source ?: return
        isLoading = true
        hasError = false

        ImageLoader.load(currentSource) { handle, error ->
            isLoading = false
            if (error != null || handle == null) {
                hasError = true
            } else {
                activeHandle = handle
                invalidateLayout()
            }
        }
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val tex = activeHandle?.texture
        return if (tex != null) tex.width.toFloat() + padL + padR else minWidth + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val tex = activeHandle?.texture
        return if (tex != null) tex.height.toFloat() + padT + padB else minHeight + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        val handle = activeHandle ?: return
        val region = (source as? ImageSource.Region)?.region ?: handle.region
        val w = bounds.width - padL - padR
        val h = bounds.height - padT - padB
        if (w <= 0f || h <= 0f) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB

        val imgW = region.width.toFloat()
        val imgH = region.height.toFloat()

        Draw.color(tint)

        when (scaleMode) {
            ScaleMode.STRETCH -> {
                Draw.rect(region, innerX + w * 0.5f, innerY + h * 0.5f, w, h)
            }
            ScaleMode.FIT -> {
                val scale = minOf(w / imgW, h / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                Draw.rect(region, innerX + w * 0.5f, innerY + h * 0.5f, drawW, drawH)
            }
            ScaleMode.CROP -> {
                val scale = maxOf(w / imgW, h / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                Draw.rect(region, innerX + w * 0.5f, innerY + h * 0.5f, drawW, drawH)
            }
        }
        Draw.color(Color.white)
    }

    /**
     * Releases texture reference when node is removed from the DOM.
     */
    fun dispose() {
        activeHandle?.release()
        activeHandle = null
    }
}
