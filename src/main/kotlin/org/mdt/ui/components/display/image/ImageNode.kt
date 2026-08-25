package org.mdt.ui.components.display.image

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.TextureRegion
import org.mdt.core.cache.ResourceHandle
import org.mdt.core.image.ImageLoader
import org.mdt.core.image.ImageSource
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.render.EngineRenderer

enum class ScaleMode {
    FIT, CROP, STRETCH, CENTER
}

/**
 * ## ImageNode
 *
 * Virtual DOM node rendering a static texture or dynamic image with automatic
 * [ResourceHandle] lifecycle reference counting.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class ImageNode : LayoutNode() {

    private var handle: ResourceHandle<TextureRegion>? = null

    var source: ImageSource? = null
        set(value) {
            if (field != value) {
                field = value
                releaseHandle()
                if (value != null) {
                    ImageLoader.load(value) { newHandle ->
                        handle = newHandle
                        invalidateLayout()
                    }
                } else {
                    invalidateLayout()
                }
            }
        }

    var scaleMode: ScaleMode = ScaleMode.FIT
    var tintColor: Color = Color(Color.white)

    fun setRegion(region: TextureRegion) {
        releaseHandle()
        handle = ResourceHandle(region)
        source = null
        invalidateLayout()
    }

    private fun releaseHandle() {
        handle?.release()
        handle = null
    }

    override fun dispose() {
        releaseHandle()
        super.dispose()
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val r = handle?.get()
        val regW = if (r != null) r.width.toFloat() else 0f
        return (if (minWidth >= 0f) maxOf(regW, minWidth) else regW) + padL + padR
    }

    override fun getPrefHeight(): Float {
        if (height >= 0f) return height
        val r = handle?.get()
        val regH = if (r != null) r.height.toFloat() else 0f
        return (if (minHeight >= 0f) maxOf(regH, minHeight) else regH) + padT + padB
    }

    override fun drawSelf(renderer: EngineRenderer) {
        super.drawSelf(renderer)

        val reg = handle?.get() ?: return
        val w = bounds.width - padL - padR
        val h = bounds.height - padT - padB
        if (w <= 0f || h <= 0f) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB

        val rw = reg.width.toFloat()
        val rh = reg.height.toFloat()
        if (rw <= 0f || rh <= 0f) return

        var dw = w
        var dh = h
        var dx = innerX
        var dy = innerY

        when (scaleMode) {
            ScaleMode.STRETCH -> {
                // dw = w, dh = h
            }
            ScaleMode.FIT -> {
                val scale = minOf(w / rw, h / rh)
                dw = rw * scale
                dh = rh * scale
                dx = innerX + (w - dw) * 0.5f
                dy = innerY + (h - dh) * 0.5f
            }
            ScaleMode.CROP -> {
                val scale = maxOf(w / rw, h / rh)
                dw = rw * scale
                dh = rh * scale
                dx = innerX + (w - dw) * 0.5f
                dy = innerY + (h - dh) * 0.5f
            }
            ScaleMode.CENTER -> {
                dw = rw
                dh = rh
                dx = innerX + (w - dw) * 0.5f
                dy = innerY + (h - dh) * 0.5f
            }
        }

        Draw.color(tintColor)
        Draw.rect(reg, dx + dw * 0.5f, dy + dh * 0.5f, dw, dh)
        Draw.color(Color.white)
    }
}
