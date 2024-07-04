package me.lofienjoyer.valkyrie;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class ShaderProgram {

    private final int vertexShader, fragmentShader, shaderProgram;

    private final FloatBuffer buffer;

    public ShaderProgram(String vertexSrc, String fragmentSrc) throws IOException {
        this.buffer = BufferUtils.createFloatBuffer(16);
        shaderProgram = glCreateProgram();
        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, Files.readString(Path.of("res", "shaders", vertexSrc)));
        glCompileShader(vertexShader);
        glAttachShader(shaderProgram, vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(vertexShader, 512));
        }
        fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, Files.readString(Path.of("res", "shaders", fragmentSrc)));
        glCompileShader(fragmentShader);
        glAttachShader(shaderProgram, fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(fragmentShader, 512));
        }
        glLinkProgram(shaderProgram);
        glValidateProgram(shaderProgram);
    }

    public void bind() {
        glUseProgram(shaderProgram);
    }

    protected int getUniformLocation(String uniformName) {
        return glGetUniformLocation(shaderProgram, uniformName);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        glUniformMatrix4fv(getUniformLocation(uniformName), false, value.get(buffer));
    }

    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniformLocation(uniformName), value);
    }

}
