package org.hubdustry.libs.layout

/**
 * Chiến lược bố cục (Layout Policy) theo mô hình 2 pha:
 * - Pha 1 (Bottom-Up): Đo lường kích thước tối thiểu từ lá lên gốc.
 * - Pha 2 (Top-Down): Định vị tọa độ và kích thước từ gốc xuống lá.
 */
interface LayoutPolicy {
    /**
     * Phase 1 (Bottom-Up): Tính toán minWidth và minHeight của container
     * dựa trên kích thước tối thiểu của các con và padding.
     */
    fun computeMinSize(node: LayoutNode)

    /**
     * Phase 2 (Top-Down): Định vị x, y, width, height cho từng node con
     * dựa trên không gian nội bộ khả dụng (inner bounds).
     */
    fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    )
}
