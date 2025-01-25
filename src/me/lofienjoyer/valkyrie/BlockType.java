package me.lofienjoyer.valkyrie;

public class BlockType {

    public int id;
    public int textureId;
    public int light;
    public boolean transparent;
    public int transparency;

    public BlockType(int id, int textureId, int light) {
        this.id = id;
        this.textureId = textureId;
        this.light = light;
    }

}
