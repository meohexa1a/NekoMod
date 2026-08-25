attribute vec2 a_position;
attribute vec2 a_texCoord0;

varying vec2 v_texCoord0;

void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord0 = a_texCoord0;
}
