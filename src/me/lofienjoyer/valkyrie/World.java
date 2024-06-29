package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.*;

public class World {

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
