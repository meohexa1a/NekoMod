uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_radius;
uniform float u_mode; // 0.0 = Dual Kawase Downsample, 1.0 = Dual Kawase Upsample

varying vec2 v_texCoords;

void main() {
    vec2 offset = u_texelSize * max(u_radius, 0.5);

    if (u_mode < 0.5) {
        // --- DOWNSAMPLE PASS (4-tap rotated bilinear + center box: 5 samples covering 16 texels) ---
        vec4 sum = texture2D(u_texture, v_texCoords) * 4.0;
        sum += texture2D(u_texture, v_texCoords - offset);
        sum += texture2D(u_texture, v_texCoords + offset);
        sum += texture2D(u_texture, v_texCoords + vec2(offset.x, -offset.y));
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x, offset.y));
        gl_FragColor = sum * 0.125;
    } else {
        // --- UPSAMPLE PASS (8-tap tent filter: 8 samples covering 36 texels smoothly) ---
        vec4 sum = vec4(0.0);
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x * 2.0, 0.0));
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x, offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2(0.0, offset.y * 2.0));
        sum += texture2D(u_texture, v_texCoords + vec2(offset.x, offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2(offset.x * 2.0, 0.0));
        sum += texture2D(u_texture, v_texCoords + vec2(offset.x, -offset.y)) * 2.0;
        sum += texture2D(u_texture, v_texCoords + vec2(0.0, -offset.y * 2.0));
        sum += texture2D(u_texture, v_texCoords + vec2(-offset.x, -offset.y)) * 2.0;
        gl_FragColor = sum * (1.0 / 12.0);
    }
}
