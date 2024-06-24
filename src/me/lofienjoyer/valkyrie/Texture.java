package me.lofienjoyer.valkyrie;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int id;

    public Texture(String fileName) {
        int width;
        int height;
        ByteBuffer buf;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buf = stbi_load(fileName, w, h, channels, 4);
            if (buf == null) {
                throw new RuntimeException("Image file [" + fileName  + "] not loaded: " + stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }

        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);

        glGenerateMipmap(GL_TEXTURE_2D);

        stbi_image_free(buf);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() {
        return id;
    }

}
