#version 460

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 uvs;

out vec2 TexCoords;

uniform mat4 view;
uniform mat4 proj;
uniform mat4 trans;
uniform vec3 camPos;

void main()
{
    gl_Position = proj * view * trans * vec4(position.x, position.y, 0, 1);

    TexCoords = uvs;
}