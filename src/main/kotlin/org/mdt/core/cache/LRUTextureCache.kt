package org.mdt.core.cache

import arc.graphics.Texture
import org.mdt.core.async.AsyncDispatcher
import java.util.Collections

/**
 * ## LRUTextureCache
 *
 * Thread-safe, VRAM-bounded Least-Recently-Used (LRU) cache for OpenGL textures.
 * Safely evicts and disposes textures when the VRAM limit is exceeded.
 */
class LRUTextureCache(
    val maxVramBytes: Long = 64L * 1024L * 1024L // 64 MB default
) {
    private var currentVramBytes: Long = 0L
    private val lruMap = Collections.synchronizedMap(
        LinkedHashMap<String, TextureHandle>(16, 0.75f, true)
    )

    /**
     * Retrieves a texture handle from cache, retaining it if found.
     */
    fun get(key: String): TextureHandle? {
        synchronized(lruMap) {
            val handle = lruMap[key] ?: return null
            if (handle.isDisposed) {
                lruMap.remove(key)
                return null
            }
            return handle.retain()
        }
    }

    /**
     * Stores a new texture in the LRU cache with automatic VRAM bounding.
     */
    fun put(key: String, texture: Texture): TextureHandle {
        val byteSize = texture.width.toLong() * texture.height.toLong() * 4L

        synchronized(lruMap) {
            // If already exists, return retained existing handle
            val existing = lruMap[key]
            if (existing != null && !existing.isDisposed) {
                return existing.retain()
            }

            val handle = TextureHandle(key, texture, byteSize) { _ ->
                // Called when ref count reaches 0
                // If it's still in LRU cache, it stays available for future get() until evicted!
            }

            lruMap[key] = handle
            currentVramBytes += byteSize
            trimToSize()
            return handle
        }
    }

    /**
     * Trims the cache down to [maxVramBytes] by evicting oldest unused textures.
     */
    private fun trimToSize() {
        while (currentVramBytes > maxVramBytes && lruMap.isNotEmpty()) {
            val iterator = lruMap.entries.iterator()
            if (!iterator.hasNext()) break
            val entry = iterator.next()
            val handle = entry.value
            iterator.remove()
            currentVramBytes -= handle.byteSize

            AsyncDispatcher.onMainThread { handle.disposeDirectly() }
        }
    }

    /**
     * Clears all cached textures and releases GPU memory.
     */
    fun clear() {
        synchronized(lruMap) {
            for (handle in lruMap.values) {
                AsyncDispatcher.onMainThread { handle.disposeDirectly() }
            }
            lruMap.clear()
            currentVramBytes = 0L
        }
    }

    companion object {
        /** Default shared global texture cache. */
        val shared by lazy { LRUTextureCache() }
    }
}
