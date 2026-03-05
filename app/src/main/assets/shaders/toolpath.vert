#version 300 es
precision mediump float;

uniform mat4 u_MVPMatrix;

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;

out vec4 v_Color;

void main() {
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
    v_Color = a_Color;
}
