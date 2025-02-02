package me.lofienjoyer.valkyrie;

import static org.lwjgl.opengl.GL46.*;

public class IntermediateFramebuffer {

    private final int id, textureId;

    public IntermediateFramebuffer(int width, int height) {
        this.id = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        resize(width, height);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error while creating framebuffer!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height) {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() {
        return id;
    }

    public int getTextureId() {
        return textureId;
    }

}
