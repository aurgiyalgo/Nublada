#version 460

layout (location = 0) in uint data;
layout (location = 1) in uvec2 position;

out vec2 textureCoords;
out vec4 outData;
out vec3 outChunkPos;
out vec3 outCamPos;

uniform mat4 proj;
uniform mat4 view;
uniform float dayTime;
uniform float worldTime;
uniform ivec3 camChunkPos;

layout(packed, binding = 0) buffer positionBuffer
{
    int positionData[];
};

const float shadow[6] = {
    0.875,
    0.875,
    0.75,
    0.75,
    1.0,
    0.5
};

vec3 getChunkPosition(int index) {
    return vec3(positionData[index * 2] & 0xffff, (positionData[index * 2] >> 16) & 0xffff, positionData[index * 2 + 1] & 0xffff);
}

void main()
{
    int x = int(data >> 5);
    int y = int(data & 0x1fu) - 1;
    vec3 chunkPosition = getChunkPosition(gl_DrawID) - camChunkPos;
    int face = int((position.x >> 20) & 0x7u);
    float positionX = int((position.x >> 8) & 0x3fu);
    float positionY = int(position.x & 0xffu);
    float positionZ = int((position.x >> 14) & 0x3fu);
    int width = int((position.x >> 23) & 0x7fu) + 1;
    int height = int((position.y >> 20) & 0x7fu) + 1;
    int light = int(position.y & 0xfffu);
    int texture = (int(position.y >> 12) & 0xff);

    if (face == 0) {
        positionY++;
    } else if (face == 1) {
        positionY++;
        positionZ++;
    } else if (face == 2) {
        positionY++;
        positionX++;
    } else if (face == 3) {
        positionY++;
    } else if (face == 4) {

    } else if (face == 5) {
        positionY++;
    }

    float offsetX = positionX + chunkPosition.x * 32;
    float offsetY = positionY - 1;
    float offsetZ = positionZ + chunkPosition.z * 32;

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

    gl_Position = proj * view * vec4(vec3(gl_Position.xyz), 1.0);

}