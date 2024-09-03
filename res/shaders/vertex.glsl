#version 460

layout (location = 0) in uint data;
layout (location = 1) in uvec2 position;

out vec2 textureCoords;
out vec4 outData;
out vec3 passLight;

uniform mat4 proj;
uniform mat4 view;
uniform float dayTime;
uniform float worldTime;
uniform mat4 shadowProj;
uniform mat4 shadowView;

layout(packed, binding = 0) buffer positionBuffer
{
    int positionData[];
};

const float shadow[6] = {
0.875f,
0.875f,
0.75f,
0.75f,
1.0f,
0.5f
};

const vec3 normal[6] = {
vec3(0, 0, 1),
vec3(0, 0, -1),
vec3(-1, 0, 0),
vec3(1, 0, 0),
vec3(0, 1, 0),
vec3(0, -1, 0)
};

out VS_OUT {
    vec3 FragPos;
    vec3 Normal;
    vec2 TexCoords;
    vec4 FragPosLightSpace;
} vs_out;

vec3 getChunkPosition(int index) {
    return vec3((positionData[index * 2] & 0xffff) - 1024 * 32, (positionData[index * 2] >> 16) & 0xffff - 1024 * 32, positionData[index * 2 + 1] - 1024 * 32);
}

void main()
{
    int x = int(data >> 5);
    int y = int(data & 0x1fu) - 1;
    vec3 chunkPosition = getChunkPosition(gl_DrawID);
    float positionX = int((position.x >> 6) & 0x3fu);
    float positionY = int(position.x & 0x3fu);
    float positionZ = int((position.x >> 12) & 0x3fu);
    int face = int((position.x >> 18) & 0x7u);
    int width = int((position.x >> 21) & 0x1fu) + 1;
    int height = int((position.x >> 26) & 0x1fu) + 1;
    int texture = int(position.y & 0xfu);
    int light = int(position.y >> 4) & 0xfff;
    if (texture == 2) {
        positionX += (sin((positionX + worldTime) * 60) + 1) / 64;
        positionY += (sin((positionY + worldTime + 2) * 60) + 1) / 64;
        positionZ += (sin((positionZ + worldTime + 5) * 60) + 1) / 64;
    }

    float offsetX = positionX + chunkPosition.x * 32 - 1;
    float offsetY = positionY + chunkPosition.y * 32 - 1;
    float offsetZ = positionZ + chunkPosition.z * 32 - 1;

    if (face == 0) {
        gl_Position = vec4(x * width + offsetX, y * height + offsetY, offsetZ + 1, 1.0);
    } else if (face == 1) {
        gl_Position = vec4((1 - x) * width + offsetX, y * height + offsetY, offsetZ, 1.0);
    } else if (face == 2) {
        gl_Position = vec4(offsetX, y * height + offsetY, x * width + offsetZ, 1.0);
    } else if (face == 3) {
        gl_Position = vec4(offsetX + 1, y * height + offsetY, (1 - x) * width + offsetZ, 1.0);
    } else if (face == 4) {
        gl_Position = vec4(x * width + offsetX, offsetY + 1, (1 - y) * height + offsetZ, 1.0);
    } else if (face == 5) {
        gl_Position = vec4(x * width + offsetX, offsetY, y * height + offsetZ, 1.0);
    }
    textureCoords = vec2(x, y + 1);
    outData = vec4(x * width, y * height, texture, shadow[face]);
    passLight = vec3((light >> 8) & 0xf, (light >> 4) & 0xf, light & 0xf) / 16;

    vs_out.FragPos = vec3(gl_Position.xyz);
    vs_out.Normal = normal[face];
    vs_out.FragPosLightSpace = shadowProj * shadowView * vec4(vs_out.FragPos, 1.0);
    gl_Position = proj * view * vec4(vs_out.FragPos, 1.0);

}