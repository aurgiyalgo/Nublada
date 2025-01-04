#version 460

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 uvs;

out vec3 TexCoords;

uniform vec2 origin;
uniform vec2 size;

uniform float width;
uniform float height;

layout(packed, binding = 4) buffer positionBuffer
{
    int positionData[];
};

void main()
{
    float x = (positionData[gl_InstanceID * 4] / width * 2 - 1 + (position.x + 1) * positionData[gl_InstanceID * 4 + 2] / width);
    float y = ((1 - positionData[gl_InstanceID * 4 + 1] / height) * 2 - 1 + (position.y - 1) * positionData[gl_InstanceID * 4 + 3] / height);
    gl_Position = vec4(x, y, 1, 1);

    TexCoords = vec3(uvs.x, 1 - uvs.y, 1);
}