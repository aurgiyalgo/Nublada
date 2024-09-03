package me.lofienjoyer.valkyrie;

public class MeshInstance {

    private int length;
    private int index;
    private long id;

    public MeshInstance(int length, int index) {
        this.length = length;
        this.index = index;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
