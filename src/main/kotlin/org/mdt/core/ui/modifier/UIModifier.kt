// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Immutable Modifier Chain Engine & Extension Factories.
// - Operating Mechanism: Zero-GC flattened array chains ([ModifierChain]); delegates to focused element classes.
// - Invariants: Immutable data classes; idempotent `applyTo(node)` mutations.
// - Dependencies: [LayoutModifiers], [DrawModifiers], [PointerModifiers], [UINode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

@file:Suppress("unused", "FunctionName")

package org.mdt.core.ui.modifier

import org.mdt.core.ui.node.UINode

/**
 * ## UIModifier
 *
 * Defines an immutable chain of layout constraints, visual styles, and input listeners.
 * Applies configured properties onto virtual [UINode] instances during composition.
 * Uses flat array sequences ([ModifierChain]) and data class elements for Zero-GC recomposition skipping.
 *
 * @see ModifierChain
 * @see UINode
 */
interface UIModifier {

    /** Applies this modifier's rules onto the target [node]. */
    fun applyTo(node: UINode)

    /** Chains this modifier with another [other] modifier in an ultra-fast, flattened structure. */
    fun then(other: UIModifier): UIModifier = when {
        other === None -> this
        this === None -> other
        else -> ModifierChain.concat(this, other)
    }

    /** Single atomic modifier element supporting value-based structural equality. */
    interface Element : UIModifier

    /** Empty modifier singleton representing a no-op chain start. */
    companion object None : UIModifier {
        override fun applyTo(node: UINode) {}
        override fun toString(): String = "Modifier.None"
    }
}

/** Top-level modifier factory returning an empty [UIModifier]. */
fun Modifier(): UIModifier = UIModifier

/** Ambient top-level accessor returning an empty [UIModifier]. */
val Modifier: UIModifier get() = UIModifier

/** Fluent modifier configuration builder. */
fun Modifier(block: UIModifier.() -> UIModifier): UIModifier = UIModifier.block()

/**
 * ## ModifierChain
 *
 * High-performance, flattened contiguous array-backed modifier sequence.
 * Eliminates recursive binary trees, reduces heap allocations, and provides O(N) sequential iteration.
 *
 * @property elements Flat contiguous array of individual modifier elements.
 *
 * @see UIModifier
 */
class ModifierChain internal constructor(
    val elements: Array<UIModifier.Element>
) : UIModifier {

    override fun applyTo(node: UINode) {
        val count = elements.size
        for (i in 0 until count) {
            elements[i].applyTo(node)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModifierChain) return false
        return elements.contentEquals(other.elements)
    }

    override fun hashCode(): Int = elements.contentHashCode()

    override fun toString(): String = "ModifierChain()"

    companion object {
        fun concat(first: UIModifier, second: UIModifier): UIModifier {
            val list = ArrayList<UIModifier.Element>(8)
            appendElements(first, list)
            appendElements(second, list)

            return when (list.size) {
                0 -> UIModifier.None
                1 -> list[0]
                else -> ModifierChain(list.toTypedArray())
            }
        }

        private fun appendElements(modifier: UIModifier, out: ArrayList<UIModifier.Element>) {
            when (modifier) {
                is UIModifier.Element -> out.add(modifier)
                is ModifierChain -> {
                    val elems = modifier.elements
                    for (i in elems.indices) {
                        out.add(elems[i])
                    }
                }
                else -> {}
            }
        }
    }
}

// --- CONDITIONAL BRANCHING EXTENSIONS ---

/** Conditionally chains [other] modifier if [condition] is 	rue. */
fun UIModifier.thenIf(condition: Boolean, other: UIModifier): UIModifier =
    if (condition) then(other) else this

/** Conditionally applies builder [block] if [condition] is 	rue. */
inline fun UIModifier.ifThen(condition: Boolean, block: UIModifier.() -> UIModifier): UIModifier =
    if (condition) block() else this
