#version 300 es
precision mediump float;

uniform vec4 u_Color;

in vec3 v_Intensity;
out vec4 fragColor;

void main() {
    fragColor = vec4(u_Color.rgb * v_Intensity, u_Color.a);
}
