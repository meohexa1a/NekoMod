package org.hubdustry.libs.compose

import org.hubdustry.libs.layout.LayoutNode

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
     * Tích lũy giá trị bằng cách duyệt qua tất cả các [Element] từ trong ra ngoài (phải sang trái).
     */
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /**
     * Kiểm tra xem có bất kỳ [Element] nào thỏa mãn [predicate] hay không.
     */
    fun any(predicate: (Element) -> Boolean): Boolean

    /**
     * Kiểm tra xem tất cả các [Element] có thỏa mãn [predicate] hay không.
     */
    fun all(predicate: (Element) -> Boolean): Boolean

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

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)

        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)
        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)

        /**
         * Áp dụng cấu hình của phần tử này lên [LayoutNode] ảo.
         */
        fun applyTo(node: LayoutNode)
    }

    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override fun any(predicate: (Element) -> Boolean): Boolean = false
        override fun all(predicate: (Element) -> Boolean): Boolean = true
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

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

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
