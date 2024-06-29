package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private byte[] data;
    private MeshInstance mesh;
    private Future<List<Integer>> meshFuture;
    private final Vector3i position;
    private boolean dirty;

    public Chunk(Vector3i position) {
        this.position = position;
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

    public Future<List<Integer>> getMeshFuture() {
        return meshFuture;
    }

    public void setMeshFuture(Future<List<Integer>> meshFuture) {
        this.meshFuture = meshFuture;
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

}
