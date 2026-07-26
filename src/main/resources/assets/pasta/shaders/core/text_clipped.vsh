#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 TextColor;
layout(location = 3) in vec4 ClipRect;
layout(location = 4) in vec4 ClipRadius;

layout(std140) uniform staticUniforms {
    float range;
};

layout(std140) uniform dynamicUniforms {
    vec2 screenSize;
    vec4 unusedTextColor;
    vec4 unusedClipRect;
    vec4 unusedClipRadius;
};

out vec2 uv;
flat out vec4 textColor;
flat out vec4 clipRect;
flat out vec4 clipRadius;

void main() {
    mat4 projection = mat4(
        2.0 / screenSize.x, 0.0, 0.0, 0.0,
        0.0, -2.0 / screenSize.y, 0.0, 0.0,
        0.0, 0.0, -1.0, 0.0,
        -1.0, 1.0, 0.0, 1.0
    );

    gl_Position = projection * vec4(Position.xy, 0.0, 1.0);
    uv = UV0;
    textColor = TextColor;
    clipRect = ClipRect;
    clipRadius = ClipRadius;
}
