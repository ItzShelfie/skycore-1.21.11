#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D Sampler0;

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

const int KERNEL_RADIUS = 8;
const float RADIUS_POWER = 1.45;
const float SIGMA_SCALE = 0.45;
const float MAX_TINT_STRENGTH = 0.68;

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

vec3 gaussianBlur(vec2 texCoord, float blurRadius) {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float radiusPx = blurRadius * RADIUS_POWER;
    if (radiusPx <= 0.01) {
        return texture(Sampler0, texCoord).rgb;
    }

    float sigma = max(radiusPx * SIGMA_SCALE, 0.001);
    vec2 texel = 1.0 / texSize;
    vec3 sum = vec3(0.0);
    float total = 0.0;

    for (int y = -KERNEL_RADIUS; y <= KERNEL_RADIUS; y++) {
        for (int x = -KERNEL_RADIUS; x <= KERNEL_RADIUS; x++) {
            vec2 samplePx = vec2(float(x), float(y)) * (radiusPx / float(KERNEL_RADIUS));
            float weight = exp(-0.5 * dot(samplePx, samplePx) / (sigma * sigma));
            vec2 sampleUv = clamp(texCoord + samplePx * texel, vec2(0.0), vec2(1.0));
            sum += texture(Sampler0, sampleUv).rgb * weight;
            total += weight;
        }
    }

    return sum / total;
}

void main() {
    float blurRadius = max(gaussian.x, 0.0);
    float maxCornerRadius = min(rectSize.x, rectSize.y) * 0.5;
    vec4 reorderedRadius = vec4(radius.x, radius.z, radius.w, radius.y);
    reorderedRadius = clamp(reorderedRadius, vec4(0.0), vec4(maxCornerRadius));

    float mask = ralpha(rectSize, uv, reorderedRadius, 1.0);
    mask *= clipMask(gl_FragCoord.xy);
    if (mask <= 0.005) {
        discard;
    }

    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 texCoord = gl_FragCoord.xy / texSize;
    texCoord = clamp(texCoord, vec2(0.0), vec2(1.0));

    vec3 blurred = gaussianBlur(texCoord, blurRadius);
    float colorBrightness = max(max(color.r, color.g), color.b);
    float tintStrength = clamp(color.a * (1.0 - colorBrightness) * MAX_TINT_STRENGTH, 0.0, MAX_TINT_STRENGTH);
    vec3 panel = mix(blurred, color.rgb, tintStrength);

    float alpha = mask * color.a;
    if (alpha <= 0.005) {
        discard;
    }

    finalColor = vec4(panel, alpha);
}
