package me.lofienjoyer.valkyrie;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.glVertexBindingDivisor;

public class VboAllocator implements GpuAllocator{

    public static final AtomicInteger idCounter = new AtomicInteger(1);

    private static final int WORD_SIZE = 1024;

    private final int vboId;
    private final int auxVboId;
    private final long[] allocatorData;
    private final long sizeInBytes;
    private final Map<Long, MeshInstance> instances;
    private int firstFreePosition = 0;

    public VboAllocator(int vao, long sizeInBytes) {
        this.allocatorData = new long[(int) (sizeInBytes / WORD_SIZE)];
        this.sizeInBytes = sizeInBytes;
        this.instances = new HashMap<>();

        glBindVertexArray(vao);
        this.vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, sizeInBytes, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 2, GL_UNSIGNED_INT, 0, 0);
        glVertexBindingDivisor(1, 1);

        this.auxVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, auxVboId);
    }

    @Override
    public Collection<MeshInstance> getMeshes() {
        return instances.values();
    }

    @Override
    public MeshInstance store(long id, int[] dataToAllocate) {
        if (dataToAllocate.length == 0) {
            var mesh = new MeshInstance(0, 0);
            mesh.setIndex(0);
            mesh.setLength(0);
            mesh.setId(id);
            return mesh;
        }

        int blocksNeeded = ((dataToAllocate.length * Integer.BYTES) / WORD_SIZE) + 1;

        for (int i = firstFreePosition; i < firstFreePosition + blocksNeeded; i++) {
            if (allocatorData[i] != 0)
                continue;

            boolean isCurrentIndexValid = true;

            for (int j = 0; j < blocksNeeded; j++) {
                if (allocatorData[i + j] != 0) {
                    isCurrentIndexValid = false;
                    break;
                }
            }

            if (!isCurrentIndexValid)
                continue;

            var mesh = new MeshInstance(0, 0);
            mesh.setId(id);
            instances.put(id, mesh);

            mesh.setLength(dataToAllocate.length / 2);
            mesh.setIndex(i * WORD_SIZE);
            for (int j = 0; j < blocksNeeded; j++) {
                allocatorData[i + j] = mesh.getId();
            }

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferSubData(GL_ARRAY_BUFFER, mesh.getIndex(), dataToAllocate);
            firstFreePosition += blocksNeeded;

            return mesh;
        }

        throw new RuntimeException("Buffer is full!");
    }

    @Override
    public void delete(long id) {
        var instanceToRemove = instances.get(id);
        if (instanceToRemove == null)
            return;

        if (instanceToRemove.getLength() == 0)
            return;

        var index = instanceToRemove.getIndex() / WORD_SIZE;
        var size = (instanceToRemove.getLength() * 2 * Integer.BYTES) / WORD_SIZE + 1;
        for (int i = index; i < index + size; i++) {
            allocatorData[i] = 0;
        }
        instances.remove(instanceToRemove.getId());
    }

    @Override
    public void optimizeBuffer(int amount) {
        int firstEmptyIndex = 0;
        int optimizedMeshes = 0;

        for (int i = 0; i < allocatorData.length; i++) {
            if (allocatorData[i] != 0)
                continue;

            firstEmptyIndex = i;

            int firstPopulatedIndex = getFirstPopulatedIndexAfter(i);
            if (firstPopulatedIndex == -1) {
                firstFreePosition = firstEmptyIndex;
                break;
            }

            long dataFound = allocatorData[firstPopulatedIndex];
            int dataLength = (instances.get(dataFound).getLength() * 2 * Integer.BYTES) / WORD_SIZE + 1;

            for (int j = firstPopulatedIndex; j < dataLength + firstPopulatedIndex; j++) {
                allocatorData[j] = 0;
            }

            for (int j = firstEmptyIndex; j < dataLength + firstEmptyIndex; j++) {
                allocatorData[j] = dataFound;
            }

            var mesh = instances.get(dataFound);
            glBindBuffer(GL_ARRAY_BUFFER, auxVboId);
            glBufferData(GL_ARRAY_BUFFER, mesh.getLength() * 2 * Integer.BYTES, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_COPY_READ_BUFFER, vboId);
            glBindBuffer(GL_COPY_WRITE_BUFFER, auxVboId);
            glCopyBufferSubData(
                    GL_COPY_READ_BUFFER,
                    GL_COPY_WRITE_BUFFER,
                    mesh.getIndex(),
                    0,
                    mesh.getLength() * 2 * Integer.BYTES
            );

            glBindBuffer(GL_COPY_READ_BUFFER, auxVboId);
            glBindBuffer(GL_COPY_WRITE_BUFFER, vboId);
            glCopyBufferSubData(
                    GL_COPY_READ_BUFFER,
                    GL_COPY_WRITE_BUFFER,
                    0,
                    firstEmptyIndex * WORD_SIZE,
                    mesh.getLength() * 2 * Integer.BYTES
            );

            mesh.setIndex(firstEmptyIndex * WORD_SIZE);

            optimizedMeshes++;
            if (optimizedMeshes == amount)
                break;
        }
    }

    @Override
    public void update(long id, int[] data) {
        if (instances.containsKey(id))
            delete(id);

        store(id, data);
    }

    private int getFirstPopulatedIndexAfter(int index) {
        for (int i = index; i < allocatorData.length; i++) {
            if (allocatorData[i] != 0)
                return i;
        }
        return -1;
    }

    @Override
    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public int getFirstFreePosition() {
        return firstFreePosition * WORD_SIZE;
    }

}
