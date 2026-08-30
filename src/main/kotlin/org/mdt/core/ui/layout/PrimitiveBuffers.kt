// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.layout

/**
 * ## IntList
 *
 * Lightweight Zero-GC reusable primitive Int buffer for layout measurements and coordinate indexing.
 *
 * @property size Current count of elements stored in the buffer.
 * @property items Underlying primitive array holding elements.
 * @property isEmpty `true` if the buffer contains zero elements.
 */
internal class IntList(initialCapacity: Int = 16) {
    var size: Int = 0
        private set
    var items: IntArray = IntArray(initialCapacity)
        private set

    val isEmpty: Boolean get() = size == 0

    fun add(value: Int) {
        if (size >= items.size) {
            items = items.copyOf(items.size * 2)
        }
        items[size++] = value
    }

    fun get(index: Int): Int = items[index]

    fun clear() {
        size = 0
    }
}

/**
 * ## FloatList
 *
 * Lightweight Zero-GC reusable primitive Float buffer for layout measurements and flex space distribution.
 *
 * @property size Current count of elements stored in the buffer.
 * @property items Underlying primitive array holding elements.
 * @property isEmpty `true` if the buffer contains zero elements.
 */
internal class FloatList(initialCapacity: Int = 16) {
    var size: Int = 0
        private set
    var items: FloatArray = FloatArray(initialCapacity)
        private set

    val isEmpty: Boolean get() = size == 0

    fun add(value: Float) {
        if (size >= items.size) {
            items = items.copyOf(items.size * 2)
        }
        items[size++] = value
    }

    fun get(index: Int): Float = items[index]

    fun clear() {
        size = 0
    }
}
