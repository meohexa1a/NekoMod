package org.mdt.core.common

import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
class TextureHandle(
    val key: String,
    val texture: Texture,
    val byteSize: Long,
    val kind: TextureKind = TextureKind.MANAGED,
    private val onZeroRefs: (TextureHandle) -> Unit = {}
) {
    /** TextureRegion wrapping the underlying OpenGL texture. */
    val region = TextureRegion(texture)

    /** Whether this texture is managed by the engine and eligible for GPU disposal upon eviction. */
    val isDisposable: Boolean get() = kind == TextureKind.MANAGED

    /** Whether this handle represents a failed load fallback texture. */
    val isFailed: Boolean get() = kind == TextureKind.FALLBACK

    /** Timestamp in nanoseconds of the most recent retention or release. */
    val lastAccessTimeNano = AtomicLong(System.nanoTime())

    private val refCount = AtomicInteger(1)
    private val _isDisposed = AtomicBoolean(false)

    /** Whether the underlying OpenGL texture has been disposed from GPU memory. */
    val isDisposed: Boolean get() = _isDisposed.get()

    /** Current number of active references holding this texture. Returns 0 if dead or disposed. */
    val activeRefCount: Int get() = maxOf(0, refCount.get())

    // =========================================================================
    // I. Atomic Reference Counting & Lifecycle Operations
    // =========================================================================

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
     * Triggers [onZeroRefs] exactly once upon transitioning from 1 to 0.
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
                if (current - 1 == 0) onZeroRefs(this)
                return true
            }
        }
    }

    /**
     * Atomically marks this handle as disposed and releases the underlying GPU texture if [isDisposable].
     *
     * @return `true` if this call transitioned the handle to disposed state, or `false` if already disposed.
     */
    internal fun dispose(): Boolean {
        if (!_isDisposed.compareAndSet(false, true)) return false

        refCount.set(-1)
        if (isDisposable) texture.dispose()
        return true
    }
}
