package me.lofienjoyer.valkyrie;

public interface Scene {

    void init();

    void draw(float delta);

    void dispose();

    void resize(int width, int height);

}
