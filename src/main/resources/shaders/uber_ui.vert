attribute vec4 a_position;    // xy = screen position, zw = uv coordinates
attribute vec4 a_color;       // rgba = vertex color / tint (unpacked by GL hardware from 4 unsigned bytes)
attribute vec4 a_boxData;     // xy = local coordinate (0..w, 0..h), zw = box size (width, height)
attribute vec4 a_style;       // x = cornerRadius (legacy/fallback), y = borderWidth, z = mode, w = texUnit
attribute vec4 a_cornerRadii; // x = topStart, y = topEnd, z = bottomEnd, w = bottomStart
attribute vec4 a_borderColor; // rgba = border color (unpacked by GL hardware from 4 unsigned bytes)
attribute vec4 a_clipRect;    // xy = min(x,y), zw = max(x,y) analytical scissor clip rectangle

uniform mat4 u_projTrans;

varying vec2 v_texCoords;
varying vec4 v_color;
varying vec2 v_localCoord;
varying vec2 v_boxSize;
varying vec4 v_style;
varying vec4 v_cornerRadii;
varying vec4 v_borderColor;
varying vec4 v_clipRect;
varying vec2 v_screenCoord;

void main() {
    v_texCoords = a_position.zw;
    v_color = a_color;
    v_color.a = v_color.a * (255.0 / 254.0);
    v_localCoord = a_boxData.xy;
    v_boxSize = a_boxData.zw;
    v_style = a_style;
    v_cornerRadii = a_cornerRadii;
    v_borderColor = a_borderColor;
    v_borderColor.a = v_borderColor.a * (255.0 / 254.0);
    v_clipRect = a_clipRect;
    v_screenCoord = a_position.xy;

    gl_Position = u_projTrans * vec4(a_position.xy, 0.0, 1.0);
}
