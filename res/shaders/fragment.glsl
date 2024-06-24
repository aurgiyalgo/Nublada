#version 460
out vec4 FragColor;

in vec2 textureCoords;
in vec3 outData;

const float border = 0.03125;

void main()
{
    if (textureCoords.x > 1 || textureCoords.y < 1) {
        discard;
    }
    FragColor = vec4(0, 0, outData.z / 256 + 1/256.0, 1.0);
}