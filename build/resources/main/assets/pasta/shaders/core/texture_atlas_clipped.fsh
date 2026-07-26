#version 330 core

in vec2 uv;
flat in vec4 tintColor;
flat in vec4 clipRect;
flat in vec4 clipRadius;

out vec4 finalColor;

uniform sampler2D texSampler;

float rdist(vec2 pos, vec2 size, vec4 rad) {
    rad.xy = (pos.x > 0.0) ? rad.xy : rad.wz;
    rad.x = (pos.y > 0.0) ? rad.x : rad.y;

    vec2 v = abs(pos) - size + rad.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - rad.x;
}

float clipMask(vec2 fragPos) {
    if (clipRect.z <= 0.0 || clipRect.w <= 0.0) {
        return 1.0;
    }

    float radiusSum = clipRadius.x + clipRadius.y + clipRadius.z + clipRadius.w;
    if (radiusSum <= 0.0) {
        vec2 pos = fragPos - clipRect.xy;
        return step(0.0, pos.x) * step(0.0, pos.y) * step(pos.x, clipRect.z) * step(pos.y, clipRect.w);
    }

    vec4 reorderedRadius = vec4(clipRadius.x, clipRadius.z, clipRadius.w, clipRadius.y);
    vec2 clipUv = (fragPos - clipRect.xy) / clipRect.zw;

    vec2 center = clipRect.zw * 0.5;
    float dist = rdist(center - (clipUv * clipRect.zw), center - 1.0, reorderedRadius);
    return 1.0 - smoothstep(0.0, 1.0, dist);
}

void main() {
    vec4 textureColor = texture(texSampler, uv);
    float clipAlpha = clipMask(gl_FragCoord.xy);
    vec4 color = textureColor * tintColor;
    finalColor = vec4(color.rgb, color.a * clipAlpha);

    if (finalColor.a < 0.01) {
        discard;
    }
}
