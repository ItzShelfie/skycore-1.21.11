#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D texSampler;

layout(std140) uniform params{
    vec2 screenSize;
    vec4 rectParams;
    vec4 uvRect;
    vec4 radius;
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    float smoothness;
    vec4 clipRect;
    vec4 clipRadius;
};

float rdist(vec2 pos, vec2 size, vec4 rad) {
    rad.xy = (pos.x > 0.0) ? rad.xy : rad.wz;
    rad.x  = (pos.y > 0.0) ? rad.x : rad.y;

    vec2 v = abs(pos) - size + rad.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - rad.x;
}

float ralpha(vec2 size, vec2 coord, vec4 rad, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, rad);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

float clipMask(vec2 fragPos) {
    float radiusSum = clipRadius.x + clipRadius.y + clipRadius.z + clipRadius.w;
    if (clipRect.z <= 0.0 || clipRect.w <= 0.0 || radiusSum <= 0.0) {
        return 1.0;
    }

    vec4 reorderedRadius = vec4(clipRadius.x, clipRadius.z, clipRadius.w, clipRadius.y);
    vec2 clipUv = (fragPos - clipRect.xy) / clipRect.zw;
    return ralpha(clipRect.zw, clipUv, reorderedRadius, 1.0);
}

void main() {
    vec2 rectSize = rectParams.zw;
    vec4 reorderedRadius = vec4(radius.x, radius.z, radius.w, radius.y);
    float shapeAlpha = ralpha(rectSize, uv, reorderedRadius, smoothness);

    if (shapeAlpha <= 0.005) {
        discard;
    }

    float clipAlpha = clipMask(gl_FragCoord.xy);
    if (clipAlpha <= 0.005) {
        discard;
    }

    vec4 topColor = mix(color1, color4, uv.x);
    vec4 bottomColor = mix(color2, color3, uv.x);
    vec4 gradientColor = mix(topColor, bottomColor, uv.y);

    vec2 texUv = uvRect.xy + (uvRect.zw * uv);
    vec4 textureColor = texture(texSampler, texUv);

    vec4 combinedColor = textureColor * gradientColor;

    finalColor = vec4(combinedColor.rgb, combinedColor.a * shapeAlpha * clipAlpha);

    if (finalColor.a < 0.01) {
        discard;
    }
}
