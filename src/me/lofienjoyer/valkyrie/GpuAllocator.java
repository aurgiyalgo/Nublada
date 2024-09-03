package me.lofienjoyer.valkyrie;

import java.util.Collection;
import java.util.List;

public interface GpuAllocator {

    Collection<MeshInstance> getMeshes();

    MeshInstance store(long id, int[] data);

    void delete(long id);

    void update(long id, int[] data);

    void optimizeBuffer(int amount);

    long getSizeInBytes();

    int getFirstFreePosition();

}
