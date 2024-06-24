package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.Future;

public class Chunk {

    private byte[] data;
    private MeshInstance mesh;
    private Future<List<Integer>> meshFuture;
    private Vector3i position;

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

    public void setPosition(Vector3i position) {
        this.position = position;
    }

}
