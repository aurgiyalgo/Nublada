package me.lofienjoyer.valkyrie;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer {

    private final int id, textureId, rboId;

    public Framebuffer(int width, int height, int samples) {
        this.id = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        this.textureId = glGenTextures();
        this.rboId = glGenRenderbuffers();
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureId);
        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, textureId, 0);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        resize(width, height, samples);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error while creating framebuffer!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height, int samples) {
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureId);
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGB, width, height, true);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH24_STENCIL8, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public int getId() {
        return id;
    }

    public int getTextureId() {
        return textureId;
    }

}
