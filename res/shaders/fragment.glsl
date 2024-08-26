#version 460
out vec4 FragColor;

in vec2 textureCoords;
in vec4 outData;
in float passLight;

const float border = 0.03125;

uniform sampler2D textureSampler;
uniform float dayTime;

const int atlasSize = 128;
const int textureSize = 32;
const int texturesPerSide = atlasSize / textureSize;

void main()
{
    if (textureCoords.x > 1 || textureCoords.y < 1) {
        discard;
    }

    float xUv = mod(outData.z, texturesPerSide) / texturesPerSide + mod(outData.x, 1.0) / texturesPerSide;
    float yUv = int(outData.z / texturesPerSide) / float(texturesPerSide) + mod(outData.y, 1.0) / texturesPerSide;
    vec4 color = texture(textureSampler, vec2(xUv, yUv));
    if (color.a < 1.0) {
        discard;
    }
    float r = ((int(passLight) >> 8) & 0xf) + 1;
    float g = ((int(passLight) >> 4) & 0xf) + 1;
    float b = (int(passLight) & 0xf) + 1;
    float s = max(dayTime, 0.25f) * 8;
    FragColor = vec4((vec3(color.r * max(r, s), color.g * max(g, s), color.b * max(b, s)) * outData.a / 8.0), 1.0);
}