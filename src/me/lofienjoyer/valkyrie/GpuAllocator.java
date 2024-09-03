package me.lofienjoyer.valkyrie;

import java.util.Collection;
import java.util.List;

public interface GpuAllocator {

    Collection<MeshInstance> getMeshes();

    MeshInstance store(MeshInstance instance, int[] data);

    void delete(MeshInstance instanceToRemove);

    void update(MeshInstance instance, int[] data);

    void optimizeBuffer(int amount);

    long getSizeInBytes();

    int getFirstFreePosition();

}
