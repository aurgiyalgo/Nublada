package me.lofienjoyer.valkyrie;

import static org.lwjgl.opengl.GL30.*;

public class DepthFramebuffer {

    private final int id, depthTextureId;

    public DepthFramebuffer() {
        this.id = glGenFramebuffers();
        this.depthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, WorldScene.SHADOW_MAP_RESOLUTION, WorldScene.SHADOW_MAP_RESOLUTION, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
        glBindFramebuffer(GL_FRAMEBUFFER, id);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getId() {
        return id;
    }

    public int getDepthTextureId() {
        return depthTextureId;
    }

}
