package me.lofienjoyer.valkyrie;

import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4i;

import java.util.*;

public class World {

    public static final int CHUNK_SIDE = 32;

    private final Map<Vector2i, Chunk> chunks;
    private final FastNoiseLite noise;
    private final FastNoiseLite caveNoise;
    private final FastNoiseLite caveNoise2;
    private final Queue<LightNode> redLightNodes;
    private final Queue<LightNode> greenLightNodes;
    private final Queue<LightNode> blueLightNodes;
    private final Queue<LightNode> sunLightNodes;
    private final Queue<LightRemovalNode> redLightRemovalNodes;
    private final Queue<LightRemovalNode> greenLightRemovalNodes;
    private final Queue<LightRemovalNode> blueLightRemovalNodes;
    private final Queue<LightRemovalNode> sunLightRemovalNodes;
    private final Queue<Vector4i> blocksToPlace;
    private final Queue<Vector4i> lightsToPlace;
    private final Set<Chunk> chunksToUpdateSunLight;

    public World() {
        this.chunks = new HashMap<>();
        var random = new SplittableRandom(System.nanoTime());
        this.noise = new FastNoiseLite(random.nextInt());
        noise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        noise.SetFrequency(1 / 128f);
        this.caveNoise = new FastNoiseLite(random.nextInt());
        caveNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        caveNoise.SetFrequency(1 / 64f);
        this.caveNoise2 = new FastNoiseLite(random.nextInt());
        caveNoise2.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        caveNoise2.SetFrequency(1 / 64f);
        this.redLightNodes = new ArrayDeque<>();
        this.greenLightNodes = new ArrayDeque<>();
        this.blueLightNodes = new ArrayDeque<>();
        this.sunLightNodes = new ArrayDeque<>();
        this.redLightRemovalNodes = new ArrayDeque<>();
        this.greenLightRemovalNodes = new ArrayDeque<>();
        this.blueLightRemovalNodes = new ArrayDeque<>();
        this.sunLightRemovalNodes = new ArrayDeque<>();
        this.blocksToPlace = new ArrayDeque<>();
        this.lightsToPlace = new ArrayDeque<>();
        this.chunksToUpdateSunLight = new HashSet<>();

        BlockManager.init();
    }

    public void update(Camera camera) {
        while (!blocksToPlace.isEmpty()) {
            var data = blocksToPlace.poll();
            setBlockInternal(data.x, data.y, data.z, data.w);
        }

        while (!lightsToPlace.isEmpty()) {
            var data = lightsToPlace.poll();
            setLightInternal(data.x, data.y, data.z, data.w);
        }

        chunksToUpdateSunLight.forEach(this::propagateSunLight);
        chunksToUpdateSunLight.clear();

        LightManager.removeRed(this, redLightRemovalNodes, redLightNodes);
        LightManager.removeGreen(this, greenLightRemovalNodes, greenLightNodes);
        LightManager.removeBlue(this, blueLightRemovalNodes, blueLightNodes);
        LightManager.removeSun(this, sunLightRemovalNodes, sunLightNodes);

        LightManager.propagateRed(this, redLightNodes);
        LightManager.propagateGreen(this, greenLightNodes);
        LightManager.propagateBlue(this, blueLightNodes);
        LightManager.propagateSun(this, sunLightNodes);

        final var worldSide = 12;
        var cameraX = (int)Math.floor(camera.getPosition().x / 32);
        var cameraZ = (int)Math.floor(camera.getPosition().z / 32);
        var meshesToDelete = new ArrayList<Long>();
        getChunks().stream().toList().forEach(chunk -> {
            var position = chunk.getPosition();
            long encodedPosition = (position.x) | (long) (0) << 16 | (long) (position.y) << 32;
            if (Math.abs(position.x - cameraX) > worldSide/2 + 2 || Math.abs(position.y - cameraZ) > worldSide/2 + 2) {
                unloadChunk(position.x, position.y);
                meshesToDelete.add(encodedPosition);
                return;
            }

            if (chunk.isDirty()) {
                Valkyrie.executorService.submit(() -> {
                    try {
                        var computedMesh = WorldScene.integerListToArray(new GreedyMesher(chunk, chunk.getWorld()).compute());
                        synchronized (Valkyrie.lock) {
                            WorldScene.meshesToUpdate.add(new MeshToUpdate(computedMesh, encodedPosition));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
                chunk.setDirty(false);
            }
        });

        var chunksToRender = new ArrayList<Chunk>();
        for (int x = -worldSide/2; x <= worldSide/2; x++) {
            for (int z = -worldSide/2; z <= worldSide/2; z++) {
                var chunkX = cameraX - x;
                var chunkZ = cameraZ - z;
                var chunk = getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    loadChunk(chunkX, chunkZ);
                    continue;
                }

                chunksToRender.add(chunk);
            }
        }

        synchronized (Valkyrie.lock) {
            WorldScene.meshesToDelete.addAll(meshesToDelete);
            WorldScene.chunksToRender = chunksToRender;
        }
    }

    public void loadChunk(int chunkX, int chunkZ) {
        var chunk = chunks.computeIfAbsent(new Vector2i(chunkX, chunkZ), position -> new Chunk(position, this));
        int[] chunkData = new int[32 * 128 * 32];
        chunk.setData(chunkData);
        var random = new SplittableRandom();
        var heightMap = getOctaves(chunkX * 32, chunkZ * 32);
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                var height = heightMap[x | z << 5] * 90 + 30;
                for (int y = 0; y < height; y++) {
                    if (y > height - 3) {
                        chunk.setBlock(x, y, z, 2);
                    } else {
                        chunk.setBlock(x, y, z, 1);
                    }

                    var cave1 = caveNoise.GetNoise((chunkX * 32 + x), y, (chunkZ * 32 + z));
                    var cave2 = caveNoise2.GetNoise((chunkX * 32 + x), y * 2, (chunkZ * 32 + z));
                    if (Math.abs(cave1) + Math.abs(cave2) < 0.15)
                        chunk.setBlock(x, y, z, 0);
                }

                for (int y = 0; y < height; y++) {
                    if (y < random.nextInt(5) + 1) {
                        chunk.setBlock(x, y, z, 5);
                    }
                }

                if (height > 0 && chunk.getBlock(x, (int) height, z) != 0 && noise.GetNoise((chunkX * 32 + x) * 512, (chunkZ * 32 + z) * 512) > 0.95 && height + 7 < 128 && x < 30 && x > 2 && z < 30 && z > 2) {
                    if (random.nextInt(100) == 0) {
                        for (int i = -3; i <= 3; i++) {
                            for (int j = -3; j <= 3; j++) {
                                for (int k = 0; k < 13; k++) {
                                    if (Math.sqrt(i * i + j * j) <= (3 - k * 0.2))
                                        chunk.setBlock(x + i, (int) (height + 3 + k), z + j, 7);
                                }
                            }
                        }

                        for (int i = 0; i < 8; i++) {
                            chunk.setBlock(x, (int) (height + i + 1), z, 4);
                        }
                    } else {
                        var leafType = random.nextInt(15) != 0 ? 3 : 8;

                        for (int i = -2; i <= 2; i++) {
                            for (int j = -2; j <= 2; j++) {
                                for (int k = 0; k < 5; k++) {
                                    if (random.nextFloat(1 + k * 2) < 1.3)
                                        chunk.setBlock(x + i, (int) (height + 3 + k), z + j, leafType);
                                }
                            }
                        }

                        for (int i = 0; i < 4; i++) {
                            chunk.setBlock(x, (int) (height + i + 1), z, 4);
                        }
                    }
                }
            }
        }
        chunk.setData(chunkData);
        chunk.setDirty(true);

        for (int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                if (i == 0 && k == 0) {
                    continue;
                }

                var neighbor = getChunk(i + chunkX, k + chunkZ);
                if (neighbor != null) {
                    neighbor.setDirty(true);
                    chunksToUpdateSunLight.add(neighbor);
                }
            }
        }

        chunksToUpdateSunLight.add(chunk);
    }

    private void propagateSunLight(Chunk chunk) {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                for (int y = 127; y >= 0; y--) {
                    chunk.setSunLight(x, y , z, 0);
                }
            }
        }
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                for (int y = 127; y >= 0; y--) {
                    chunk.getBlock(x, y, z);
                    if (chunk.getBlock(x, y ,z) == 0) {
                        chunk.setSunLight(x, y , z, 7);
                        sunLightNodes.add(new LightNode(x | y << 5 | z << 12, chunk));
                    } else {
                        break;
                    }
                }
            }
        }
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
        var position = getChunkPositionAt(x, y, z);
        if (!chunks.containsKey(position))
            return 0;

        return chunks.get(position)
                .getBlock(Math.abs(x - position.x * CHUNK_SIDE), y, Math.abs(z - position.y * CHUNK_SIDE));
    }

    public int getBlock(Vector3f position) {
        return getBlock((int)position.x, (int)position.y, (int)position.z);
    }

    private Vector2i getChunkPositionAt(int x, int y, int z) {
        return new Vector2i((int)Math.floor(x / (float)CHUNK_SIDE), (int)Math.floor(z / (float)CHUNK_SIDE));
    }

    private void setBlockInternal(int voxel, int x, int y, int z) {
        var position = getChunkPositionAt(x, y, z);
        var chunk = chunks.get(position);
        if (chunk == null || chunk.getData() == null) {
            return;
        }

        var blockX = Math.abs(x - position.x * CHUNK_SIDE);
        var blockZ = Math.abs(z - position.y * CHUNK_SIDE);
        int index = blockX | y << 5 | blockZ << 12;
        redLightRemovalNodes.add(new LightRemovalNode(index, chunk.getRedLight(blockX, y, blockZ), chunk));
        greenLightRemovalNodes.add(new LightRemovalNode(index, chunk.getGreenLight(blockX, y, blockZ), chunk));
        blueLightRemovalNodes.add(new LightRemovalNode(index, chunk.getBlueLight(blockX, y, blockZ), chunk));
        sunLightRemovalNodes.add(new LightRemovalNode(index, chunk.getBlueLight(blockX, y, blockZ), chunk));
        chunk.setBlock(blockX, y, blockZ, voxel);
        chunk.setDirty(true);
        updateBlockNeighbors(position, blockX, blockZ);
        chunksToUpdateSunLight.add(chunk);
    }

    public void setBlock(int voxel, int x, int y, int z) {
        blocksToPlace.add(new Vector4i(voxel, x, y, z));
    }

    public void setBlock(int voxel, Vector3f position) {
        setBlock(voxel, (int)position.x, (int)position.y, (int)position.z);

        var blockType = BlockManager.getVoxelById(voxel);
        setLight(blockType.light, (int)position.x, (int)position.y, (int)position.z);
    }

    private void setLightInternal(int light, int x, int y, int z) {
        var position = getChunkPositionAt(x, y, z);
        var chunk = chunks.get(position);
        if (chunk == null || chunk.getData() == null) {
            return;
        }

        var blockX = Math.abs(x - position.x * CHUNK_SIDE);
        var blockZ = Math.abs(z - position.y * CHUNK_SIDE);
        chunk.setRedLight(blockX, y, blockZ, (light >> 8) & 0x7);
        chunk.setGreenLight(blockX, y, blockZ, (light >> 4) & 0x7);
        chunk.setBlueLight(blockX, y, blockZ, (light) & 0x7);
        int index = blockX | y << 5 | blockZ << 12;
        redLightNodes.add(new LightNode(index, chunk));
        greenLightNodes.add(new LightNode(index, chunk));
        blueLightNodes.add(new LightNode(index, chunk));
        chunk.setDirty(true);
        updateBlockNeighbors(position, blockX, blockZ);
    }

    public void setLight(int light, int x, int y, int z) {
        lightsToPlace.add(new Vector4i(light, x, y, z));
    }

    public void setLight(int light, Vector3f position) {
        setLight(light, (int)position.x, (int)position.y, (int)position.z);
    }

    private void updateBlockNeighbors(Vector2i chunkPos, int blockX, int blockZ) {
        if (blockX == 0) {
            var neighbor = getChunk(chunkPos.x - 1, chunkPos.y);
            if (neighbor != null)
                neighbor.setDirty(true);
        }
        if (blockX == CHUNK_SIDE - 1) {
            var neighbor = getChunk(chunkPos.x + 1, chunkPos.y);
            if (neighbor != null) {
                neighbor.setDirty(true);
            }
        }
        if (blockZ == 0) {
            var neighbor = getChunk(chunkPos.x, chunkPos.y - 1);
            if (neighbor != null) {
                neighbor.setDirty(true);
            }
        }
        if (blockZ == CHUNK_SIDE - 1) {
            var neighbor = getChunk(chunkPos.x, chunkPos.y + 1);
            if (neighbor != null) {
                neighbor.setDirty(true);
            }
        }
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

    public void unloadChunk(int chunkX, int chunkZ) {
        chunks.remove(new Vector2i(chunkX, chunkZ));
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(new Vector2i(chunkX, chunkZ));
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

}
