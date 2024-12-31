package me.lofienjoyer.valkyrie;

import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private int[] data;
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
        return data[x | y << 5 | z << 12] >> 12;
    }

    public void setBlock(int x, int y, int z, int id) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (id << 12);
    }

    public int getRedLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 9) & 0x7;
    }

    public void setRedLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = ((data[x | y << 5 | z << 12] & 0xf1ff) | (light << 9));
    }

    public int getGreenLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 6) & 0x7;
    }

    public void setGreenLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = ((data[x | y << 5 | z << 12] & 0xfe3f) | (light << 6));
    }

    public int getBlueLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 3) & 0x7;
    }

    public void setBlueLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = ((data[x | y << 5 | z << 12] & 0xffc7) | (light << 3));
    }

    public int getSunLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return data[x | y << 5 | z << 12] & 0x7;
    }

    public void setSunLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = ((data[x | y << 5 | z << 12] & 0xfff8) | light);
    }

    public int[] getData() {
        return data;
    }

    public void setData(int[] data) {
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
