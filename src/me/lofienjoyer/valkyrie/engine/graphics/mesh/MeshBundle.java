package me.lofienjoyer.valkyrie.engine.graphics.mesh;

import me.lofienjoyer.valkyrie.Valkyrie;
import me.lofienjoyer.valkyrie.engine.world.Chunk;
import me.lofienjoyer.valkyrie.engine.world.ChunkPreMeshData;

import static me.lofienjoyer.valkyrie.engine.world.World.*;

/**
 * Contains the solid and transparent meshes of a chunk
 */
public class MeshBundle {

    private final Chunk chunk;

//    private final Mesh[] solidMesh;
    private final Mesh[] transparentMesh;

//    private final Mesher[] greedyMesher;
    private final Mesher[] dynamicMesher;

    private boolean loaded = false;

    public MeshBundle(Chunk chunk) {
        this.chunk = chunk;

//        this.solidMesh = new Mesh[CHUNK_HEIGHT / CHUNK_SECTION_HEIGHT];
//        for (int i = 0; i < solidMesh.length; i++) {
//            solidMesh[i] = Valkyrie.LOADER.allocateMesh();
//        }

        this.transparentMesh = new Mesh[CHUNK_HEIGHT / CHUNK_SECTION_HEIGHT];
        for (int i = 0; i < transparentMesh.length; i++) {
            transparentMesh[i] = Valkyrie.LOADER.allocateMesh();
        }

//        this.greedyMesher = new Mesher[CHUNK_HEIGHT / CHUNK_SECTION_HEIGHT];
        this.dynamicMesher = new Mesher[CHUNK_HEIGHT / CHUNK_SECTION_HEIGHT];
    }

    public void compute(int section) {
        ChunkPreMeshData chunkPreMeshData = new ChunkPreMeshData(chunk);

//        greedyMesher[section] = new GreedyMesher(chunkPreMeshData, section).compute();
        dynamicMesher[section] = new DynamicMesher(chunkPreMeshData, section).compute();
    }

    public boolean loadMeshToGpu(int section) {
        if (dynamicMesher[section] == null)
            return false;

//        greedyMesher[section].loadToGpu(solidMesh[section]);
        dynamicMesher[section].loadToGpu(transparentMesh[section]);

//        greedyMesher[section] = null;
        dynamicMesher[section] = null;
        setLoaded(true);

        if (transparentMesh[section].getVertexCount() == 0)
            return false;

        return true;
    }

    public Mesh getSolidMeshes(int section) {
        return null;
    }

    public Mesh getTransparentMeshes(int section) {
        return transparentMesh[section];
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    private synchronized void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

}
