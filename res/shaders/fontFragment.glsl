#version 460
out vec4 FragColor;

in vec3 TexCoords;

uniform sampler2D screenTexture;

const int atlasSize = 256;
const int textureSize = 16;
const int texturesPerSide = atlasSize / textureSize;

void main()
{
    float xUv = mod(TexCoords.z, texturesPerSide) / texturesPerSide + mod(TexCoords.x, 1.0) / texturesPerSide;
    float yUv = int(TexCoords.z / texturesPerSide) / float(texturesPerSide) + mod(TexCoords.y, 1.0) / texturesPerSide;
    FragColor = texture(screenTexture, vec2(xUv, yUv));
}