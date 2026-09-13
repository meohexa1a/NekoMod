package org.hubdustry.core.layout

import androidx.compose.runtime.AbstractApplier
import kotlin.test.Test
import kotlin.test.assertTrue

class TripleReversalAdversarialTest {

    // Reference Applier using standard List operations
    class ReferenceApplier(val list: ArrayList<Int>) : AbstractApplier<Int>(0) {
        override fun onClear() { list.clear() }
        override fun insertBottomUp(index: Int, instance: Int) { list.add(index, instance) }
        override fun insertTopDown(index: Int, instance: Int) { /* no-op */ }
        override fun remove(index: Int, count: Int) {
            for (i in 0 until count) list.removeAt(index)
        }
        override fun move(from: Int, to: Int, count: Int) {
            val dest = if (from > to) to else to - count
            if (count == 1) {
                if (from == to + 1 || from == to - 1) {
                    val tmp = list[from]
                    list[from] = list[to]
                    list[to] = tmp
                    return
                }
            }
            val sub = ArrayList<Int>(count)
            for (i in 0 until count) {
                sub.add(list[from + i])
            }
            for (i in 0 until count) {
                list.removeAt(from)
            }
            for (i in 0 until count) {
                list.add(dest + i, sub[i])
            }
        }
    }

    private fun reverseRange(list: ArrayList<Int>, start: Int, end: Int) {
        var i = start
        var j = end
        while (i < j) {
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
            i++
            j--
        }
    }

    // Exact implementation from ARCHITECTURE_V3_DOSSIER.md Section 4.2
    private fun moveChildrenDossier(children: ArrayList<Int>, from: Int, to: Int, count: Int) {
        if (count <= 0 || from < 0 || to < 0) return
        val size = children.size
        if (from + count > size || to > size) return

        val dest = if (from > to) to else to - count
        if (from == dest) return

        if (count == 1) {
            // The dossier says: internalSwapOrShift(from, dest)
            // What if it does shift?
            shiftOne(children, from, dest)
        } else {
            if (from < dest) {
                reverseRange(children, from, from + count - 1)
                reverseRange(children, from + count, dest + count - 1)
                reverseRange(children, from, dest + count - 1)
            } else {
                reverseRange(children, dest, from - 1)
                reverseRange(children, from, from + count - 1)
                reverseRange(children, dest, from + count - 1)
            }
        }
    }

    // Pure Triple Reversal without count == 1 special branch
    private fun moveChildrenPureTripleReversal(children: ArrayList<Int>, from: Int, to: Int, count: Int) {
        if (count <= 0 || from < 0 || to < 0) return
        val size = children.size
        if (from + count > size || to > size) return

        val dest = if (from > to) to else to - count
        if (from == dest) return

        if (from < dest) {
            reverseRange(children, from, from + count - 1)
            reverseRange(children, from + count, dest + count - 1)
            reverseRange(children, from, dest + count - 1)
        } else {
            reverseRange(children, dest, from - 1)
            reverseRange(children, from, from + count - 1)
            reverseRange(children, dest, from + count - 1)
        }
    }

    private fun shiftOne(list: ArrayList<Int>, from: Int, dest: Int) {
        val item = list[from]
        if (from < dest) {
            for (i in from until dest) {
                list[i] = list[i + 1]
            }
        } else {
            for (i in from downTo dest + 1) {
                list[i] = list[i - 1]
            }
        }
        list[dest] = item
    }

    @Test
    fun testFromEqualsToBug() {
        val list = arrayListOf(10, 20, 30, 40, 50)
        println("Original: $list")
        // Calling move where from == to == 0, count = 1
        var caughtException = false
        try {
            moveChildrenDossier(list, 0, 0, 1)
        } catch (e: IndexOutOfBoundsException) {
            caughtException = true
            println("Caught expected crash on (0, 0, 1): ${e.message}")
        }
        assertTrue(caughtException, "Dossier must crash with IndexOutOfBoundsException when from=0, to=0, count=1 due to dest = -1")
    }

    @Test
    fun testFromEqualsToCorruption() {
        val list = arrayListOf(0, 1, 2, 3, 4, 5)
        // Calling move where from == 3, to == 3, count = 2
        // Should be a NO-OP since items at 3, 4 are moving to 3.
        val originalCopy = ArrayList(list)
        moveChildrenDossier(list, 3, 3, 2)
        println("Original: $originalCopy")
        println("After moveChildrenDossier(3, 3, 2): $list")
        // Check if list was corrupted
        val corrupted = list != originalCopy
        println("Is list corrupted? $corrupted")
        assertTrue(corrupted, "Dossier algorithm corrupted array on from==to==3, count=2!")
    }


    @Test
    fun testSpecialEdgeCases() {
        // 1. from == to
        val list1 = arrayListOf(0, 1, 2, 3, 4)
        moveChildrenDossier(list1, 2, 2, 1)
        println("from==to=2, count=1: $list1")

        // 2. count == size
        val list2 = arrayListOf(0, 1, 2, 3)
        moveChildrenDossier(list2, 0, 4, 4)
        println("count==size: $list2")

        // 3. adjacent forward
        val list3 = arrayListOf(0, 1, 2, 3)
        moveChildrenDossier(list3, 1, 3, 1)
        println("move 1 to 3 (count 1): $list3")
    }
}
