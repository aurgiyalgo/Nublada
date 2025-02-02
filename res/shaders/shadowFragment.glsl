#version 460
out vec4 FragColor;

in vec2 textureCoords;
in vec4 outData;

const float border = 0.03125;

uniform sampler2D textureSampler;
uniform float dayTime;

const int atlasSize = 256;
const int textureSize = 16;
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
}