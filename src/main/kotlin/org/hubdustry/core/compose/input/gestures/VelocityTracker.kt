package org.hubdustry.core.compose.input.gestures

// ─── 1D Velocity Tracker Architecture ─────────────────────────────
//
//   Circular Ring Buffer (Zero-GC, 10 samples fixed capacity):
//   [ S0 ] ──► [ S1 ] ──► [ S2 ] ──► [ S3 ] ──► [ S4 ] ...
//                ▲
//                │ writeIndex (wraps modulo MAX_SAMPLES)
//
//   Least Squares Linear Regression over sliding sample window:
//   Slope = Σ( (t - meanT) * (p - meanP) ) / Σ( (t - meanT)^2 )
// ──────────────────────────────────────────────────────────────────

/**
 * Bộ theo dõi vận tốc cử chỉ 1 chiều (1D Velocity Tracker) cho cử chỉ vuốt trượt (Fling).
 *
 * TUÂN THỦ KỶ LUẬT HIỆU NĂNG ZERO-GC (GEMINI.md Rule 3.1 & 1.1):
 * - Sử dụng Circular Buffer mảng nguyên thủy cố định (LongArray, FloatArray), 0 byte heap allocation.
 * - Tính toán vận tốc giải tích bằng hồi quy tuyến tính bình phương tối thiểu (Least Squares)
 *   qua cửa sổ trượt [sampleWindowMillis] gần nhất (mặc định 100ms).
 * - Tự động loại bỏ nhiễu rung tay hoặc cử chỉ quá ngắn (< 25ms duration), trả về 0f.
 * - Triết lý NEVER-THROW: Không bao giờ văng ngoại lệ toán học, lọc sạch NaN và số vô hạn.
 */
class VelocityTracker1D(sampleWindowMillis: Long = DEFAULT_SAMPLE_WINDOW_MILLIS) {

    companion object {
        private const val MAX_SAMPLES = 10
        private const val MAX_VELOCITY = 15000f // 15,000 px/s
        private const val DEFAULT_SAMPLE_WINDOW_MILLIS = 100L
        private const val MIN_SAMPLE_WINDOW_MILLIS = 10L
        private const val MIN_TRACKING_DURATION_MILLIS = 25L
        private const val MIN_DENOMINATOR_EPSILON = 0.0001
        private const val MILLIS_TO_SECONDS = 1000.0
    }

    private val sampleWindowMillis: Long = sampleWindowMillis.coerceAtLeast(MIN_SAMPLE_WINDOW_MILLIS)

    private val times = LongArray(MAX_SAMPLES)
    private val positions = FloatArray(MAX_SAMPLES)
    private var writeIndex = 0
    private var sampleCount = 0

// ─── Public Velocity Sampling & Computation ───────────────────────

    /**
     * Thêm một mẫu vị trí [position] tại thời điểm [timeMillis].
     * Tự động lọc NaN và số vô hạn (Gateway Sanitization).
     */
    fun addPosition(timeMillis: Long, position: Float) {
        if (position.isNaN() || position.isInfinite()) return

        times[writeIndex] = timeMillis
        positions[writeIndex] = position
        writeIndex = (writeIndex + 1) % MAX_SAMPLES
        if (sampleCount < MAX_SAMPLES) {
            sampleCount++
        }
    }

    /**
     * Tính toán vận tốc hiện tại theo đơn vị pixel / giây (px/s).
     * Trả về giá trị dương hoặc âm tùy theo chiều dịch chuyển.
     */
    fun calculateVelocity(): Float {
        if (sampleCount < 2) return 0f

        // Tìm mẫu mới nhất (newest sample)
        val newestIndex = (writeIndex - 1 + MAX_SAMPLES) % MAX_SAMPLES
        val newestTime = times[newestIndex]

        // Thu thập các mẫu nằm trong cửa sổ thời gian sampleWindowMillis (mặc định 100ms)
        var validCount = 0
        var sumTime = 0.0
        var sumPosition = 0.0

        for (i in 0 until sampleCount) {
            val sampleIndex = (writeIndex - 1 - i + MAX_SAMPLES * 2) % MAX_SAMPLES
            val sampleAgeMillis = newestTime - times[sampleIndex]
            if (sampleAgeMillis > sampleWindowMillis) {
                break
            }
            validCount++
            // Chuẩn hóa trục thời gian tương đối so với newestTime (gốc 0 tại điểm chạm mới nhất để tránh tràn số)
            sumTime += -sampleAgeMillis.toDouble()
            sumPosition += positions[sampleIndex].toDouble()
        }

        if (validCount < 2) return 0f

        // Kiểm tra thời gian tối thiểu giữa mẫu cũ nhất và mới nhất (loại trừ drag nhân tạo < 25ms)
        val oldestIndex = (writeIndex - validCount + MAX_SAMPLES * 2) % MAX_SAMPLES
        val totalDuration = newestTime - times[oldestIndex]
        if (totalDuration < MIN_TRACKING_DURATION_MILLIS) return 0f

        val meanTime = sumTime / validCount
        val meanPosition = sumPosition / validCount

        // Tính độ dốc (Slope) qua công thức bình phương tối thiểu: sum((t - meanT) * (p - meanP)) / sum((t - meanT)^2)
        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until validCount) {
            val sampleIndex = (writeIndex - 1 - i + MAX_SAMPLES * 2) % MAX_SAMPLES
            val deltaTimeFromMean = (-newestTime + times[sampleIndex]).toDouble() - meanTime
            val deltaPositionFromMean = positions[sampleIndex].toDouble() - meanPosition
            numerator += deltaTimeFromMean * deltaPositionFromMean
            denominator += deltaTimeFromMean * deltaTimeFromMean
        }

        if (denominator <= MIN_DENOMINATOR_EPSILON) return 0f

        // Đổi từ px/ms sang px/s (nhân với 1000)
        val velocityPxPerSec = (numerator / denominator * MILLIS_TO_SECONDS).toFloat()

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
        writeIndex = 0
        sampleCount = 0
    }
}

