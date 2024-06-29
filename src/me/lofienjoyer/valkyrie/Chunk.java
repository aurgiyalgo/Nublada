package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private byte[] data;
    private MeshInstance mesh;
    private final Vector3i position;
    private boolean dirty;
    private boolean meshLoaded;
    private final World world;

    public Chunk(Vector3i position, World world) {
        this.position = position;
        this.world = world;
    }

    public int getBlock(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return data[x | y << 5 | z << 10];
    }

    public void setBlock(int x, int y, int z, int id) {
        if (data == null) return;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 10] = (byte) id;
    }

    public MeshInstance getMesh() {
        return mesh;
    }

    public void setMesh(MeshInstance mesh) {
        this.mesh = mesh;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Vector3i getPosition() {
        return position;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public World getWorld() {
        return world;
    }

    public boolean isMeshLoaded() {
        return meshLoaded;
    }

    public void setMeshLoaded(boolean meshLoaded) {
        this.meshLoaded = meshLoaded;
    }

}
