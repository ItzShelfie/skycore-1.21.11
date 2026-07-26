#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 TintColor;
layout(location = 3) in vec4 ClipRect;
layout(location = 4) in vec4 ClipRadius;

layout(std140) uniform PackedGlobals {
    vec2 screenSize;
};

out vec2 uv;
flat out vec4 tintColor;
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
    tintColor = TintColor;
    clipRect = ClipRect;
    clipRadius = ClipRadius;
}
