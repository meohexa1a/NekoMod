package org.hubdustry.libs.layout

/**
 * Hướng sắp xếp trục của bố cục tuyến tính (Flex).
 */
enum class Orientation {
    /** Trục chính nằm ngang (Row/HBox), trục phụ thẳng đứng. */
    HORIZONTAL,

    /** Trục chính thẳng đứng (Column/VBox), trục phụ nằm ngang. */
    VERTICAL
}

/**
 * Cờ quy định cách [LayoutNode] chiếm không gian trong slot được phân bổ bởi container.
 */
enum class SizeFlag {
    /** Thu gọn kích thước về minWidth / minHeight (Hug Content). */
    SHRINK,

    /** Chiếm toàn bộ không gian của slot được phân bổ. */
    FILL,

    /** Tranh chấp khoảng trống còn thừa của Container cha theo stretchRatio. */
    EXPAND
}

/**
 * Hướng căn chỉnh khi phần tử có kích thước nhỏ hơn slot khả dụng (ví dụ khi SHRINK).
 */
enum class Alignment {
    /** Căn sát mép trên hoặc mép trái. */
    START,

    /** Căn chính giữa. */
    CENTER,

    /** Căn sát mép dưới hoặc mép phải. */
    END
}

/**
 * Tính toán độ dời tọa độ (offset) dựa trên hướng căn chỉnh [Alignment].
 *
 * @param allocated Không gian khả dụng của slot.
 * @param actual Kích thước thực tế của phần tử.
 * @return Độ dời tương đối so với mép bắt đầu của slot.
 */
fun Alignment.computeOffset(allocated: Float, actual: Float): Float = when (this) {
    Alignment.START -> 0f
    Alignment.CENTER -> (allocated - actual) * 0.5f
    Alignment.END -> allocated - actual
}

