package me.lofienjoyer.valkyrie;

public class BlockType {

    public int id;
    public int textureId;
    public int redLight;
    public int greenLight;
    public int blueLight;
    public boolean transparent;

    public BlockType(int id, int textureId, int redLight, int greenLight, int blueLight) {
        this.id = id;
        this.textureId = textureId;
        this.redLight = redLight;
        this.greenLight = greenLight;
        this.blueLight = blueLight;
    }

}
