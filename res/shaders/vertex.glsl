#version 460

layout (location = 0) in uint data;
layout (location = 1) in uvec2 position;

out vec2 textureCoords;
out vec4 outData;
out vec4 passLight;
out vec3 outCamPos;

uniform mat4 proj;
uniform mat4 view;
uniform float dayTime;
uniform float worldTime;
uniform mat4 shadowProj;
uniform mat4 shadowView;
uniform ivec3 camChunkPos;

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
    return vec3(positionData[index * 2] & 0xffff, (positionData[index * 2] >> 16) & 0xffff, positionData[index * 2 + 1] & 0xffff);
}

void main()
{
    int x = int(data >> 5);
    int y = int(data & 0x1fu) - 1;
    vec3 chunkPosition = getChunkPosition(gl_DrawID) - camChunkPos;
    float positionX = int((position.x >> 8) & 0x3fu);
    float positionY = int(position.x & 0xffu);
    float positionZ = int((position.x >> 14) & 0x3fu);
    int face = int((position.x >> 20) & 0x7u);
    int width = int((position.x >> 23) & 0x7fu) + 1;
    int height = int((position.y >> 20) & 0x7fu) + 1;
    int light = int(position.y & 0xfffu);
    int texture = (int(position.y >> 12) & 0xff);
    if (texture == 52) {
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
    passLight = vec4((light >> 9) & 0x7, (light >> 6) & 0x7, (light >> 3) & 0x7, light & 0x7) / 8;

    vs_out.FragPos = vec3(gl_Position.xyz);
    vs_out.Normal = normal[face];
    vs_out.FragPosLightSpace = shadowProj * shadowView * vec4(vs_out.FragPos, 1.0);
    gl_Position = proj * view * vec4(vs_out.FragPos, 1.0);
    outCamPos = camChunkPos;

}