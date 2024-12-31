#version 460

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 uvs;

out vec2 TexCoords;

uniform vec2 origin;
uniform vec2 size;

void main()
{
    float x = (origin.x * 2 - 1 + (position.x + 1) * size.x);
    float y = ((1 - origin.y) * 2 - 1 + (position.y - 1) * size.y);
    gl_Position = vec4(x, y, 1, 1);

    TexCoords = vec2(uvs.x, 1 - uvs.y);
}