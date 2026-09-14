package org.hubdustry.core.compose.modifier

import org.hubdustry.core.layout.LayoutNode

/**
 * Giao diện cấu hình [LayoutNode] theo mô hình Modifier bất biến của Jetpack Compose (AOSP).
 * Cho phép ghép nối, gấp (fold) và duyệt qua các phần tử modifier một cách tất định.
 */
interface Modifier {

    /**
     * Tích lũy giá trị bằng cách duyệt qua tất cả các [Element] từ ngoài vào trong (trái sang phải).
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Ghép nối modifier hiện tại với [other].
     */
    infix fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    /**
     * Một phần tử đơn vị trong chuỗi Modifier.
     */
    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        /**
         * Áp dụng cấu hình của phần tử này lên [LayoutNode] ảo.
         */
        fun applyTo(node: LayoutNode)
    }

    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override infix fun then(other: Modifier): Modifier = other
        override fun toString(): String = "Modifier"
    }
}

/**
 * Ghép nối 2 Modifier [outer] và [inner] theo thứ tự từ ngoài vào trong.
 */
class CombinedModifier(
    val outer: Modifier,
    val inner: Modifier
) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun equals(other: Any?): Boolean =
        other is CombinedModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString(): String = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}

/**
 * Duyệt qua tất cả các phần tử trong [Modifier] theo thứ tự từ ngoài vào trong
 * và áp dụng cấu hình lên [LayoutNode] mà không cấp phát lambda closure (Zero-GC).
 */
fun Modifier.applyTo(node: LayoutNode) {
    when (this) {
        is Modifier.Element -> applyTo(node)
        is CombinedModifier -> {
            outer.applyTo(node)
            inner.applyTo(node)
        }
        else -> {
            if (this !== Modifier) {
                foldIn(Unit) { _, element -> element.applyTo(node) }
            }
        }
    }
}
