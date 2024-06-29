package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class SsboAllocator implements GpuAllocator {

    private final int buffer;
    private final int auxBuffer;
    private int firstFreePosition;
    private final List<MeshInstance> instances;

    public SsboAllocator(int index, int sizeInMiB) {
        this.buffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, sizeInMiB * 1024 * 1024 * Integer.BYTES, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer);

        this.auxBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, auxBuffer);

        this.instances = new ArrayList<>();
    }

    public MeshInstance store(int[] data) {
        var instance = new MeshInstance(data.length / 2, firstFreePosition);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, firstFreePosition, data);
        firstFreePosition += data.length * Integer.BYTES;
        instances.add(instance);
        return instance;
    }

    public void delete(MeshInstance instanceToRemove) {
        instances.remove(instanceToRemove);
        var lastInstance = instances.getLast();
        var size = lastInstance.getIndex() - instanceToRemove.getIndex() - instanceToRemove.getLength() * Integer.BYTES * 2;
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, auxBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, size, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_COPY_READ_BUFFER, buffer);
        glBindBuffer(GL_COPY_WRITE_BUFFER, auxBuffer);
        glCopyBufferSubData(
                GL_COPY_READ_BUFFER,
                GL_COPY_WRITE_BUFFER,
                instanceToRemove.getIndex() + instanceToRemove.getLength() * Integer.BYTES * 2,
                0,
                size
        );

        glBindBuffer(GL_COPY_READ_BUFFER, auxBuffer);
        glBindBuffer(GL_COPY_WRITE_BUFFER, buffer);
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
        instance.setLength(data.length);
        instance.setIndex(firstFreePosition);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, firstFreePosition, data);
        firstFreePosition += data.length * Integer.BYTES;
        instances.add(instance);
    }

    public int getFirstFreePosition() {
        return firstFreePosition;
    }

}
