package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private short[] data;
    private MeshInstance mesh;
    private final Vector3i position;
    private boolean dirty;
    private boolean priority;
    private boolean meshLoaded;
    private final World world;
    private final List<Future<List<Integer>>> futures;

    public Chunk(Vector3i position, World world) {
        this.position = position;
        this.world = world;
        this.futures = new ArrayList<>();
    }

    public int getBlock(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return data[x | y << 5 | z << 10] & 0xf;
    }

    public void setBlock(int x, int y, int z, int id) {
        if (data == null) return;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 10] = (short) (id & 0xf);
    }

    public int getRedLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 10] >> 12) & 0xf;
    }

    public void setRedLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 10] = (short) ((data[x | y << 5 | z << 10] & 0x0fff) | (light << 12));
    }

    public int getGreenLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 10] >> 8) & 0xf;
    }

    public void setGreenLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 10] = (short) ((data[x | y << 5 | z << 10] & 0xf0ff) | (light << 8));
    }

    public int getBlueLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 10] >> 4) & 0xf;
    }

    public void setBlueLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 10] = (short) ((data[x | y << 5 | z << 10] & 0xff0f) | (light << 4));
    }

    public MeshInstance getMesh() {
        return mesh;
    }

    public void setMesh(MeshInstance mesh) {
        this.mesh = mesh;
    }

    public short[] getData() {
        return data;
    }

    public void setData(short[] data) {
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

    public List<Future<List<Integer>>> getFutures() {
        return futures;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

}
