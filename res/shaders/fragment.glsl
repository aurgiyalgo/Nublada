#version 460
out vec4 FragColor;

in vec2 textureCoords;
in vec4 outData;

const float border = 0.03125;

uniform sampler2D textureSampler;

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
    FragColor = vec4(texture(textureSampler, vec2(xUv, yUv)).rgb * outData.a, 1.0);
}