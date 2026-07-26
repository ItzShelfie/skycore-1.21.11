#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 ClipRect;
layout(location = 3) in vec4 ClipRadius;

layout(std140) uniform params {
    vec2 screenSize;
    vec4 rectParams;
    vec4 uvRect;
    vec4 radius;
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    float smoothness;
    vec4 uniformClipRect;
    vec4 uniformClipRadius;
};

out vec2 uv;
flat out vec4 quadClipRect;
flat out vec4 quadClipRadius;

void main() {
    mat4 projection = mat4(
        2.0 / screenSize.x, 0.0, 0.0, 0.0,
        0.0, -2.0 / screenSize.y, 0.0, 0.0,
        0.0, 0.0, -1.0, 0.0,
        -1.0, 1.0, 0.0, 1.0
    );

    gl_Position = projection * vec4(Position.xy, 0.0, 1.0);
    uv = UV0;
    quadClipRect = ClipRect;
    quadClipRadius = ClipRadius;
}
