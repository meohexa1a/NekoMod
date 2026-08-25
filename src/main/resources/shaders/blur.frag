uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_radius;
uniform vec2 u_dir;

varying vec2 v_texCoord0;

void main() {
    vec2 step = u_texelSize * u_dir * max(u_radius, 0.0);

    vec4 color = texture2D(u_texture, v_texCoord0) * 0.2270270270;
    color += texture2D(u_texture, v_texCoord0 + step * 1.3846153846) * 0.3162162162;
    color += texture2D(u_texture, v_texCoord0 - step * 1.3846153846) * 0.3162162162;
    color += texture2D(u_texture, v_texCoord0 + step * 3.2307692308) * 0.0702702703;
    color += texture2D(u_texture, v_texCoord0 - step * 3.2307692308) * 0.0702702703;

    gl_FragColor = color;
}
