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
    float r = (int(passLight) >> 8) & 0xf;
    float g = (int(passLight) >> 4) & 0xf;
    float b = int(passLight) & 0xf;
    FragColor = vec4(color.rgb * max(passLight / (8), dayTime * 0.8 + 0.2) * outData.a, 1.0);
}