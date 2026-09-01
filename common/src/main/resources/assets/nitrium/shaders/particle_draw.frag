#version 330 core
out vec4 fragColor;

void main() {
    vec2 uv = gl_PointCoord * 2.0 - 1.0;
    float dist = dot(uv, uv);
    if (dist > 1.0) {
        discard;
    }
    float alpha = 1.0 - dist;
    fragColor = vec4(1.0, 0.9, 0.6, alpha * 0.75);
}
