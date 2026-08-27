package org.mdt.core.common

import arc.graphics.Texture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.iterator

/**
 * ## LRUTextureCache
 *
 * Lock-free, coroutine-friendly Least-Recently-Used (LRU) texture cache.
 * Tracks cached [TextureHandle] instances and evicts unreferenced textures when exceeding [maxIdleVramBytes].
 *
 * ### Invariants:
 * 1. **TextureHandle Decoupling:** [TextureHandle] remains a pure leaf resource without cache baggage.
 * 2. **Zero VRAM Leak on Duplicate Put:** Redundant handles submitted to [put] are safely disposed immediately.
 * 3. **Non-Blocking Eviction:** High-resolution nanosecond timestamps order evictable idle handles (`refCount == 0`).
 * 4. **Protected Atlas Textures:** Textures marked [TextureOwnership.SHARED] are never destroyed.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class LRUTextureCache(
    val maxIdleVramBytes: Long = 64L * 1024L * 1024L // 64 MB default idle budget
) {
    /** Internal cache entry tracking LRU access timestamps independently of TextureHandle. */
    class CacheEntry(
        val handle: TextureHandle,
        val lastAccessNano: AtomicLong = AtomicLong(System.nanoTime())
    ) {
        fun touch() {
            lastAccessNano.set(System.nanoTime())
        }
    }

    /** Lock-free concurrent texture registry mapping unique keys to their entries. */
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /** Total VRAM in bytes consumed by idle (unreferenced, refCount == 0) textures. */
    val idleVramBytes: Long
        get() = cache.values.sumOf { if (!it.handle.isDisposed && it.handle.isDisposable && it.handle.activeRefCount == 0) it.handle.byteSize else 0L }

    /** Total VRAM in bytes consumed by dynamic managed textures (excluding shared game atlas). */
    val dynamicVramBytes: Long
        get() = cache.values.sumOf { if (!it.handle.isDisposed && it.handle.isDisposable) it.handle.byteSize else 0L }

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
        val entry = cache[key] ?: return null
        val handle = entry.handle
        if (handle.isDisposed) {
            cache.remove(key, entry)
            return null
        }

        if (handle.retain()) {
            entry.touch()
            return handle
        }

        cache.remove(key, entry)
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
     * @param key Optional cache key identifier (defaults to handle.key).
     * @return Retained [TextureHandle].
     */
    fun put(handle: TextureHandle, key: String = handle.key): TextureHandle {
        val cacheKey = key.ifEmpty { handle.key }
        require(cacheKey.isNotEmpty()) { "Cache key must not be empty" }
        if (handle.isDisposed) return handle

        val existing = get(cacheKey)
        if (existing != null) {
            if (handle != existing) handle.dispose()
            return existing
        }

        val newEntry = CacheEntry(handle)
        val previous = cache.putIfAbsent(cacheKey, newEntry)
        if (previous != null) {
            val prevHandle = previous.handle
            if (!prevHandle.isDisposed && prevHandle.retain()) {
                previous.touch()
                if (handle != prevHandle) handle.dispose()
                return prevHandle
            }
        }

        trimIdlePool()
        return handle
    }

    /**
     * Convenience overload storing a new raw [Texture] wrapped into a [TextureHandle].
     */
    fun put(
        key: String,
        texture: Texture,
        ownership: TextureOwnership = TextureOwnership.MANAGED
    ): TextureHandle = put(TextureHandle(texture, ownership, key), key)

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
        return put(created, key)
    }

    // =========================================================================
    // II. Public Invalidation & Cache Eviction
    // =========================================================================

    /**
     * Clears all idle (unreferenced, refCount == 0) textures from cache and frees GPU memory.
     */
    fun clearIdle() {
        for ((key, entry) in cache) {
            val handle = entry.handle
            if (handle.activeRefCount == 0 && cache.remove(key, entry)) {
                handle.dispose()
            }
        }
    }

    /**
     * Clears the entire cache (both active and idle textures), disposing all managed resources.
     */
    fun clear() {
        for ((key, entry) in cache) {
            cache.remove(key, entry)
            entry.handle.dispose()
        }
    }

    // =========================================================================
    // III. Internal Eviction & Memory Management (Private)
    // =========================================================================

    /**
     * Trims idle textures down to [maxIdleVramBytes] in a lock-free, single-runner pass.
     */
    fun trimIdlePool() {
        if (!isTrimming.compareAndSet(false, true)) return

        try {
            if (idleVramBytes <= maxIdleVramBytes) return

            // Collect all unreferenced handles sorted by least-recently accessed
            val idleEntries = cache.entries
                .filter { it.value.handle.activeRefCount == 0 && !it.value.handle.isDisposed }
                .sortedBy { it.value.lastAccessNano.get() }

            for ((key, entry) in idleEntries) {
                if (idleVramBytes <= maxIdleVramBytes) break
                if (entry.handle.activeRefCount == 0 && cache.remove(key, entry)) {
                    entry.handle.dispose()
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
