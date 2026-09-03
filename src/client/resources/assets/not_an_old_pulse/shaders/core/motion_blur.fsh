#version 150

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

#ifndef BLUR_ALPHA
#define BLUR_ALPHA 0.35
#endif

void main() {
    vec4 history = texture(InSampler, texCoord);
    fragColor = vec4(history.rgb, BLUR_ALPHA);
}
