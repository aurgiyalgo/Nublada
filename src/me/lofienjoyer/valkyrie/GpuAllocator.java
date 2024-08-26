package me.lofienjoyer.valkyrie;

public interface GpuAllocator {

    MeshInstance store(MeshInstance instance, int[] data);

    void delete(MeshInstance instanceToRemove);

    void update(MeshInstance instance, int[] data);

    void optimizeBuffer(int amount);

    long getSizeInBytes();

    int getFirstFreePosition();

}
