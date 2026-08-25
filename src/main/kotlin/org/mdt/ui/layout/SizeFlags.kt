package org.mdt.ui.layout

object SizeFlags {
    /** Do not expand, place at the start of the allocated slot. */
    const val SHRINK_BEGIN = 0
    /** Fill the entire allocated slot. */
    const val FILL = 1
    /** Expand to take up available space in the container. */
    const val EXPAND = 2
    /** Expand to take up available space and fill the entire slot. */
    const val EXPAND_FILL = FILL or EXPAND
    /** Do not expand, center within the allocated slot. */
    const val SHRINK_CENTER = 4
    /** Do not expand, place at the end of the allocated slot. */
    const val SHRINK_END = 8
}
