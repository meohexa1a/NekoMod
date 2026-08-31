uniform sampler2D u_atlas;    // Texture Unit 0 (Atlas / BMFont / Icons / White Pixel)
uniform sampler2D u_gameBlur; // Texture Unit 2 (Blurred Game World FBO)
uniform vec2 u_screenSize;    // Screen dimensions (width, height) in pixels
uniform float u_hasBlur;      // 1.0 = Blur Texture active, 0.0 = Fallback to tint

varying vec2 v_texCoords;
varying vec4 v_color;
varying vec2 v_localCoord;
varying vec2 v_boxSize;
varying vec4 v_style;         // x = cornerRadius, y = borderWidth, z = mode, w = texUnit
varying vec4 v_borderColor;
varying vec4 v_clipRect;      // Analytical Scissor Clip: xy = min(x,y), zw = max(x,y)
varying vec2 v_screenCoord;   // Logical Screen Position (DPI-independent)

// Modes:
// 0.0 = MODE_FONT (BMFont text glyph)
// 1.0 = MODE_SDF_BOX (Rounded rect with optional texture/solid fill and border)
// 2.0 = MODE_TEXTURE (Plain quad texture/icon without SDF math)
// 3.0 = MODE_GLASS (Frosted glass sampling blurred game background)

float computeRoundedBoxSDF(vec2 point, vec2 size, float radius) {
    vec2 halfSize = size * 0.5;
    vec2 centeredPoint = point - halfSize;
    float clampedRadius = min(radius, min(halfSize.x, halfSize.y));
    vec2 offset = abs(centeredPoint) - halfSize + clampedRadius;
    return length(max(offset, 0.0)) + min(max(offset.x, offset.y), 0.0) - clampedRadius;
}

void main() {
    // 1. Fast Analytical Scissor Clip (DPI-independent in logical screen coordinates)
    vec2 clipInside = step(v_clipRect.xy, v_screenCoord.xy) * step(v_screenCoord.xy, v_clipRect.zw);
    if (clipInside.x * clipInside.y < 0.5) {
        discard;
    }

    float mode = v_style.z;
    float cornerRadius = v_style.x;
    float borderWidth = v_style.y;

    // --- MODE 0: BMFont Text Glyph ---
    if (mode < 0.5) {
        vec4 fontTex = texture2D(u_atlas, v_texCoords);
        gl_FragColor = v_color * fontTex;
        return;
    }

    // --- MODE 2: Plain Textured Quad (Fast-path, no SDF) ---
    if (mode > 1.5 && mode < 2.5) {
        vec4 sampledTexture = texture2D(u_atlas, v_texCoords);
        gl_FragColor = v_color * sampledTexture;
        return;
    }

    // --- SDF Box & Glass Modes (MODE 1 & MODE 3) ---
    float distance = computeRoundedBoxSDF(v_localCoord, v_boxSize, cornerRadius);
    float edgeSoftness = 1.0;
    float boxAlpha = clamp(0.5 - distance / edgeSoftness, 0.0, 1.0);

    vec4 baseFillColor;

    // Mode 3: Frosted Glass
    if (mode > 2.5) {
        if (u_hasBlur > 0.5) {
            vec2 screenUV = v_screenCoord.xy / max(u_screenSize, vec2(1.0, 1.0));
            vec4 blurredBackground = texture2D(u_gameBlur, screenUV);

            // Saturation vibrance boost (simulates light transmission through frosted glass)
            float lum = dot(blurredBackground.rgb, vec3(0.2126, 0.7152, 0.0722));
            vec3 vibrant = mix(vec3(lum), blurredBackground.rgb, 1.20) * 1.04;

            // Smooth tint blend
            vec3 tintedRgb = mix(vibrant, v_color.rgb, v_color.a);
            baseFillColor = vec4(tintedRgb, 1.0);
        } else {
            // Clean fallback when blur is disabled (solid translucent surface)
            baseFillColor = v_color;
        }
    } else {
        // Mode 1: Solid or Atlas Texture Fill
        vec4 sampledAtlas = texture2D(u_atlas, v_texCoords);
        baseFillColor = v_color * sampledAtlas;
    }

    // Border Composition
    if (borderWidth > 0.001 && v_borderColor.a > 0.001) {
        float innerDistance = distance + borderWidth;
        float borderAlpha = clamp(0.5 + innerDistance / edgeSoftness, 0.0, 1.0);
        float borderFactor = borderAlpha * clamp(0.5 - distance / edgeSoftness, 0.0, 1.0);
        baseFillColor = mix(baseFillColor, v_borderColor, borderFactor * v_borderColor.a);
    }

    // Straight Alpha blending: only modulate alpha channel by boxAlpha (prevents double-alpha darkening on antialiased edges)
    gl_FragColor = vec4(baseFillColor.rgb, baseFillColor.a * boxAlpha);
}
