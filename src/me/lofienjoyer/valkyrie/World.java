package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {

    private final Map<Vector3i, Chunk> chunks;
    private final FastNoiseLite noise;

    public World() {
        this.chunks = new HashMap<>();
        this.noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        noise.SetFrequency(1 / 256f);
    }

    public void loadChunk(int chunkX, int chunkY, int chunkZ) {
        var chunk = chunks.computeIfAbsent(new Vector3i(chunkX, chunkY, chunkZ), Chunk::new);
        chunk.setMeshFuture(Valkyrie.executorService.submit(() -> {
            byte[] chunkData = new byte[32 * 32 * 32];
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    var height = (noise.GetNoise(x + chunkX * 32, z + chunkZ * 32) + 1) / 2f;
                    height *= 224;
                    height -= (chunkY * 32) - 32;
                    for (int y = 0; y < Math.min(height, 32); y++) {
                        chunkData[x | y << 5 | z << 10] = 1;
                    }
                }
            }
            chunk.setData(chunkData);
            return new GreedyMesher(chunkData).compute();
        }));
    }

    public Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return chunks.get(new Vector3i(chunkX, chunkY, chunkZ));
    }

    public List<Chunk> getChunks() {
        return chunks.values().stream().toList();
    }

}
