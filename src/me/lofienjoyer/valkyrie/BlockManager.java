package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;

public class BlockManager {

    private static final List<BlockType> BLOCK_TYPES = new ArrayList<>();

    public static void init() {
        for (int i = 0; i < 16; i++) {
            BLOCK_TYPES.add(new BlockType(i, i, 0, 0, 0));
        }
        BLOCK_TYPES.get(3).transparent = true;
    }

    public static BlockType getVoxelById(int id) {
        return BLOCK_TYPES.get(id);
    }

}
