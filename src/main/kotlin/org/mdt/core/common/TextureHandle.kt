package org.mdt.core.common

import arc.graphics.Pixmap
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * ## TextureKind
 *
 * Defines the lifecycle and ownership model of a cached OpenGL texture.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
enum class TextureKind {
    /** Dynamic texture allocated by the engine -> Eligible for GPU disposal upon cache eviction. */
    MANAGED,

    /** External shared texture (e.g. Mindustry Game Atlas) -> Never disposed by the cache. */
    SHARED,

    /** Error placeholder texture (e.g. iconic 'ohno') -> Never disposed, marks loading failure. */
    FALLBACK
}

/**
 * ## TextureHandle
 *
 * Thread-safe, lock-free reference-counted wrapper around an unmanaged OpenGL [Texture].
 * Uses atomic Compare-And-Swap (CAS) state transitions for coroutine compatibility without blocking locks.
 *
 * ### Invariants:
 * - Textures marked [TextureKind.SHARED] or [TextureKind.FALLBACK] are never destroyed on the GPU.
 * - Negative reference count underflows from redundant [release] calls are strictly prevented.
 * - Disposals are automatically dispatched to the OpenGL main render thread.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class TextureHandle(
    val key: String,
    val texture: Texture,
    val byteSize: Long = computeByteSize(texture),
    val kind: TextureKind = TextureKind.MANAGED,
    onZeroRefs: (TextureHandle) -> Unit = {}
) {
    /** TextureRegion wrapping the underlying OpenGL texture. */
    val region = TextureRegion(texture)

    /** Width in pixels of the underlying texture. */
    val width: Int get() = texture.width

    /** Height in pixels of the underlying texture. */
    val height: Int get() = texture.height

    /** Whether this texture is managed by the engine and eligible for GPU disposal upon eviction. */
    val isDisposable: Boolean get() = kind == TextureKind.MANAGED

    /** Whether this handle represents a failed load fallback texture. */
    val isFailed: Boolean get() = kind == TextureKind.FALLBACK

    /** Timestamp in nanoseconds of the most recent retention or release. */
    val lastAccessTimeNano = AtomicLong(System.nanoTime())

    private val refCount = AtomicInteger(1)
    private val _isDisposed = AtomicBoolean(false)
    private val onZeroRefsCallback = AtomicReference<(TextureHandle) -> Unit>(onZeroRefs)

    /** Whether the underlying OpenGL texture has been disposed from GPU memory. */
    val isDisposed: Boolean get() = _isDisposed.get()

    /** Current number of active references holding this texture. Returns 0 if dead or disposed. */
    val activeRefCount: Int get() = maxOf(0, refCount.get())

    // =========================================================================
    // I. Atomic Reference Counting & Lifecycle Operations
    // =========================================================================

    /**
     * Updates the callback invoked when the reference count drops to zero.
     */
    fun setOnZeroRefs(callback: (TextureHandle) -> Unit) {
        onZeroRefsCallback.set(callback)
    }

    /**
     * Atomically increments the reference count using a lock-free CAS loop.
     *
     * @return `true` if retained successfully, or `false` if the handle was already disposed.
     */
    fun retain(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current < 0) return false
            if (refCount.compareAndSet(current, current + 1)) {
                lastAccessTimeNano.set(System.nanoTime())
                return true
            }
        }
    }

    /**
     * Atomically decrements the reference count using a lock-free CAS loop.
     * Triggers [onZeroRefsCallback] exactly once upon transitioning from 1 to 0.
     *
     * @return `true` if released successfully, or `false` if the handle was already disposed or at 0.
     */
    fun release(): Boolean {
        while (true) {
            if (_isDisposed.get()) return false
            val current = refCount.get()
            if (current <= 0) return false

            if (refCount.compareAndSet(current, current - 1)) {
                lastAccessTimeNano.set(System.nanoTime())
                if (current - 1 == 0) {
                    onZeroRefsCallback.get()?.invoke(this)
                }
                return true
            }
        }
    }

    /**
     * Atomically marks this handle as disposed and releases the underlying GPU texture if [isDisposable].
     * Dispatches texture disposal safely to the main render thread.
     *
     * @return `true` if this call transitioned the handle to disposed state, or `false` if already disposed.
     */
    fun dispose(): Boolean {
        if (!_isDisposed.compareAndSet(false, true)) return false

        refCount.set(-1)
        if (isDisposable) {
            AsyncDispatcher.onMainThread {
                texture.dispose()
            }
        }
        return true
    }

    // =========================================================================
    // II. Factory & Utility Companion Methods
    // =========================================================================

    companion object {
        /**
         * Calculates the standard 32-bit RGBA VRAM byte size of a texture.
         */
        fun computeByteSize(texture: Texture): Long =
            texture.width.toLong() * texture.height.toLong() * 4L

        /**
         * Factory creating a [TextureHandle] from a raw [Texture].
         */
        fun of(
            key: String,
            texture: Texture,
            kind: TextureKind = TextureKind.MANAGED,
            onZeroRefs: (TextureHandle) -> Unit = {}
        ): TextureHandle = TextureHandle(
            key = key,
            texture = texture,
            byteSize = computeByteSize(texture),
            kind = kind,
            onZeroRefs = onZeroRefs
        )

        /**
         * Factory creating an unmanaged, shared [TextureHandle] (e.g. from an atlas).
         */
        fun shared(key: String, texture: Texture): TextureHandle =
            of(key, texture, TextureKind.SHARED)

        /**
         * Factory creating an unmanaged, shared [TextureHandle] from a [TextureRegion].
         */
        fun shared(key: String, region: TextureRegion): TextureHandle =
            of(key, region.texture, TextureKind.SHARED)

        /**
         * Factory creating a fallback/placeholder [TextureHandle].
         */
        fun fallback(key: String = "fallback:placeholder", texture: Texture): TextureHandle =
            of(key, texture, TextureKind.FALLBACK)

        /**
         * Factory creating a fallback/placeholder [TextureHandle] from a [TextureRegion].
         */
        fun fallback(key: String = "fallback:placeholder", region: TextureRegion): TextureHandle =
            of(key, region.texture, TextureKind.FALLBACK)

        /**
         * Factory uploading a decoded [Pixmap] to a new managed [Texture] and disposing the pixmap.
         *
         * @param key Unique cache key identifier.
         * @param pixmap Decoded in-memory pixmap.
         * @param filter Texture filtering mode (default linear).
         * @param kind Lifecycle classification (default MANAGED).
         * @return Retained [TextureHandle].
         */
        fun fromPixmap(
            key: String,
            pixmap: Pixmap,
            filter: Texture.TextureFilter = Texture.TextureFilter.linear,
            kind: TextureKind = TextureKind.MANAGED,
            onZeroRefs: (TextureHandle) -> Unit = {}
        ): TextureHandle {
            val texture = Texture(pixmap).apply {
                setFilter(filter)
            }
            pixmap.dispose()
            return of(key, texture, kind, onZeroRefs)
        }
    }
}

/**
 * Executes the given [block] holding a reference to this [TextureHandle], releasing it automatically upon completion.
 */
inline fun <R> TextureHandle.use(block: (TextureHandle) -> R): R {
    return try {
        block(this)
    } finally {
        release()
    }
}
