#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(std140) uniform staticUniforms {
    float range;
};

layout(std140) uniform dynamicUniforms {
    vec2 screenSize;
    vec4 textColor;
    vec4 clipRect;
    vec4 clipRadius;
};

out vec2 uv;

void main() {
    mat4 projection = mat4(
        2.0 / screenSize.x, 0.0, 0.0, 0.0,
        0.0, -2.0 / screenSize.y, 0.0, 0.0,
        0.0, 0.0, -1.0, 0.0,
        -1.0, 1.0, 0.0, 1.0
    );

    gl_Position = projection * vec4(Position.xy, 0.0, 1.0);
    uv = UV0;
}
