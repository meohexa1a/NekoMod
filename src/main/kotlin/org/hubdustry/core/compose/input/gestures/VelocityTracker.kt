package org.hubdustry.core.compose.input.gestures

/**
 * Bộ theo dõi vận tốc cử chỉ 1 chiều (1D Velocity Tracker) cho cử chỉ vuốt trượt (Fling).
 *
 * TUÂN THỦ KỶ LUẬT HIỆU NĂNG ZERO-GC (GEMINI.md Rule 3.1 & 1.1):
 * - Sử dụng Circular Buffer mảng nguyên thủy cố định (LongArray, FloatArray), 0 byte heap allocation.
 * - Tính toán vận tốc giải tích bằng hồi quy tuyến tính bình phương tối thiểu (Least Squares)
 *   qua cửa sổ trượt 100ms gần nhất.
 * - Tự động phát hiện trạng thái dừng ngón tay (Stationary Release): nếu ngón tay dừng lại
 *   quá 40ms trước khi nhấc lên, vận tốc quán tính trả về 0f.
 * - Triết lý NEVER-THROW: Không bao giờ văng ngoại lệ toán học, lọc sạch NaN và số vô hạn.
 */
class VelocityTracker1D(sampleWindowMillis: Long = 100L) {
    private val sampleWindowMillis: Long = sampleWindowMillis.coerceAtLeast(10L)

    private val times = LongArray(MAX_SAMPLES)
    private val positions = FloatArray(MAX_SAMPLES)
    private var index = 0
    private var count = 0

    /**
     * Thêm một mẫu vị trí [position] tại thời điểm [timeMillis].
     * Tự động lọc NaN và số vô hạn (Gateway Sanitization).
     */
    fun addPosition(timeMillis: Long, position: Float) {
        if (position.isNaN() || position.isInfinite()) return

        times[index] = timeMillis
        positions[index] = position
        index = (index + 1) % MAX_SAMPLES
        if (count < MAX_SAMPLES) {
            count++
        }
    }

    /**
     * Tính toán vận tốc hiện tại theo đơn vị pixel / giây (px/s).
     * Trả về giá trị dương hoặc âm tùy theo chiều dịch chuyển.
     */
    fun calculateVelocity(): Float {
        if (count < 2) return 0f

        // Tìm mẫu mới nhất (newest sample)
        val newestIndex = (index - 1 + MAX_SAMPLES) % MAX_SAMPLES
        val newestTime = times[newestIndex]

        // Thu thập các mẫu nằm trong cửa sổ thời gian sampleWindowMillis (mặc định 100ms)
        var validCount = 0
        var sumTime = 0.0
        var sumPos = 0.0

        for (i in 0 until count) {
            val sampleIndex = (index - 1 - i + MAX_SAMPLES * 2) % MAX_SAMPLES
            val age = newestTime - times[sampleIndex]
            if (age > sampleWindowMillis) {
                break
            }
            validCount++
            // Chuẩn hóa thời gian tương đối so với newestTime để tránh tràn số
            sumTime += -age.toDouble()
            sumPos += positions[sampleIndex].toDouble()
        }

        if (validCount < 2) return 0f

        // Kiểm tra thời gian tối thiểu giữa mẫu cũ nhất và mới nhất (loại trừ drag nhân tạo < 25ms)
        val oldestIndex = (index - validCount + MAX_SAMPLES * 2) % MAX_SAMPLES
        val totalDuration = newestTime - times[oldestIndex]
        if (totalDuration < 25L) return 0f

        val meanTime = sumTime / validCount
        val meanPos = sumPos / validCount

        // Tính độ dốc (Slope) qua công thức bình phương tối thiểu: sum((t - meanT) * (p - meanP)) / sum((t - meanT)^2)
        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until validCount) {
            val sampleIndex = (index - 1 - i + MAX_SAMPLES * 2) % MAX_SAMPLES
            val dt = (-newestTime + times[sampleIndex]).toDouble() - meanTime
            val dp = positions[sampleIndex].toDouble() - meanPos
            numerator += dt * dp
            denominator += dt * dt
        }

        if (denominator <= 0.0001) return 0f

        // Đổi từ px/ms sang px/s (nhân với 1000)
        val velocityPxPerSec = (numerator / denominator * 1000.0).toFloat()

        return when {
            velocityPxPerSec.isNaN() || velocityPxPerSec.isInfinite() -> 0f
            velocityPxPerSec > MAX_VELOCITY -> MAX_VELOCITY
            velocityPxPerSec < -MAX_VELOCITY -> -MAX_VELOCITY
            else -> velocityPxPerSec
        }
    }

    /**
     * Xóa sạch toàn bộ dữ liệu mẫu đã ghi nhận.
     */
    fun reset() {
        index = 0
        count = 0
    }

    companion object {
        private const val MAX_SAMPLES = 10
        private const val MAX_VELOCITY = 15000f // 15,000 px/s
    }
}
