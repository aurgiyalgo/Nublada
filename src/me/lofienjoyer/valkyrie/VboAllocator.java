package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class VboAllocator implements GpuAllocator {

    private final int vbo;
    private final int auxVbo;
    private int firstFreePosition;
    private List<MeshInstance> instances;

    public VboAllocator(int vao) {
        glBindVertexArray(vao);
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 64 * 1024 * 1024 * Integer.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 2, GL_UNSIGNED_INT, 0, 0);
        glVertexBindingDivisor(1, 1);

        this.auxVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, auxVbo);
        glBufferData(GL_ARRAY_BUFFER, 2048 * Integer.BYTES, GL_DYNAMIC_DRAW);

        this.instances = new ArrayList<>();
    }

    public MeshInstance store(int[] data) {
        var instance = new MeshInstance(data.length / 2, firstFreePosition);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, firstFreePosition, data);
        firstFreePosition += data.length * Integer.BYTES;
        instances.add(instance);
        return instance;
    }

    public void delete(MeshInstance instanceToRemove) {
        instances.remove(instanceToRemove);
        var lastInstance = instances.getLast();
        glBindBuffer(GL_COPY_READ_BUFFER, vbo);
        glBindBuffer(GL_COPY_WRITE_BUFFER, auxVbo);
        var size = lastInstance.getIndex() - instanceToRemove.getIndex();
        glCopyBufferSubData(
                GL_COPY_READ_BUFFER,
                GL_COPY_WRITE_BUFFER,
                instanceToRemove.getIndex() + instanceToRemove.getLength() * Integer.BYTES,
                0,
                size
        );

        glBindBuffer(GL_COPY_READ_BUFFER, auxVbo);
        glBindBuffer(GL_COPY_WRITE_BUFFER, vbo);
        glCopyBufferSubData(
                GL_COPY_READ_BUFFER,
                GL_COPY_WRITE_BUFFER,
                0,
                instanceToRemove.getIndex(),
                size
        );

        instances.forEach(mesh -> {
            if (mesh.getIndex() < instanceToRemove.getIndex())
                return;

            mesh.setIndex(mesh.getIndex() - instanceToRemove.getLength() * Integer.BYTES);
        });

        firstFreePosition -= instanceToRemove.getLength() * Integer.BYTES;
    }

    public int getFirstFreePosition() {
        return firstFreePosition;
    }

}
