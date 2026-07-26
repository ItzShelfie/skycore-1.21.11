#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D texSampler;

layout(std140) uniform staticUniforms {
    float range;
};

layout(std140) uniform dynamicUniforms {
    vec2 screenSize;
    vec4 textColor;
    vec4 clipRect;
    vec4 clipRadius;
};

float median(vec3 c) {
    return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
}

float rdist(vec2 pos, vec2 size, vec4 rad) {
    rad.xy = (pos.x > 0.0) ? rad.xy : rad.wz;
    rad.x = (pos.y > 0.0) ? rad.x : rad.y;
    vec2 v = abs(pos) - size + rad.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - rad.x;
}

float clipMask(vec2 fragPos) {
    float radiusSum = clipRadius.x + clipRadius.y + clipRadius.z + clipRadius.w;
    if (clipRect.z <= 0.0 || clipRect.w <= 0.0 || radiusSum <= 0.0) {
        return 1.0;
    }

    vec4 reorderedRadius = vec4(clipRadius.x, clipRadius.z, clipRadius.w, clipRadius.y);
    vec2 clipUv = (fragPos - clipRect.xy) / clipRect.zw;

    vec2 center = clipRect.zw * 0.5;
    float dist = rdist(center - (clipUv * clipRect.zw), center - 1.0, reorderedRadius);
    return 1.0 - smoothstep(0.0, 1.0, dist);
}

void main() {
    vec3 sampleColor = texture(texSampler, uv).rgb;
    float dist = median(sampleColor) - 0.5;

    float pxRange = range * (1.0 / length(fwidth(uv) * textureSize(texSampler, 0)));
    float alpha = smoothstep(-0.5, 0.5, dist * pxRange);

    float clipAlpha = clipMask(gl_FragCoord.xy);
    vec4 color = vec4(textColor.rgb, textColor.a * alpha * clipAlpha);

    if (color.a < 0.01) {
        discard;
    }

    finalColor = color;
}
