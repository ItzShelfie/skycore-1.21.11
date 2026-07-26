#version 330 core

in vec2 uv;
flat in vec2 rectSize;
flat in vec4 radius;
flat in vec4 color1;
flat in vec4 color2;
flat in vec4 color3;
flat in vec4 color4;
flat in vec4 stroke1;
flat in vec4 stroke2;
flat in vec4 stroke3;
flat in vec4 stroke4;
flat in vec2 roundParams;
flat in vec4 clipRect;
flat in vec4 clipRadius;

out vec4 finalColor;

float rdist(vec2 pos, vec2 size, vec4 rad) {
    rad.xy = (pos.x > 0.0) ? rad.xy : rad.wz;
    rad.x = (pos.y > 0.0) ? rad.x : rad.y;
    vec2 v = abs(pos) - size + rad.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - rad.x;
}

float clipMask(vec2 fragPos) {
    float radiusSum = clipRadius.x + clipRadius.y + clipRadius.z + clipRadius.w;
    if (clipRect.z <= 0.0 || clipRect.w <= 0.0) {
        return 1.0;
    }

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

vec3 overlay(vec3 base, vec3 blend) {
    vec3 result;
    result.r = base.r < 0.5 ? (2.0 * base.r * blend.r) : (1.0 - 2.0 * (1.0 - base.r) * (1.0 - blend.r));
    result.g = base.g < 0.5 ? (2.0 * base.g * blend.g) : (1.0 - 2.0 * (1.0 - base.g) * (1.0 - blend.g));
    result.b = base.b < 0.5 ? (2.0 * base.b * blend.b) : (1.0 - 2.0 * (1.0 - base.b) * (1.0 - blend.b));
    return result;
}

vec4 lerpColor(vec4 a, vec4 b, float t) {
    return mix(a, b, t);
}

vec4 getGradientColor(vec2 value) {
    vec4 topColor = lerpColor(color1, color2, value.x);
    vec4 bottomColor = lerpColor(color3, color4, value.x);
    return lerpColor(topColor, bottomColor, value.y);
}

vec4 getStrokeColor(vec2 value) {
    vec4 topColor = lerpColor(stroke1, stroke2, value.x);
    vec4 bottomColor = lerpColor(stroke3, stroke4, value.x);
    return lerpColor(topColor, bottomColor, value.y);
}

void main() {
    vec4 reorderedRadius = vec4(radius.x, radius.z, radius.w, radius.y);
    vec2 center = rectSize * 0.5;

    float dist = rdist(center - (uv * rectSize), center - 0.5, reorderedRadius);
    float aa = fwidth(dist);
    float outer_mask = smoothstep(aa, -aa, dist);

    if (outer_mask <= 0.005) {
        discard;
    }

    float thickness = roundParams.x;
    float overlayStroke = roundParams.y;
    float fill_mask = smoothstep(aa, -aa, dist + thickness);
    float stroke_mask = outer_mask - fill_mask;

    vec4 base = getGradientColor(uv);
    vec4 stroke = getStrokeColor(uv);

    float top_a = stroke_mask * stroke.a;
    vec3 blended_rgb = (overlayStroke > 0.5) ? overlay(base.rgb, stroke.rgb) : stroke.rgb;
    vec3 frag_rgb = mix(base.rgb, blended_rgb, top_a);

    float clipAlpha = clipMask(gl_FragCoord.xy);
    if (clipAlpha <= 0.005) {
        discard;
    }

    float base_alpha = base.a * outer_mask;
    float frag_alpha = max(base_alpha, top_a) * clipAlpha;

    if (frag_alpha < 0.01) {
        discard;
    }

    finalColor = vec4(frag_rgb, frag_alpha);
}
