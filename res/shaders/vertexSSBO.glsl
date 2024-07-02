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
    return vec3(positionData[index * 3], positionData[index * 3 + 1], positionData[index * 3 + 2]);
}

void main()
{
    int x = int(data >> 5);
    int y = int(data & 0x1fu) - 1;
    int dataIndex = gl_InstanceID + gl_BaseInstance;
    uvec2 position = uvec2(vertexData[dataIndex * 2], vertexData[dataIndex * 2 + 1]);
    vec3 chunkPosition = getChunkPosition(gl_DrawID);
    float offsetX = int((position.x >> 6) & 0x3fu) + chunkPosition.x * 32 - 1;
    float offsetY = int(position.x & 0x3fu) + chunkPosition.y * 32 - 1;
    float offsetZ = int((position.x >> 12) & 0x3fu) + chunkPosition.z * 32 - 1;
    int face = int((position.x >> 18) & 0x7u);
    int width = int((position.x >> 21) & 0x1fu) + 1;
    int height = int((position.x >> 26) & 0x1fu) + 1;
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