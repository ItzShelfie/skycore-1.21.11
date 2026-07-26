#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform params {
    vec2 screenSize;
    vec2 canvasSize;
    vec2 rectSize;
    vec4 radius;
    vec4 color;
    vec4 gaussian;
    vec4 clipRect;
    vec4 clipRadius;
};

float rdist(vec2 pos, vec2 size, vec4 rad) {
    rad.xy = (pos.x > 0.0) ? rad.xy : rad.wz;
    rad.x = (pos.y > 0.0) ? rad.x : rad.y;

    vec2 v = abs(pos) - size + rad.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - rad.x;
}

float ralpha(vec2 size, vec2 coord, vec4 rad, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), max(center - 1.0, vec2(0.0)), rad);
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

float roundedMask(vec2 pixel, vec2 center, vec2 halfRect, vec4 rad) {
    float dist = rdist(center - pixel, halfRect, rad);
    float aa = max(fwidth(dist), 0.75);
    return smoothstep(aa, -aa, dist);
}

float blurredMask(vec2 pixel, vec2 center, vec2 halfRect, vec4 rad, float blurRadius) {
    if (blurRadius <= 0.01) {
        return roundedMask(pixel, center, halfRect, rad);
    }

    float sigma = max(blurRadius * 0.42, 0.001);
    float total = 0.0;
    float alpha = 0.0;

    for (int y = -4; y <= 4; y++) {
        for (int x = -4; x <= 4; x++) {
            vec2 offset = vec2(float(x), float(y)) * (blurRadius / 4.0);
            float weight = exp(-dot(offset, offset) / (2.0 * sigma * sigma));
            alpha += roundedMask(pixel + offset, center, halfRect, rad) * weight;
            total += weight;
        }
    }

    return alpha / total;
}

void main() {
    float blurRadius = max(gaussian.x, 0.0);
    bool background = gaussian.y > 0.5;
    float maxCornerRadius = min(rectSize.x, rectSize.y) * 0.5;

    vec4 reorderedRadius = vec4(radius.x, radius.z, radius.w, radius.y);
    reorderedRadius = clamp(reorderedRadius, vec4(0.0), vec4(maxCornerRadius));

    vec2 pixel = uv * canvasSize;
    vec2 center = canvasSize * 0.5;
    vec2 halfRect = max(rectSize * 0.5 - 0.5, vec2(0.0));
    float dist = rdist(center - pixel, halfRect, reorderedRadius);

    float alpha = background
            ? blurredMask(pixel, center, halfRect, reorderedRadius, blurRadius)
            : roundedMask(pixel, center, halfRect, reorderedRadius);
    if (!background && blurRadius > 0.0) {
        float outside = max(dist, 0.0);
        float t = clamp(outside / blurRadius, 0.0, 1.0);
        float gaussianAlpha = exp(-4.5 * t * t);
        gaussianAlpha *= 1.0 - smoothstep(0.82, 1.0, t);
        alpha = max(alpha, gaussianAlpha);
    }

    float clipAlpha = clipMask(gl_FragCoord.xy);
    if (clipAlpha <= 0.005) {
        discard;
    }

    alpha *= color.a * clipAlpha;
    if (alpha < 0.005) {
        discard;
    }

    finalColor = vec4(color.rgb, alpha);
}
