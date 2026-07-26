#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec2 RectSize;
layout(location = 3) in vec4 Radius;
layout(location = 4) in vec4 Color1;
layout(location = 5) in vec4 Color2;
layout(location = 6) in vec4 Color3;
layout(location = 7) in vec4 Color4;
layout(location = 8) in vec4 Stroke1;
layout(location = 9) in vec4 Stroke2;
layout(location = 10) in vec4 Stroke3;
layout(location = 11) in vec4 Stroke4;
layout(location = 12) in vec2 RoundParams;
layout(location = 13) in vec4 ClipRect;
layout(location = 14) in vec4 ClipRadius;

layout(std140) uniform PackedGlobals {
    vec2 screenSize;
};

out vec2 uv;
flat out vec2 rectSize;
flat out vec4 radius;
flat out vec4 color1;
flat out vec4 color2;
flat out vec4 color3;
flat out vec4 color4;
flat out vec4 stroke1;
flat out vec4 stroke2;
flat out vec4 stroke3;
flat out vec4 stroke4;
flat out vec2 roundParams;
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
    rectSize = RectSize;
    radius = Radius;
    color1 = Color1;
    color2 = Color2;
    color3 = Color3;
    color4 = Color4;
    stroke1 = Stroke1;
    stroke2 = Stroke2;
    stroke3 = Stroke3;
    stroke4 = Stroke4;
    roundParams = RoundParams;
    clipRect = ClipRect;
    clipRadius = ClipRadius;
}
