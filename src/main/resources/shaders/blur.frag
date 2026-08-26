uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_radius;
uniform vec2 u_dir;

varying vec2 v_texCoord0;

void main() {
    // Clamped continuous sampling step to avoid Nyquist undersampling gaps / grid artifacts
    float safeRadius = clamp(u_radius, 0.4, 1.2);
    vec2 step = u_texelSize * u_dir * safeRadius;

    // Mathematically correct center-peaked 9-tap Gaussian bilinear convolution (Zero Ghosting)
    vec4 color = texture2D(u_texture, v_texCoord0) * 0.382928;
    color += texture2D(u_texture, v_texCoord0 + step * 1.411764) * 0.241536;
    color += texture2D(u_texture, v_texCoord0 - step * 1.411764) * 0.241536;
    color += texture2D(u_texture, v_texCoord0 + step * 3.294117) * 0.067000;
    color += texture2D(u_texture, v_texCoord0 - step * 3.294117) * 0.067000;

    gl_FragColor = color;
}
