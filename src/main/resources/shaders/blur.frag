uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_radius;
uniform float u_mode; // 0.0 = Dual Kawase Downsample, 1.0 = Dual Kawase Upsample

varying vec2 v_texCoords;

void main() {
    vec2 offset = u_texelSize * u_radius;

    if (u_mode < 0.5) {
        // --- DOWNSAMPLE PASS (5-tap rotated bilinear box: 1 center + 4 diagonal corners) ---
        // Samples 16 source texels smoothly via GPU bilinear filtering
        vec4 sum = texture2D(u_texture, v_texCoords) * 4.0;
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x, -offset.y));
        sum += texture2D(u_texture, v_texCoords + vec2( offset.x, -offset.y));
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x,  offset.y));
        sum += texture2D(u_texture, v_texCoords + vec2( offset.x,  offset.y));
        gl_FragColor = sum * 0.125;
    } else {
        // --- UPSAMPLE PASS (9-tap smooth tent filter with center weight) ---
        // Center weight 4.0 eliminates ghosting/doughnut-hole/split-image artifacts
        vec4 sum = texture2D(u_texture, v_texCoords) * 4.0;

        // 4 diagonal corners (weight 2.0 each = 8.0)
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x, -offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2( offset.x, -offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x,  offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2( offset.x,  offset.y)) * 2.0;

        // 4 cardinal axis edges (weight 1.0 each = 4.0)
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x * 2.0, 0.0));
        sum += texture2D(u_texture, v_texCoords + vec2( offset.x * 2.0, 0.0));
        sum += texture2D(u_texture, v_texCoords + vec2(0.0, -offset.y * 2.0));
        sum += texture2D(u_texture, v_texCoords + vec2(0.0,  offset.y * 2.0));

        // Total weight = 4.0 + 8.0 + 4.0 = 16.0
        gl_FragColor = sum * 0.0625;
    }
}
