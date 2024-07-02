#version 460

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 uvs;

out vec2 TexCoords;

void main()
{
    gl_Position = vec4(position.x, position.y, 1, 1);

    TexCoords = uvs;
}