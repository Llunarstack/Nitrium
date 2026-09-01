#version 430 core

layout(location = 0) in vec3 aCorner;

layout(std430, binding = 1) readonly buffer Transforms {
	vec4 row0[];
	vec4 row1[];
	vec4 row2[];
	vec4 row3[];
	int animFrame[];
};

uniform mat4 uProjection;
uniform mat4 uView;

out vec4 vColor;

void main() {
	int instance = gl_InstanceID;
	mat4 model = mat4(row0[instance], row1[instance], row2[instance], row3[instance]);
	vec4 world = model * vec4(aCorner, 1.0);
	gl_Position = uProjection * uView * world;
	vColor = vec4(0.35, 0.65, 1.0, 0.35);
}
