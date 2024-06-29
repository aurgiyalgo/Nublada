package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class VboAllocator implements GpuAllocator {

    private final int vbo;
    private final int auxVbo;
    private int firstFreePosition;
    private final List<MeshInstance> instances;

    public VboAllocator(int vao, int sizeInMiB) {
        glBindVertexArray(vao);
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, sizeInMiB * 1024 * 1024 * Integer.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 2, GL_UNSIGNED_INT, 0, 0);
        glVertexBindingDivisor(1, 1);

        this.auxVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, auxVbo);

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
        var size = firstFreePosition - (instanceToRemove.getIndex() + instanceToRemove.getLength() * Integer.BYTES * 2);
        glBindBuffer(GL_ARRAY_BUFFER, auxVbo);
        glBufferData(GL_ARRAY_BUFFER, size, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_COPY_READ_BUFFER, vbo);
        glBindBuffer(GL_COPY_WRITE_BUFFER, auxVbo);
        glCopyBufferSubData(
                GL_COPY_READ_BUFFER,
                GL_COPY_WRITE_BUFFER,
                instanceToRemove.getIndex() + instanceToRemove.getLength() * Integer.BYTES * 2,
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

            mesh.setIndex(mesh.getIndex() - instanceToRemove.getLength() * Integer.BYTES * 2);
        });

        firstFreePosition -= instanceToRemove.getLength() * Integer.BYTES * 2;
    }

    @Override
    public void update(MeshInstance instance, int[] data) {
        delete(instance);
        instance.setLength(data.length / 2);
        instance.setIndex(firstFreePosition);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, firstFreePosition, data);
        firstFreePosition += data.length * Integer.BYTES;
        instances.add(instance);
    }

    public int getFirstFreePosition() {
        return firstFreePosition;
    }

}
