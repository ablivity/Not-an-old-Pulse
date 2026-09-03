#version 150

uniform sampler2D CurrentSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform MotionBlurUniforms {
    mat4 currentProjectionInverse;
    mat4 previousProjection;
    mat4 currentModelViewInverse;
    mat4 previousModelView;
    vec4 cameraDeltaAndFrameTime;
    vec4 blurParams;
};

in vec2 texCoord;

out vec4 fragColor;

float randomNoise(vec2 coord) {
    return fract(sin(dot(coord, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 currentSample = texture(CurrentSampler, texCoord);
    float strength = clamp(blurParams.x, 0.0, 0.99);

    if (strength <= 0.0001) {
        fragColor = currentSample;
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    vec4 currentNdc = vec4(
        texCoord * 2.0 - 1.0,
        depth * 2.0 - 1.0,
        1.0
    );

    vec4 currentView = currentProjectionInverse * currentNdc;
    if (abs(currentView.w) < 0.000001) {
        fragColor = currentSample;
        return;
    }
    currentView /= currentView.w;

    vec4 currentWorldRelative = currentModelViewInverse * currentView;
    vec4 previousWorldRelative = vec4(
        currentWorldRelative.xyz + cameraDeltaAndFrameTime.xyz,
        1.0
    );

    vec4 previousClip = previousProjection
        * previousModelView
        * previousWorldRelative;

    if (previousClip.w <= 0.000001) {
        fragColor = currentSample;
        return;
    }

    vec2 previousNdc = previousClip.xy / previousClip.w;
    float frameTime = max(cameraDeltaAndFrameTime.w, 1.0 / 240.0);
    float maxVelocity = max(blurParams.y, 0.001);
    float velocityScale = blurParams.z;

    vec2 velocity = clamp(
        (currentNdc.xy - previousNdc)
            * (1.0 / frameTime)
            * velocityScale,
        vec2(-maxVelocity),
        vec2(maxVelocity)
    );

    // CH Motion V3-style spatial sampling. At 50% strength this matches the
    // reference shader's default MOTION_BLUR_STRENGTH of roughly 5.5.
    float sampleScale = strength / 2.75;
    float noise = randomNoise(texCoord);

    vec3 accumulated = vec3(0.0);
    const int HALF_SAMPLES = 4;
    const float SAMPLE_COUNT = 9.0;

    for (int i = -HALF_SAMPLES; i <= HALF_SAMPLES; ++i) {
        vec2 sampleCoord = texCoord
            + velocity * (float(i) + noise) * sampleScale;
        accumulated += texture(
            CurrentSampler,
            clamp(sampleCoord, vec2(0.0), vec2(1.0))
        ).rgb;
    }

    fragColor = vec4(accumulated / SAMPLE_COUNT, currentSample.a);
}
