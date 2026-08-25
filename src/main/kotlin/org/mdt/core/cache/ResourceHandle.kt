package org.mdt.core.cache

import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import java.util.concurrent.atomic.AtomicInteger

/**
 * ## TextureHandle
 *
 * Safe reference-counted wrapper around an unmanaged OpenGL [Texture].
 * Ensures GPU memory is never leaked and textures are automatically released
 * when unmounted from the UI tree.
 */
class TextureHandle(
    val key: String,
    val texture: Texture,
    val byteSize: Long,
    private val onZeroRefs: (TextureHandle) -> Unit
) {
    val region = TextureRegion(texture)

    private val refCount = AtomicInteger(1)
    var isDisposed: Boolean = false
        private set

    /**
     * Increments the reference count by 1.
     */
    fun retain(): TextureHandle {
        if (!isDisposed) refCount.incrementAndGet()
        return this
    }

    /**
     * Decrements the reference count by 1. If no references remain, triggers cleanup.
     */
    fun release() {
        if (isDisposed) return
        val remaining = refCount.decrementAndGet()
        if (remaining <= 0) onZeroRefs(this)
    }

    /**
     * Disposes the underlying OpenGL texture on the GPU.
     */
    internal fun disposeDirectly() {
        if (!isDisposed) {
            isDisposed = true
            texture.dispose()
        }
    }
}
