#version 460

layout (location = 0) in uint data;

out vec2 textureCoords;
out vec4 outData;

uniform mat4 proj;
uniform mat4 view;

layout(packed, binding = 0) readonly restrict buffer positionBuffer
{
    int positionData[];
};

layout(packed, binding = 1) readonly restrict buffer vertexBuffer
{
    int vertexData[];
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
    int encodedPosition = positionData[index];
    return vec3((encodedPosition >> 7) & 0x7f, encodedPosition & 0x7f, (encodedPosition >> 14) & 0x7f);
}

void main()
{
    int x = int(data >> 5);
    int y = int(data & 0x1fu) - 1;
    int dataIndex = gl_InstanceID + gl_BaseInstance;
    uvec2 position = uvec2(vertexData[dataIndex * 2], vertexData[dataIndex * 2 + 1]);
    vec3 chunkPosition = getChunkPosition(gl_DrawID);
    float offsetX = int((position.x >> 5) & 0x1fu) + chunkPosition.x * 32;
    float offsetY = int(position.x & 0x1fu) + chunkPosition.y * 32;
    float offsetZ = int((position.x >> 10) & 0x1fu) + chunkPosition.z * 32;
    int face = int((position.x >> 15) & 0x7u);
    int width = int((position.x >> 18) & 0x1fu) + 1;
    int height = int((position.x >> 23) & 0x1fu) + 1;
    int texture = int(position.y);

    if (face == 0) {
        gl_Position = proj * view * vec4(x * width + offsetX, y * height + offsetY, offsetZ + 1, 1.0);
    } else if (face == 1) {
        gl_Position = proj * view * vec4((1 - x) * width + offsetX, y * height + offsetY, offsetZ, 1.0);
    } else if (face == 2) {
        gl_Position = proj * view * vec4(offsetX, y * height + offsetY, x * width + offsetZ, 1.0);
    } else if (face == 3) {
        gl_Position = proj * view * vec4(offsetX + 1, y * height + offsetY, (1 - x) * width + offsetZ, 1.0);
    } else if (face == 4) {
        gl_Position = proj * view * vec4(x * width + offsetX, offsetY + 1, (1 - y) * height + offsetZ, 1.0);
    } else if (face == 5) {
        gl_Position = proj * view * vec4(x * width + offsetX, offsetY, y * height + offsetZ, 1.0);
    }
    textureCoords = vec2(x, y + 1);
    outData = vec4(x * width, y * height, texture, shadow[face]);
}