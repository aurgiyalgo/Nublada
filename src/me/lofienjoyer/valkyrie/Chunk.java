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
        return (data[x | y << 5 | z << 12] >> 13) & 0x7;
    }

    public void setRedLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0x1fff) | (light << 13));
    }

    public int getGreenLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 10) & 0x7;
    }

    public void setGreenLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0xe3ff) | (light << 10));
    }

    public int getBlueLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 7) & 0x7;
    }

    public void setBlueLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0xfc7f) | (light << 7));
    }

    public int getSunLight(int x, int y, int z) {
        if (data == null) return 0;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return (data[x | y << 5 | z << 12] >> 4) & 0x7;
    }

    public void setSunLight(int x, int y, int z, int light) {
        if (data == null) return;
        if (y < 0 || y > 127 || x < 0 || x > 31 || z < 0 || z > 31) return;
        data[x | y << 5 | z << 12] = (short) ((data[x | y << 5 | z << 12] & 0xff8f) | (light << 4));
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
