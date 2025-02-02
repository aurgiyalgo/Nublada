package me.lofienjoyer.valkyrie;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer {

    private final int id;
    private int textureId, rboId;

    public Framebuffer(int width, int height, int samples) {
        this.id = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, id);

        resize(width, height, samples);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error while creating framebuffer!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height, int samples) {
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        if (textureId != 0)
            glDeleteTextures(textureId);
        this.textureId = glGenTextures();

        if (rboId != 0)
            glDeleteRenderbuffers(rboId);
        this.rboId = glGenRenderbuffers();

        if (samples == 0) {
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
            glBindTexture(GL_TEXTURE_2D, 0);

            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        } else {
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureId);
            glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, textureId, 0);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGB, width, height, true);
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

            glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH24_STENCIL8, width, height);
        }
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getId() {
        return id;
    }

    public int getTextureId() {
        return textureId;
    }

}
