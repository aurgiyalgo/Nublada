package me.lofienjoyer.valkyrie;

public interface GpuAllocator {

    MeshInstance store(int[] data);

    void delete(MeshInstance instanceToRemove);

    void update(MeshInstance instance, int[] data);

    int getFirstFreePosition();

}
