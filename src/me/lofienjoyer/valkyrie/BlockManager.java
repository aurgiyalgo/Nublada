package me.lofienjoyer.valkyrie;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class BlockManager {

    private static final List<BlockType> BLOCK_TYPES = new ArrayList<>(256);

    public static void init() {
        for (int i = 0; i < 256; i++) {
            BLOCK_TYPES.add(new BlockType(i, 0, 0));
        }
        BLOCK_TYPES.get(0).transparency = 3;

        var blocksFolder = new File("res/blocks");
        if (!blocksFolder.exists()) {
            throw new RuntimeException("res/blocks folder not found.");
        }

        var blockFiles = Arrays.stream(blocksFolder.listFiles()).toList();
        var yaml = new Yaml();
        var random = new Random();

        blockFiles.forEach(file -> {
            if (!file.getName().endsWith(".yml"))
                return;

            try {
                var data = (Map<String, Object>) yaml.load(new FileReader(file));

                var transparent = (boolean) data.getOrDefault("transparent", false);
                var texture = (int) data.getOrDefault("texture", 0);
                var id = (int) data.getOrDefault("id", 0);
                var redLight = (int) data.getOrDefault("redLight", 0);
                var greenLight = (int) data.getOrDefault("greenLight", 0);
                var blueLight = (int) data.getOrDefault("blueLight", 0);
                var transparency = (int) data.getOrDefault("transparency", 0);
                var collision = (boolean) data.getOrDefault("collision", true);

                var blockType = new BlockType(id, texture, (redLight & 0x7) << 8 | (greenLight & 0x7) << 4 | (blueLight & 0x7));
                blockType.transparent = transparent;
                blockType.transparency = transparency;
                blockType.collision = collision;
                BLOCK_TYPES.set(id, blockType);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static BlockType getVoxelById(int id) {
        return BLOCK_TYPES.get(id);
    }

}
