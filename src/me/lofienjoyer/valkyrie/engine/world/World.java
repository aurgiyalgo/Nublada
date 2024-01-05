package me.lofienjoyer.valkyrie.engine.world;

import me.lofienjoyer.valkyrie.Valkyrie;
import me.lofienjoyer.valkyrie.engine.config.Config;
import me.lofienjoyer.valkyrie.engine.events.world.ChunkLoadEvent;
import me.lofienjoyer.valkyrie.engine.events.world.ChunkUnloadEvent;
import me.lofienjoyer.valkyrie.engine.events.world.ChunkUpdateEvent;
import me.lofienjoyer.valkyrie.engine.graphics.camera.Camera;
import me.lofienjoyer.valkyrie.engine.utils.Maths;
import me.lofienjoyer.valkyrie.engine.utils.PerlinNoise;
import me.lofienjoyer.valkyrie.engine.world.populator.CastlePopulator;
import me.lofienjoyer.valkyrie.engine.world.populator.Populator;
import me.lofienjoyer.valkyrie.engine.world.populator.TerrainPopulator;
import me.lofienjoyer.valkyrie.engine.world.populator.TreePopulator;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Handles the render and data for a world
 */
public class World {

    public static final int CHUNK_WIDTH = 32;
    public static final int CHUNK_HEIGHT = 256;
    public static final int CHUNK_SECTION_HEIGHT = 32;
    public static int LOAD_DISTANCE = 5;
    public static int FULLY_LOAD_DISTANCE = 1;

    private final List<Populator> populators;

    private final Map<Vector2i, Chunk> chunks;
    private final Map<Vector2i, FutureChunk> futureChunks;

    private final Vector2i playerPosition;

    private final PerlinNoise noise;
    private double seed;

    private final List<Chunk> chunksToGenerate;

    public World() {
        this.chunks = new HashMap<>();
        this.futureChunks = new HashMap<>();
        this.chunksToGenerate = new ArrayList<>();
        this.playerPosition = new Vector2i();

        Valkyrie.EVENT_HANDLER.registerListener(ChunkUpdateEvent.class, this::handleChunkUpdate);

        var config = Config.getInstance();
        LOAD_DISTANCE = config.get("load_distance", Integer.class);
        FULLY_LOAD_DISTANCE = config.get("fully_load_distance", Integer.class);

        // TODO: 22/9/22 Temporary code (replace with proper world loading)
        this.seed = new Random().nextGaussian() * 255;

//        Yaml yaml = new Yaml();
//        File worldFolder = new File("world");
//        Map<String, Object> data = new HashMap<>();
//        try {
//            if (!worldFolder.exists()) {
//                worldFolder.mkdir();
//                this.seed = new Random().nextGaussian() * 255;
//                data.put("seed", seed);
//
//                FileWriter writer = new FileWriter("world/world.yml");
//                yaml.dump(data, writer);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            data = yaml.load(new FileInputStream("world/world.yml"));
//            this.seed = (double) data.get("seed");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        this.noise = new PerlinNoise(seed, 900);

        this.populators = new ArrayList<>();
        populators.add(new TerrainPopulator(noise));
        populators.add(new TreePopulator(noise));
        populators.add(new CastlePopulator(noise));

        Valkyrie.LOG.info("World generation seed set to " + noise.getSeed());
    }

    public synchronized void update(float delta, Camera camera) {
//        checkGeneratingChunks();
        generateChunkIfPending();

        int playerX = (int) Math.floor(camera.getPosition().x / (float) World.CHUNK_WIDTH);
        int playerZ = (int) Math.floor(camera.getPosition().z / (float) World.CHUNK_WIDTH);

        if (playerPosition.x != playerX || playerPosition.y != playerZ) {
            var iterator = chunks.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var position = entry.getKey();
                if (position.x < playerX - LOAD_DISTANCE - 2 ||
                        position.x > playerX + LOAD_DISTANCE + 2 ||
                        position.y < playerZ - LOAD_DISTANCE - 2 ||
                        position.y > playerZ + LOAD_DISTANCE + 2
                ) {
                    iterator.remove();
                    entry.getValue().onDestroy();
                    Valkyrie.EVENT_HANDLER.process(new ChunkUnloadEvent(entry.getValue()));
                }
            }
        }

        playerPosition.x = playerX;
        playerPosition.y = playerZ;

        for (int x = -LOAD_DISTANCE; x <= LOAD_DISTANCE; x++) {
            for (int z = -LOAD_DISTANCE; z <= LOAD_DISTANCE; z++) {
                int chunkX = playerX + x;
                int chunkZ = playerZ + z;

                var chunk = getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    addChunk(chunkX, chunkZ);
                } else {
                    if (chunk.getState() == ChunkState.UNLOADED)
                        continue;

                    if (x < -FULLY_LOAD_DISTANCE || x > FULLY_LOAD_DISTANCE || z < -FULLY_LOAD_DISTANCE || z > FULLY_LOAD_DISTANCE) {
                        setChunkState(chunk, ChunkState.SOFT_LOADED);
                    } else {
                        setChunkState(chunk, ChunkState.FULLY_LOADED);
                    }
                }

            }
        }
    }

    /**
     * Adds a chunk to the world and schedules it for generation
     * @param x Chunk X coordinate
     * @param z Chunk Z coordinate
     */
    public void addChunk(int x, int z) {
        Vector2i position = new Vector2i(x, z);
        if (chunks.containsKey(position))
            throw new RuntimeException("Chunk already loaded");

        Chunk chunk = new Chunk(position, this);

        chunks.put(position, chunk);

        chunksToGenerate.add(chunk);
    }

    /**
     * Queues the chunk to mesh and flags its neighbors as not-updated
     * @param chunk Chunk to mesh
     */
    private void initializeChunk(Chunk chunk) {
        Valkyrie.EVENT_HANDLER.process(new ChunkLoadEvent(chunk));

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                var neighbor = getChunk(i + chunk.getPosition().x, j + chunk.getPosition().y);
                if (neighbor != null) {
                    for (int k = 0; k < 8; k++) {
                        Valkyrie.EVENT_HANDLER.process(new ChunkUpdateEvent(neighbor, new Vector3i(0, k * CHUNK_SECTION_HEIGHT, 0)));
                    }
                }

            }
        }
    }

    private void handleChunkUpdate(ChunkUpdateEvent event) {

    }

    private void generateChunkIfPending() {
        var iterator = chunksToGenerate.iterator();
        if (iterator.hasNext()) {
            var chunk = iterator.next();
            chunk.loadChunk(this);
            loadFutureChunk(chunk);
            initializeChunk(chunk);
            iterator.remove();
        }
    }

    public void saveWorld() {
        for (Chunk chunk : chunks.values()) {
            chunk.saveToFile();
        }
    }

    public Vector3f rayCast(Vector3f position, Vector3f direction, float distance, boolean isPlace) {
        float xPos = (float) Math.floor(position.x);
        float yPos = (float) Math.floor(position.y);
        float zPos = (float) Math.floor(position.z);

        if (direction.length() == 0)
            return null;

        direction = direction.normalize();

        int stepX = Maths.signum(direction.x);
        int stepY = Maths.signum(direction.y);
        int stepZ = Maths.signum(direction.z);
        Vector3f tMax = new Vector3f(Maths.intbound(position.x, direction.x), Maths.intbound(position.y, direction.y), Maths.intbound(position.z, direction.z));
        Vector3f tDelta = new Vector3f((float)stepX / direction.x, (float)stepY / direction.y, (float)stepZ / direction.z);
        float faceX = 0;
        float faceY = 0;
        float faceZ = 0;

        do {
            if (getBlock((int)xPos, (int)yPos, (int)zPos) != 0) {
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

    public synchronized void loadFutureChunk(Chunk chunk) {
        var futureChunk = futureChunks.get(chunk.getPosition());
        if (futureChunk != null)
            futureChunk.setBlocksInChunk(chunk);

        chunk.setState(ChunkState.FULLY_LOADED);
    }

    public Chunk getChunk(int x, int z) {
        return chunks.get(new Vector2i(x, z));
    }

    public int getBlock(float x, float y, float z) {
        return getBlock((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    }

    public int getBlock(int x, int y, int z) {
        Vector2i position = getChunkPositionAt(x, z);
        if (!chunks.containsKey(position)) return 0;
        return chunks.get(position)
                .getBlock(Math.abs(position.x * CHUNK_WIDTH - x), y, Math.abs(position.y * CHUNK_WIDTH - z));
    }

    public int getBlock(Vector3f position) {
        return getBlock((int)position.x, (int)position.y, (int)position.z);
    }

    public synchronized void setBlock(int voxel, int x, int y, int z, boolean updateChunk) {
        Vector2i position = getChunkPositionAt(x, z);
        var chunk = chunks.get(position);
        if (chunk == null || chunk.getState() != ChunkState.FULLY_LOADED) {
            FutureChunk futureChunk = futureChunks.get(position);
            if (futureChunk == null) {
                futureChunk = new FutureChunk(position);
                futureChunks.put(position, futureChunk);
            }
            futureChunk.addBlock((short) voxel, x - position.x * CHUNK_WIDTH, y, z - position.y * CHUNK_WIDTH);
            return;
        }

        chunk.setBlock(voxel, x - position.x * CHUNK_WIDTH, y, z - position.y * CHUNK_WIDTH, updateChunk);
    }

    public void setBlock(int voxel, Vector3f position) {
        setBlock(voxel, (int)position.x, (int)position.y, (int)position.z, true);
    }

    // TODO 13/12/23 Move the data compression/decompression to the generation service
    public synchronized void setChunkState(Chunk chunk, ChunkState chunkState) {
        switch (chunkState){
            case SOFT_LOADED:
                if (chunk.state != ChunkState.SOFT_LOADED) {
                    chunk.compressedData = chunk.compress(chunk.voxels);
                    chunk.voxels = null;
                }
                break;
            case FULLY_LOADED:
                if (chunk.state != ChunkState.FULLY_LOADED) {
                    chunk.voxels = chunk.decompress(chunk.compressedData);
                }
                break;
        }

        chunk.state = chunkState;
    }

    private Vector2i getChunkPositionAt(int x, int z) {
        return new Vector2i((int)Math.floor(x / (float)CHUNK_WIDTH), (int)Math.floor(z / (float)CHUNK_WIDTH));
    }

    public Map<Vector2i, Chunk> getChunks() {
        return chunks;
    }

    public List<Populator> getPopulators() {
        return populators;
    }

}
