#version 330 core
layout(location = 0) in vec2 aCorner;

uniform vec2 uViewport;

void main() {
    gl_Position = vec4(aCorner, 0.0, 1.0);
    gl_PointSize = 4.0;
}
