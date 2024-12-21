package me.lofienjoyer.valkyrie;

import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private short[] data;
    private final Vector2i position;
    private boolean dirty;
    private boolean priority;
    private boolean meshLoaded;
    private final World world;
    private final List<Future<List<Integer>>> futures;

    public Chunk(Vector2i position, World world) {
        this.position = position;
        this.world = world;
        this.futures = new ArrayList<>();
    }

    public int getBlock(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return data[x | y << 5 | z << 12] & 0xf;
    }

    public void setBlock(int x, int y, int z, int id) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) (id & 0xf);
    }

    public int getRedLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 12) & 0xf;
    }

    public void setRedLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0x0fff) | (light << 12));
    }

    public int getGreenLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 8) & 0xf;
    }

    public void setGreenLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0xf0ff) | (light << 8));
    }

    public int getBlueLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 4) & 0xf;
    }

    public void setBlueLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0xff0f) | (light << 4));
    }

    public short[] getData() {
        return data;
    }

    public void setData(short[] data) {
        this.data = data;
    }

    public Vector2i getPosition() {
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

}
