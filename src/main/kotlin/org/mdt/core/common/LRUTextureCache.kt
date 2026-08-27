package org.mdt.core.common

import arc.graphics.Texture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.iterator

/**
 * ## LRUTextureCache
 *
 * High-performance, lock-free, coroutine-friendly Least-Recently-Used (LRU) texture cache.
 * Designed for asynchronous game engines with zero blocking monitor locks (`synchronized`).
 *
 * ### Architectural Features:
 * 1. **TextureHandle-Centric:** All operations are managed strictly through [TextureHandle] lifecycle primitives.
 * 2. **Lock-Free Atomic Retrieval:** Atomic CAS increments pin textures in memory without thread blocking.
 * 3. **Zero VRAM Leak on Duplicate Put:** Redundant handles submitted to [put] are safely disposed immediately.
 * 4. **Non-Blocking Eviction:** High-resolution nanosecond timestamps order evictable idle handles (`refCount == 0`).
 * 5. **Protected Atlas Textures:** Textures marked [TextureKind.SHARED] or [TextureKind.FALLBACK] are never destroyed.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class LRUTextureCache(
    val maxIdleVramBytes: Long = 64L * 1024L * 1024L // 64 MB default idle budget
) {
    /** Lock-free concurrent texture registry mapping unique keys to their handles. */
    private val cache = ConcurrentHashMap<String, TextureHandle>()

    /** Total VRAM currently consumed by idle (unreferenced, refCount == 0) textures. */
    private val _idleVramBytes = AtomicLong(0L)

    /** Total VRAM in bytes consumed by idle textures. */
    val idleVramBytes: Long get() = _idleVramBytes.get()

    /** Total VRAM in bytes consumed by all cached textures. */
    val totalVramBytes: Long get() = cache.values.sumOf { if (!it.isDisposed) it.byteSize else 0L }

    /** Total VRAM in bytes consumed by dynamic managed textures (excluding shared game atlas). */
    val dynamicVramBytes: Long get() = cache.values.sumOf { if (!it.isDisposed && it.kind == TextureKind.MANAGED) it.byteSize else 0L }

    /** Total number of textures currently tracked in the cache. */
    val size: Int get() = cache.size

    /** Single-runner guard for idle eviction to prevent concurrent trim contention. */
    private val isTrimming = AtomicBoolean(false)

    // =========================================================================
    // I. Texture Retrieval & Insertion (Read/Write API)
    // =========================================================================

    /**
     * Retrieves a texture handle from cache, atomically retaining it if found.
     * Guaranteed lock-free and thread-safe.
     *
     * @param key Unique texture key identifier.
     * @return Retained [TextureHandle], or `null` if absent or disposed.
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
     * Checks whether a texture handle exists in the cache under the given [key].
     *
     * @param key Unique texture key identifier.
     * @return `true` if the key is present in cache.
     */
    fun contains(key: String): Boolean = cache.containsKey(key)

    /**
     * Stores a [TextureHandle] in the cache and returns a retained reference.
     * If an entry with the same key already exists, disposes [handle] if it differs to prevent VRAM leaks.
     *
     * @param handle [TextureHandle] instance.
     * @return Retained [TextureHandle].
     */
    fun put(handle: TextureHandle): TextureHandle {
        val key = handle.key
        if (handle.isDisposed) return handle

        val existing = get(key)
        if (existing != null) {
            if (handle != existing) handle.dispose()
            return existing
        }

        handle.setOnZeroRefs { deadHandle -> onHandleZeroRefs(deadHandle) }

        val previous = cache.putIfAbsent(key, handle)
        if (previous != null && !previous.isDisposed && previous.retain()) {
            if (handle != previous) handle.dispose()
            return previous
        }

        return handle
    }

    /**
     * Convenience overload storing a new raw [Texture] wrapped into a [TextureHandle].
     */
    fun put(
        key: String,
        texture: Texture,
        kind: TextureKind = TextureKind.MANAGED
    ): TextureHandle = put(TextureHandle.of(key, texture, kind))

    /**
     * Retrieves a cached [TextureHandle], or computes and stores a new one via [factory] atomically.
     *
     * @param key Unique texture key identifier.
     * @param factory Producer lambda creating the [TextureHandle] on cache miss.
     * @return Retained [TextureHandle].
     */
    inline fun getOrPut(key: String, factory: () -> TextureHandle): TextureHandle {
        val cached = get(key)
        if (cached != null) return cached
        val created = factory()
        return put(created)
    }

    // =========================================================================
    // II. Public Invalidation & Cache Eviction
    // =========================================================================

    /**
     * Clears all idle (unreferenced, refCount == 0) textures from cache and frees GPU memory.
     */
    fun clearIdle() {
        for ((key, handle) in cache) {
            if (handle.activeRefCount == 0 && cache.remove(key, handle)) {
                _idleVramBytes.addAndGet(-handle.byteSize)
                handle.dispose()
            }
        }
    }

    /**
     * Clears the entire cache (both active and idle textures), disposing all managed resources.
     */
    fun clear() {
        for ((key, handle) in cache) {
            cache.remove(key, handle)
            handle.dispose()
        }
        _idleVramBytes.set(0L)
    }

    // =========================================================================
    // III. Internal Eviction & Memory Management (Private)
    // =========================================================================

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

            // Collect all unreferenced handles sorted by least-recently accessed
            val idleHandles = cache.values.filter { it.activeRefCount == 0 && !it.isDisposed }
                .sortedBy { it.lastAccessTimeNano.get() }

            for (handle in idleHandles) {
                if (_idleVramBytes.get() <= maxIdleVramBytes) break
                if (handle.activeRefCount == 0 && cache.remove(handle.key, handle)) {
                    _idleVramBytes.addAndGet(-handle.byteSize)
                    handle.dispose()
                }
            }
        } finally {
            isTrimming.set(false)
        }
    }

    companion object {
        /** Default shared global texture cache. */
        val shared by lazy { LRUTextureCache() }
    }
}
