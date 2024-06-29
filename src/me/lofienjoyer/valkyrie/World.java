package me.lofienjoyer.valkyrie;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class World {

    public static final int CHUNK_SIDE = 32;

    private final Map<Vector3i, Chunk> chunks;
    private final FastNoiseLite noise;

    public World() {
        this.chunks = new HashMap<>();
        var random = new SplittableRandom();
        this.noise = new FastNoiseLite(random.nextInt());
        noise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        noise.SetFrequency(1 / 512f);
    }

    public void loadChunk(int chunkX, int chunkY, int chunkZ) {
        var chunk = chunks.computeIfAbsent(new Vector3i(chunkX, chunkY, chunkZ), position -> new Chunk(position, this));
        Valkyrie.executorService.submit(() -> {
            byte[] chunkData = new byte[32 * 32 * 32];
            chunk.setData(chunkData);
            var random = new SplittableRandom();
            var heightMap = getOctaves(chunkX * 32, chunkZ * 32);
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    var height = heightMap[x | z << 5];
                    height *= 192;
                    height -= (chunkY * 32) - 64;
                    var localMax = Math.min(height, 32);
                    for (int y = 0; y < localMax; y++) {
                        if (chunkY * 32 + y < random.nextInt(5) + 1) {
                            chunk.setBlock(x, y, z, 5);
                        } else if (y > height - 3) {
                            chunk.setBlock(x, y, z, 2);
                        } else {
                            chunk.setBlock(x, y, z, 1);
                        }
                    }

                    if (localMax > 0 && random.nextInt(60) == 0 && localMax + 7 < 32 && x < 31 && x > 1 && z < 31 && z > 1) {
                        for (int i = 0; i < 4; i++) {
                            chunk.setBlock(x, (int) (localMax + i + 1), z, 4);
                        }

                        for (int i = -2; i <= 2; i++) {
                            for (int j = -2; j <= 2; j++) {
                                for (int k = 0; k < 5; k++) {
                                    if (random.nextInt(1 + k * 2) == 0)
                                        chunk.setBlock(x + i, (int) (localMax + 3 + k), z + j, 3);
                                }
                            }
                        }
                    }
                }
            }
            chunk.setData(chunkData);
            chunk.setDirty(true);

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        if (i == 0 && j == 0 && k == 0) {
                            continue;
                        }

                        var neighbor = getChunk(i + chunkX, j + chunkY, k + chunkZ);
                        if (neighbor != null) {
                            neighbor.setDirty(true);
                        }
                    }

                }
            }
        });
    }

    public Vector3f rayCast(Vector3f position, Vector3f direction, float distance, boolean isPlace) {
        float xPos = (float) Math.floor(position.x);
        float yPos = (float) Math.floor(position.y);
        float zPos = (float) Math.floor(position.z);

        if (direction.length() == 0)
            return null;

        direction = direction.normalize();

        int stepX = ValkyrieMath.signum(direction.x);
        int stepY = ValkyrieMath.signum(direction.y);
        int stepZ = ValkyrieMath.signum(direction.z);
        Vector3f tMax = new Vector3f(ValkyrieMath.intbound(position.x, direction.x), ValkyrieMath.intbound(position.y, direction.y), ValkyrieMath.intbound(position.z, direction.z));
        Vector3f tDelta = new Vector3f((float)stepX / direction.x, (float)stepY / direction.y, (float)stepZ / direction.z);
        float faceX = 0;
        float faceY = 0;
        float faceZ = 0;

        do {
            var block = getBlock((int)xPos, (int)yPos, (int)zPos);
            if (block != 0) {
                if (!isPlace) {
                    return new Vector3f(xPos, yPos, zPos);
                } else {
                    return new Vector3f((int)(xPos + faceX), (int)(yPos + faceY), (int)(zPos + faceZ));
                }
            }
            if (tMax.x < tMax.y) {
                if (tMax.x < tMax.z) {
                    if (tMax.x > distance) break;

                    xPos += stepX;
                    tMax.x += tDelta.x;

                    faceX = -stepX;
                    faceY = 0;
                    faceZ = 0;
                } else {
                    if (tMax.z > distance) break;
                    zPos += stepZ;
                    tMax.z += tDelta.z;
                    faceX = 0;
                    faceY = 0;
                    faceZ = -stepZ;
                }
            } else {
                if (tMax.y < tMax.z) {
                    if (tMax.y > distance) break;
                    yPos += stepY;
                    tMax.y += tDelta.y;
                    faceX = 0;
                    faceY = -stepY;
                    faceZ = 0;
                } else {
                    if (tMax.z > distance) break;
                    zPos += stepZ;
                    tMax.z += tDelta.z;
                    faceX = 0;
                    faceY = 0;
                    faceZ = -stepZ;
                }
            }
        } while (true);

        return null;
    }

    public int getBlock(int x, int y, int z) {
        Vector3i position = getChunkPositionAt(x, y, z);
        if (!chunks.containsKey(position))
            return 0;

        return chunks.get(position)
                .getBlock(Math.abs(x - position.x * CHUNK_SIDE), Math.abs(y - position.y * CHUNK_SIDE), Math.abs(z - position.z * CHUNK_SIDE));
    }

    public int getBlock(Vector3f position) {
        return getBlock((int)position.x, (int)position.y, (int)position.z);
    }

    private Vector3i getChunkPositionAt(int x, int y, int z) {
        return new Vector3i((int)Math.floor(x / (float)CHUNK_SIDE), (int)Math.floor(y / (float)CHUNK_SIDE), (int)Math.floor(z / (float)CHUNK_SIDE));
    }

    public void setBlock(int voxel, int x, int y, int z) {
        Vector3i position = getChunkPositionAt(x, y, z);
        var chunk = chunks.get(position);
        if (chunk == null || chunk.getData() == null) {
            return;
        }

        var blockX = Math.abs(x - position.x * CHUNK_SIDE);
        var blockY = Math.abs(y - position.y * CHUNK_SIDE);
        var blockZ = Math.abs(z - position.z * CHUNK_SIDE);
        chunk.setBlock(blockX, blockY, blockZ, voxel);
        chunk.setDirty(true);
        if (blockX == 0) {
            var neighbor = getChunk(position.x - 1, position.y, position.z);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockX == CHUNK_SIDE - 1) {
            var neighbor = getChunk(position.x + 1, position.y, position.z);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockY == 0) {
            var neighbor = getChunk(position.x, position.y - 1, position.z);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockY == CHUNK_SIDE - 1) {
            var neighbor = getChunk(position.x, position.y + 1, position.z);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockZ == 0) {
            var neighbor = getChunk(position.x, position.y, position.z - 1);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockZ == CHUNK_SIDE - 1) {
            var neighbor = getChunk(position.x, position.y, position.z + 1);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
    }

    public void setBlock(int voxel, Vector3f position) {
        setBlock(voxel, (int)position.x, (int)position.y, (int)position.z);
    }

    private float[] getOctaves(int chunkX, int chunkZ) {
        float[] octaves = new float[32 * 32];

        float frequency = 1;
        float amplitude = 1;
        float amplitudeSum = 0;
        for (int i = 0; i < 4; i++) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    octaves[x | z << 5] += ((noise.GetNoise((x + chunkX) * frequency, (z + chunkZ) * frequency) + 1) / 2f) * amplitude;
                }
            }
            amplitudeSum += amplitude;
            amplitude *= 0.25f;
            frequency *= 3.5f;
        }

        for (int i = 0; i < octaves.length; i++) {
            octaves[i] /= amplitudeSum;
        }

        return octaves;
    }

    public void unloadChunk(int chunkX, int chunkY, int chunkZ) {
        chunks.remove(new Vector3i(chunkX, chunkY, chunkZ));
    }

    public Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return chunks.get(new Vector3i(chunkX, chunkY, chunkZ));
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

}
