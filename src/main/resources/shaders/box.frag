uniform vec2 u_size;
uniform float u_opacity;
uniform vec4 u_cornerRadii;
uniform float u_edgeSoftness;
uniform float u_fillMode;
uniform vec4 u_fillColor;

uniform sampler2D u_fillTexture;
uniform vec2 u_uvScale;
uniform vec2 u_uvOffset;

uniform float u_borderWidth;
uniform vec4 u_borderColor;
uniform float u_borderStyle;
uniform float u_dashLength;
uniform float u_dashRatio;

uniform vec4 u_innerShadowColor;
uniform float u_innerShadowSpread;
uniform float u_innerShadowBlur;

uniform vec4 u_glowColor;
uniform float u_glowSpread;
uniform float u_glowBlur;

uniform sampler2D u_backdropTex;
uniform float u_backdropWeight;
uniform vec4 u_backdropCoords;
uniform float u_backdropBlend;
uniform float u_backdropMinAlpha;

uniform vec4 u_colorFilter;
uniform float u_noiseAmount;

varying vec4 v_color;
varying vec2 v_texCoord0;

float roundedRectSDF(vec2 p, vec2 size, vec4 radii) {
    vec2 halfSize = size * 0.5;
    vec2 cp = p - halfSize;
    vec4 r = max(radii, 0.0);
    r = min(r, vec4(min(size.x, size.y) * 0.5));

    float cr = (cp.x > 0.0)
        ? ((cp.y > 0.0) ? r.y : r.z)
        : ((cp.y > 0.0) ? r.x : r.w);

    vec2 q = abs(cp) - halfSize + cr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - cr;
}

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

float boxShadow(vec2 p, vec2 size, vec4 radii, float spread, float blur) {
    vec2 sp = vec2(spread * 0.5);
    vec2 expandedSize = size + sp * 2.0;
    vec4 expandedRadii = radii + vec4(spread * 0.5);
    float outer = roundedRectSDF(p, expandedSize, expandedRadii);
    if (outer > blur) return 0.0;
    return gaussian(outer, blur * 0.4);
}

vec3 applyFilter(vec3 clr, vec4 filter) {
    float mode = filter.x;
    float amount = filter.y;
    if (amount <= 0.0) return clr;
    if (mode == 1.0) {
        float gray = dot(clr, vec3(0.299, 0.587, 0.114));
        return mix(clr, vec3(gray), amount);
    } else if (mode == 2.0) {
        vec3 sepia = vec3(
            dot(clr, vec3(0.393, 0.769, 0.189)),
            dot(clr, vec3(0.349, 0.686, 0.168)),
            dot(clr, vec3(0.272, 0.534, 0.131))
        );
        return mix(clr, sepia, amount);
    } else if (mode == 3.0) {
        return clr * (1.0 + amount);
    } else if (mode == 4.0) {
        return mix(clr, vec3(1.0) - clr, amount);
    }
    return clr;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 uv = v_texCoord0;
    vec2 p = uv * u_size;
    float edge = u_edgeSoftness;

    float d = roundedRectSDF(p, u_size, u_cornerRadii);
    float alpha = 1.0 - smoothstep(-edge, edge, d);
    if (alpha < 0.001) discard;

    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);

    if (u_fillMode == 0.0) {
        color = vec4(u_fillColor.rgb, u_fillColor.a * alpha);
    } else if (u_fillMode == 1.0) {
        vec2 texUv = uv * u_uvScale + u_uvOffset;
        vec4 texColor = texture2D(u_fillTexture, texUv);
        color = vec4(texColor.rgb * u_fillColor.rgb, texColor.a * u_fillColor.a * alpha);
    }

    color.a *= u_opacity;

    if (u_backdropWeight > 0.0) {
        vec2 backUv = u_backdropCoords.xy + uv * (u_backdropCoords.zw - u_backdropCoords.xy);
        vec4 backColor = texture2D(u_backdropTex, backUv);
        float blend = u_backdropBlend;
        color.rgb = mix(color.rgb, backColor.rgb, blend * u_backdropWeight);
        color.a = max(color.a, max(backColor.a, u_backdropMinAlpha) * u_backdropWeight * blend);
    }

    if (u_glowColor.a > 0.0) {
        float glow = boxShadow(p, u_size, u_cornerRadii, u_glowSpread, u_glowBlur);
        color.rgb += u_glowColor.rgb * glow * u_glowColor.a;
        color.a = max(color.a, glow * u_glowColor.a);
    }

    if (u_innerShadowColor.a > 0.0) {
        float inner = boxShadow(p, u_size, u_cornerRadii, -u_innerShadowSpread, u_innerShadowBlur);
        inner = 1.0 - inner;
        color.rgb = mix(color.rgb, u_innerShadowColor.rgb, inner * u_innerShadowColor.a);
    }

    if (u_borderWidth > 0.0) {
        float bw = u_borderWidth;
        vec2 innerSize = vec2(max(u_size.x - bw * 2.0, 0.0), max(u_size.y - bw * 2.0, 0.0));
        vec4 innerRadii = max(u_cornerRadii - vec4(bw), vec4(0.0));
        float outerD = roundedRectSDF(p, u_size, u_cornerRadii);
        float innerD = roundedRectSDF(p - vec2(bw), innerSize, innerRadii);
        float borderAlpha = smoothstep(-edge, edge, innerD) - smoothstep(-edge, edge, outerD);
        borderAlpha = clamp(borderAlpha, 0.0, 1.0);

        if (borderAlpha > 0.001) {
            float draw = 1.0;
            if (u_borderStyle > 0.0) {
                vec2 hs = u_size * 0.5;
                float angle = atan(p.y - hs.y, p.x - hs.x);
                float peri = 2.0 * (u_size.x + u_size.y);
                float dist = angle / 6.2831853 * peri;
                float seg = mod(dist, u_dashLength) / u_dashLength;
                draw = step(seg, u_dashRatio);
                if (u_borderStyle > 1.0) draw = abs(draw - step(0.5, seg));
            }
            color.rgb = mix(color.rgb, u_borderColor.rgb, borderAlpha * draw * u_opacity);
            color.a = max(color.a, borderAlpha * draw * u_opacity);
        }
    }

    color.rgb = applyFilter(color.rgb, u_colorFilter);

    if (u_noiseAmount > 0.0) {
        float n = hash(p);
        color.rgb = mix(color.rgb, vec3(n), u_noiseAmount * 0.1);
    }

    color.a *= v_color.a;
    gl_FragColor = color;
}
