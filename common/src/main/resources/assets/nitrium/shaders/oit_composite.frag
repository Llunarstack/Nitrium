#version 330 core
in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uAccumulation;
uniform sampler2D uRevealage;

void main() {
    vec4 accum = texture(uAccumulation, vUv);
    float reveal = texture(uRevealage, vUv).r;
    float alpha = clamp(reveal, 1e-4, 1.0);
    vec3 color = accum.rgb / max(accum.a, 1e-4);
    fragColor = vec4(color, min(accum.a / alpha, 1.0));
}
