package org.mdt.core.cache

import arc.graphics.Texture
import org.mdt.core.common.AsyncDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * ## LRUTextureCache
 *
 * High-performance, lock-free, coroutine-friendly Least-Recently-Used (LRU) texture cache.
 * Designed for asynchronous game engines with zero blocking monitor locks (`synchronized`).
 *
 * ### Features:
 * 1. **Lock-Free Atomic Retrieval:** Atomic CAS increments pin textures in memory without thread blocking.
 * 2. **Zero VRAM Leak on Duplicate Put:** Unused dynamic textures submitted to [put] are safely disposed immediately.
 * 3. **Non-Blocking Eviction:** High-resolution nanosecond timestamps order evictable idle handles (`refCount == 0`).
 * 4. **Protected Atlas Textures:** Game atlas and external textures marked `isDisposable = false` are never destroyed.
 * 5. **Diagnostic Failure Tracking:** Handles can report [TextureHandle.isFailed] status.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class LRUTextureCache(
    val maxIdleVramBytes: Long = 64L * 1024L * 1024L // 64 MB default idle budget
) {
    /** Lock-free concurrent texture registry. */
    private val cache = ConcurrentHashMap<String, TextureHandle>()

    /** Total VRAM currently consumed by idle (unreferenced, refCount == 0) textures. */
    private val _idleVramBytes = AtomicLong(0L)

    val idleVramBytes: Long get() = _idleVramBytes.get()

    /** Total active textures registered in cache. */
    val size: Int get() = cache.size

    /** Single-runner guard for idle eviction to prevent concurrent trim contention. */
    private val isTrimming = AtomicBoolean(false)

    /**
     * Retrieves a texture handle from cache, atomically retaining it if found.
     * Guaranteed lock-free and thread-safe.
     */
    fun get(key: String): TextureHandle? {
        val handle = cache[key] ?: return null
        if (handle.isDisposed) {
            cache.remove(key, handle)
            return null
        }

        if (handle.retain()) return handle

        cache.remove(key, handle)
        return null
    }

    /**
     * Stores a new texture in the cache and returns a retained [TextureHandle].
     * If the key already exists, safely disposes the redundant [texture] if [TextureKind.MANAGED] to prevent VRAM leaks.
     */
    fun put(
        key: String,
        texture: Texture,
        kind: TextureKind = TextureKind.MANAGED
    ): TextureHandle {
        val isDisposable = kind == TextureKind.MANAGED

        val existing = get(key)
        if (existing != null) {
            if (isDisposable && texture != existing.texture) AsyncDispatcher.onMainThread { texture.dispose() }
            return existing
        }

        val byteSize = texture.width.toLong() * texture.height.toLong() * 4L
        val handle = TextureHandle(key, texture, byteSize, kind) { deadHandle -> onHandleZeroRefs(deadHandle) }

        val previous = cache.putIfAbsent(key, handle)
        if (previous != null && !previous.isDisposed && previous.retain()) {
            if (isDisposable && texture != previous.texture) AsyncDispatcher.onMainThread { texture.dispose() }
            return previous
        }

        return handle
    }

    /**
     * Invoked atomically when all active UI references to a [TextureHandle] reach 0.
     * Updates idle VRAM tracking and triggers lock-free eviction.
     */
    private fun onHandleZeroRefs(handle: TextureHandle) {
        if (handle.isDisposed) return
        _idleVramBytes.addAndGet(handle.byteSize)
        trimIdlePool()
    }

    /**
     * Trims idle textures down to [maxIdleVramBytes] in a lock-free, single-runner pass.
     */
    private fun trimIdlePool() {
        if (!isTrimming.compareAndSet(false, true)) return

        try {
            if (_idleVramBytes.get() <= maxIdleVramBytes) return

            // Collect all unreferenced handles
            val idleHandles = cache.values.filter { it.activeRefCount == 0 && !it.isDisposed }
                .sortedBy { it.lastAccessTimeNano.get() }

            for (handle in idleHandles) {
                if (_idleVramBytes.get() <= maxIdleVramBytes) break
                if (handle.activeRefCount == 0 && cache.remove(handle.key, handle)) {
                    _idleVramBytes.addAndGet(-handle.byteSize)
                    AsyncDispatcher.onMainThread { handle.disposeDirectly() }
                }
            }
        } finally {
            isTrimming.set(false)
        }
    }

    /**
     * Clears all idle (unreferenced) textures from cache and frees GPU memory.
     */
    fun clear() {
        for ((key, handle) in cache) {
            if (handle.activeRefCount == 0 && cache.remove(key, handle)) {
                _idleVramBytes.addAndGet(-handle.byteSize)
                AsyncDispatcher.onMainThread { handle.disposeDirectly() }
            }
        }
    }

    /**
     * Force-clears the entire cache (both active and idle).
     */
    fun clearAll() {
        for ((key, handle) in cache) {
            cache.remove(key, handle)
            AsyncDispatcher.onMainThread { handle.disposeDirectly() }
        }
        _idleVramBytes.set(0L)
    }

    companion object {
        /** Default shared global texture cache. */
        val shared by lazy { LRUTextureCache() }
    }
}
