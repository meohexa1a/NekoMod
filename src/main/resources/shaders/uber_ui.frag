uniform sampler2D u_atlas;    // Texture Unit 0 (Atlas / BMFont / Icons / White Pixel)

varying vec2 v_texCoords;
varying vec4 v_color;
varying vec2 v_localCoord;
varying vec2 v_boxSize;
varying vec4 v_style;         // x = cornerRadius, y = borderWidth, z = mode (0 = TEXT, 1 = BOX)
varying vec4 v_cornerRadii;  // x = topStart, y = topEnd, z = bottomEnd, w = bottomStart
varying vec4 v_borderColor;
varying vec4 v_clipRect;      // Analytical Scissor Clip: xy = min(x,y), zw = max(x,y)
varying vec4 v_clipRadii;     // Clip Corner Radii: x = topStart, y = topEnd, z = bottomEnd, w = bottomStart
varying vec2 v_screenCoord;   // Logical Screen Position (DPI-independent)

// Modes:
// 0.0 = MODE_TEXT (BMFont text glyph)
// 1.0 = MODE_BOX  (Solid card, button, rounded avatar/icon, border)

float computeRoundedBoxSDF(vec2 point, vec2 size, vec4 radii) {
    vec2 halfSize = size * 0.5;
    vec2 centeredPoint = point - halfSize;
    // Quadrant Selection in Arc Local Space (Y is up in localCoord):
    // Top-Left:     centeredPoint.x <= 0.0 && centeredPoint.y > 0.0  -> radii.x (topStart)
    // Top-Right:    centeredPoint.x >  0.0 && centeredPoint.y > 0.0  -> radii.y (topEnd)
    // Bottom-Right: centeredPoint.x >  0.0 && centeredPoint.y <= 0.0 -> radii.z (bottomEnd)
    // Bottom-Left:  centeredPoint.x <= 0.0 && centeredPoint.y <= 0.0 -> radii.w (bottomStart)
    float radius = (centeredPoint.y > 0.0) ?
        ((centeredPoint.x > 0.0) ? radii.y : radii.x) :
        ((centeredPoint.x > 0.0) ? radii.z : radii.w);
    float clampedRadius = min(radius, min(halfSize.x, halfSize.y));
    vec2 offset = abs(centeredPoint) - halfSize + clampedRadius;
    return length(max(offset, 0.0)) + min(max(offset.x, offset.y), 0.0) - clampedRadius;
}

void main() {
    // 1. Fast Analytical Scissor Clip (DPI-independent in logical screen coordinates)
    // Tối ưu phần cứng GPU (Coherent Branching): Khi v_style.w <= 0.5 (Quad không nằm trong container cắt gọt),
    // GPU skip 100% toàn bộ bước kiểm tra scissor bên dưới, tiết kiệm chu kỳ ALU cho các widget thông thường.
    if (v_style.w > 0.5) {
        if (v_clipRect.x >= v_clipRect.z || v_clipRect.y >= v_clipRect.w) {
            discard;
        }
        vec2 clipInside = step(v_clipRect.xy, v_screenCoord.xy) * step(v_screenCoord.xy, v_clipRect.zw);
        if (clipInside.x * clipInside.y < 0.5) {
            discard;
        }
    }

    // --- MODE 0: BMFont Text Glyph ---
    // Văn bản luôn luôn chỉ cắt giải tích hình chữ nhật (AABB Scissor Clip), không bo tròn góc
    if (v_style.z < 0.5) {
        vec4 glyphColor = v_color * texture2D(u_atlas, v_texCoords);
        if (glyphColor.a <= 0.001) {
            discard;
        }
        gl_FragColor = glyphColor;
        return;
    }

    // 2. Analytical SDF Rounded Box Clip (Chỉ áp dụng cho Hộp/Cards/Avatars/Borders khi có clip)
    float clipAlpha = 1.0;
    if (v_style.w > 0.5 && max(max(v_clipRadii.x, v_clipRadii.y), max(v_clipRadii.z, v_clipRadii.w)) > 0.001) {
        vec2 clipSize = v_clipRect.zw - v_clipRect.xy;
        vec2 clipPoint = v_screenCoord.xy - v_clipRect.xy;
        float clipDistance = computeRoundedBoxSDF(clipPoint, clipSize, v_clipRadii);
        clipAlpha = clamp(0.5 - clipDistance, 0.0, 1.0);
        if (clipAlpha <= 0.001) {
            discard;
        }
    }

    // --- MODE 1: All Boxes (Solid fill, Rounded card, Avatar, Textured Icon, Border) ---
    float borderWidth = v_style.y;

    float distance = computeRoundedBoxSDF(v_localCoord, v_boxSize, v_cornerRadii);
    float edgeSoftness = 1.0;
    float boxAlpha = clamp(0.5 - distance / edgeSoftness, 0.0, 1.0);

    if (boxAlpha <= 0.001) {
        discard;
    }

    // Lấy mẫu texture lót (màu trơn lấy white pixel, ảnh/icon lấy atlas sprite)
    vec4 baseFillColor = v_color * texture2D(u_atlas, v_texCoords);

    // Hòa trộn viền trong giải tích (Inner Border)
    if (borderWidth > 0.001 && v_borderColor.a > 0.001) {
        float innerDistance = distance + borderWidth;
        float innerAlpha = clamp(0.5 - innerDistance / edgeSoftness, 0.0, 1.0);
        float borderCoverage = clamp(boxAlpha - innerAlpha, 0.0, 1.0);
        float borderMixFactor = borderCoverage / max(boxAlpha, 0.0001);

        // Alpha-weighted composite: Triệt tiêu rò rỉ màu trắng/đen khi nền trong suốt
        float fillWeight = baseFillColor.a * (1.0 - borderMixFactor);
        float borderWeight = v_borderColor.a * borderMixFactor;
        float totalAlpha = fillWeight + borderWeight;

        if (totalAlpha > 0.001) {
            vec3 blendedRgb = (baseFillColor.rgb * fillWeight + v_borderColor.rgb * borderWeight) / totalAlpha;
            baseFillColor = vec4(blendedRgb, totalAlpha);
        } else {
            baseFillColor = vec4(0.0);
        }
    }

    float finalAlpha = baseFillColor.a * boxAlpha * clipAlpha;
    if (finalAlpha <= 0.001) {
        discard;
    }

    // Straight Alpha output cho Arc SpriteBatch (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    gl_FragColor = vec4(baseFillColor.rgb, finalAlpha);
}
