package me.lofienjoyer.valkyrie;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Valkyrie {

    public static ExecutorService executorService;

    private static int width = 1280, height = 720;

    public static final Object lock = new Object();

    public static List<Chunk> meshesToDelete = new ArrayList<>();
    public static List<Chunk> chunksToRender = new ArrayList<>();
    public static List<MeshToUpdate> meshesToUpdate = new ArrayList<>();

    public static int drawLength;
    public static int shadowDrawLength;

    public static void main(String[] args) throws IOException {
        final var vsync = 0;
        System.out.println("Vsync: " + (vsync == 1));

        final var shouldUseSsboRendering = shouldUseSsboRendering(args);
        System.out.println("Using SSBO: " + shouldUseSsboRendering);

        if (!glfwInit()) {
            System.err.println("Error loading glfw!");
            return;
        }

        long windowId = glfwCreateWindow(width, height, "Valkyrie", 0, 0);
        glfwWindowHint(GLFW_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_VERSION_MINOR, 6);
        glfwMakeContextCurrent(windowId);
        glfwSwapInterval(vsync);
        GL.createCapabilities();

        var input = Input.getInstance();
        input.setup(windowId);

        ImGui.createContext();
        var imGuiGlfw = new ImGuiImplGlfw();
        var imGuiGl3 = new ImGuiImplGl3();
        imGuiGlfw.init(windowId, true);
        imGuiGl3.init("#version 460");

        ImGui.getIO().setIniFilename(null);
        ImGui.getIO().setLogFilename(null);

        glClearColor(1, 0, 0.75f, 1);

        float[] quadVertices = { // vertex attributes for a quad that fills the entire screen in Normalized Device Coordinates.
                // positions   // texCoords
                -1.0f,  1.0f,  0.0f, 1.0f,
                -1.0f, -1.0f,  0.0f, 0.0f,
                1.0f, -1.0f,  1.0f, 0.0f,

                -1.0f,  1.0f,  0.0f, 1.0f,
                1.0f, -1.0f,  1.0f, 0.0f,
                1.0f,  1.0f,  1.0f, 1.0f
        };

        int quadVao = glGenVertexArrays();
        int quadVbo = glGenBuffers();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, Float.BYTES * 4, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, Float.BYTES * 4, Float.BYTES * 2);

        var fboProgram = new ShaderProgram("fboVertex.glsl", "fboFragment.glsl");

        var fbo = new Framebuffer(width, height);
        var shadowFbo = new DepthFramebuffer();

        int[] data = {
                getData(0, 0),
                getData(2, 2),
                getData(0, 2)
        };

        int vaoId = glGenVertexArrays();
        int vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, 0, 0);

        GpuAllocator allocator;
        ShaderProgram program;
        ShaderProgram shadowProgram;
        if (shouldUseSsboRendering) {
            throw new RuntimeException("SSBO rendering not supported.");
//            allocator = new SsboAllocator(1, 4);
//            program = new ShaderProgram("vertexSSBO.glsl", "fragment.glsl");
        } else {
            program = new ShaderProgram("vertex.glsl", "fragment.glsl");
            shadowProgram = new ShaderProgram("shadowVertex.glsl", "shadowFragment.glsl");
            allocator = new VboAllocator(vaoId, 64 * 1024 * 1024);
        }

        var texture = new Texture("res/textures/blocks/tiles.png");
        glBindTexture(GL_TEXTURE_2D, texture.getId());

        int indirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);

        int chunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);

        int shadowIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, shadowIndirectBuffer);

        int shadowChunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, shadowChunkPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadowChunkPositionBuffer);

        var camera = new Camera();
        program.bind();
        program.setUniform("proj", Camera.createProjectionMatrix(width, height));

        var shadowCamera = new Camera();
        shadowCamera.setPosition(new Vector3f(0, 200, 0));
        shadowProgram.bind();
        shadowProgram.setUniform("proj", Camera.createOrthoProjectionMatrix(128));

        executorService = Executors.newFixedThreadPool(3);
        var world = new World();
        var worldTimer = new Timer();
        worldTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                world.update(camera);
            }
        }, 0, 50);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        long timer = System.nanoTime();
        long counter = 0;
        long frames = 0;

        glfwSetWindowSizeCallback(windowId, (id, width, height) -> {
            Valkyrie.width = width;
            Valkyrie.height = height;
            glViewport(0, 0, width, height);
            program.bind();
            program.setUniform("proj", Camera.createProjectionMatrix(width, height));
            fbo.resize(width, height);
        });

        var delta = 1 / 60f;
        boolean wireframe = false;
        var random = new Random();
        var updateCamera = true;
        var saturation = new float[] { 0.5f };
        var timeSpeed = new float[] { 0f };
        var dayTime = 0f;

        while (!glfwWindowShouldClose(windowId)) {
            if (updateCamera) {
                camera.update(windowId, delta);

                if (Input.isButtonJustPressed(GLFW_MOUSE_BUTTON_1)) {
                    var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, false);
                    if (position != null) {
                        world.setBlock(0, position);
                    }
                }

                if (Input.isButtonJustPressed(GLFW_MOUSE_BUTTON_2)) {
                    var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, true);
                    if (position != null) {
                        world.setBlock(6, position);
                        world.setLight(random.nextInt(0xfff + 1), position);
                    }
                }
            }

            // lock
            synchronized (lock) {
                uploadMeshes(allocator);
                deletePendingMeshes(allocator);
                allocator.optimizeBuffer(32);
                updateIndirectBuffer(chunksToRender, new Vector3i((int) (camera.getPosition().x / 32), 0, (int) (camera.getPosition().z / 32)), indirectBuffer, chunkPositionBuffer, shadowIndirectBuffer, shadowChunkPositionBuffer);
            }

            if (Input.isKeyJustPressed(GLFW_KEY_P))
                wireframe = !wireframe;

            glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

            glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());

            dayTime += delta * timeSpeed[0];
            var worldTime = (float) (Math.sin(dayTime * Math.PI) + 1) / 2f;
            glClearColor(0.125f * (worldTime * 0.9f), 0.5f * (worldTime * 0.9f), 0.75f * (worldTime * 0.9f), 1);

            // Shadow pass
            glViewport(0, 0, 512, 512);
            glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo.getId());
            glClear(GL_DEPTH_BUFFER_BIT);
            shadowCamera.setPosition(camera.getPosition());
//            shadowCamera.getPosition().x += 16;
            shadowCamera.getPosition().y += 64;
            shadowCamera.getPosition().z += 64 * (worldTime * 2 - 1);
            shadowProgram.bind();
            shadowProgram.setUniform("view", Camera.createViewMatrixLookingAt(shadowCamera.getPosition(), camera.getPosition()));
            glBindTexture(GL_TEXTURE_2D, texture.getId());
            glEnable(GL_DEPTH_TEST);
            glBindVertexArray(vaoId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, shadowIndirectBuffer);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, shadowChunkPositionBuffer);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadowChunkPositionBuffer);
            glMultiDrawArraysIndirect(GL_TRIANGLE_FAN, 0, shadowDrawLength, 0);

            // Color pass
            glViewport(0, 0, width, height);
            glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            program.bind();
            program.setUniform("view", Camera.createViewMatrix(camera));
            program.setUniform("shadowView", Camera.createViewMatrixLookingAt(shadowCamera.getPosition(), camera.getPosition()));
            program.setUniform("shadowProj", Camera.createOrthoProjectionMatrix(128));
            program.setUniform("lightPos", shadowCamera.getPosition());
            program.setUniform("camPos", camera.getPosition());
            program.setUniform("dayTime", worldTime);
            program.setUniform("worldTime", (float) glfwGetTime() * 0.0625f);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture.getId());
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureId());
            glEnable(GL_DEPTH_TEST);
            glBindVertexArray(vaoId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);
            glMultiDrawArraysIndirect(GL_TRIANGLE_FAN, 0, drawLength, 0);

            glActiveTexture(GL_TEXTURE0);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClearColor(1, 1, 1, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            fboProgram.bind();
            fboProgram.setUniform("saturation", saturation[0]);
            glBindVertexArray(quadVao);
            glDisable(GL_DEPTH_TEST);
            glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
            glBindTexture(GL_TEXTURE_2D, fbo.getTextureId());
            glDrawArrays(GL_TRIANGLES, 0, 6);

            imGuiGlfw.newFrame();
            ImGui.newFrame();

            ImGui.begin("Debug");

            ImGui.beginTabBar("tabs");

            if (ImGui.beginTabItem("Data")) {
                ImGui.text("FPS: " + 1 / delta);
                ImGui.text("Frame time: " + delta);
                ImGui.text(String.format("X: %.2f", camera.getPosition().x));
                ImGui.text(String.format("Y: %.2f", camera.getPosition().y));
                ImGui.text(String.format("Z: %.2f", camera.getPosition().z));
                ImGui.text("Chunk meshes");
                ImGui.sameLine();
                ImGui.progressBar((float) allocator.getFirstFreePosition() / allocator.getSizeInBytes());
                ImGui.text(allocator.getFirstFreePosition() + " / " + allocator.getSizeInBytes());
                synchronized (lock) {
                    ImGui.text("Chunks rendered: " + chunksToRender.size());
                    ImGui.text("Meshes to delete: " + meshesToDelete.size());
                    ImGui.text("Meshes to update: " + meshesToUpdate.size());
                }
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Params")) {
                ImGui.sliderFloat("Saturation", saturation, 0.25f, 0.75f);
                ImGui.sliderFloat("Time speed", timeSpeed, 0f, 1f);
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Textures")) {
                ImGui.image(texture.getId(), 256, 256);
                ImGui.sameLine();
                ImGui.image(fbo.getTextureId(), 256, 256);
                ImGui.image(shadowFbo.getDepthTextureId(), 256, 256);
                ImGui.endTabItem();
            }

            ImGui.endTabBar();

            updateCamera = !ImGui.isWindowHovered() && !ImGui.isWindowFocused();

            ImGui.end();

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            input.update();
            glfwPollEvents();
            glfwSwapBuffers(windowId);
            counter += (System.nanoTime() - timer);
            delta = (System.nanoTime() - timer) / 1000000000f;
            timer = System.nanoTime();
            frames++;
        }

        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        worldTimer.cancel();
        executorService.shutdownNow();

        glfwDestroyWindow(windowId);

        System.out.println("Average frame time: " + ((float) counter / 1000000f) / frames + "ms");
    }

    private static void deletePendingMeshes(GpuAllocator allocator) {
        if (meshesToDelete.isEmpty())
            return;

        for (int i = 0; i < Math.min(meshesToDelete.size(), 32); i++) {
            var chunk = meshesToDelete.removeFirst();
            if (chunk.getMesh() != null)
                allocator.delete(chunk.getMesh());
        }
    }

    private static void uploadMeshes(GpuAllocator allocator) {
        for (int i = 0; i < Math.min(meshesToUpdate.size(), 32); i++) {
            var meshToUpdate = meshesToUpdate.removeFirst();
            var mesh = meshToUpdate.chunk().getMesh();
            if (mesh == null) {
                meshToUpdate.chunk().setMesh(allocator.store(new MeshInstance(0, 0), meshToUpdate.data()));
            } else {
                allocator.update(mesh, meshToUpdate.data());
            }
        }
    }

    private static int updateIndirectBuffer(Collection<Chunk> chunks, Vector3i camPos, int indirectBuffer, int chunkPositionBuffer, int shadowIndirectBuffer, int shadowChunkPositionBuffer) {
        var indirectCmdsList = new ArrayList<Integer>();
        var chunkPositions = new ArrayList<Vector3i>();
        var shadowIndirectCmdsList = new ArrayList<Integer>();
        var shadowChunkPositions = new ArrayList<Vector3i>();
        for (Chunk chunk : chunks) {
            var mesh = chunk.getMesh();
            if (mesh == null)
                continue;

            indirectCmdsList.add(3);
            indirectCmdsList.add(mesh.getLength());
            indirectCmdsList.add(0);
            indirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));
            var position = chunk.getPosition();
            chunkPositions.add(position);
            if (chunk.getPosition().x > camPos.x + 2 || chunk.getPosition().x < camPos.x - 2 || chunk.getPosition().z > camPos.z + 2 || chunk.getPosition().z < camPos.z - 2)
                continue;

            shadowIndirectCmdsList.add(3);
            shadowIndirectCmdsList.add(mesh.getLength());
            shadowIndirectCmdsList.add(0);
            shadowIndirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));
            shadowChunkPositions.add(position);
        }

        int[] chunkPositionsArray = new int[chunkPositions.size() * 3];
        for (int i = 0; i < chunkPositions.size(); i++) {
            var position = chunkPositions.get(i);
            chunkPositionsArray[i * 3] = position.x;
            chunkPositionsArray[i * 3 + 1] = position.y;
            chunkPositionsArray[i * 3 + 2] = position.z;
        }

        int[] shadowChunkPositionsArray = new int[shadowChunkPositions.size() * 3];
        for (int i = 0; i < shadowChunkPositions.size(); i++) {
            var position = shadowChunkPositions.get(i);
            shadowChunkPositionsArray[i * 3] = position.x;
            shadowChunkPositionsArray[i * 3 + 1] = position.y;
            shadowChunkPositionsArray[i * 3 + 2] = position.z;
        }

        int[] shadowIndirectCmds = new int[shadowIndirectCmdsList.size()];
        for (int i = 0; i < shadowIndirectCmdsList.size(); i++) {
            shadowIndirectCmds[i] = shadowIndirectCmdsList.get(i);
        }

        int[] indirectCmds = new int[indirectCmdsList.size()];
        for (int i = 0; i < indirectCmdsList.size(); i++) {
            indirectCmds[i] = indirectCmdsList.get(i);
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectCmds, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionsArray, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, shadowIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, shadowIndirectCmds, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, shadowChunkPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, shadowChunkPositionsArray, GL_DYNAMIC_DRAW);

        drawLength = indirectCmds.length / 4;
        shadowDrawLength = shadowIndirectCmds.length / 4;
        return indirectCmds.length / 4;
    }

    public static int[] integerListToArray(List<Integer> list) {
        int[] data = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            data[i] = list.get(i);
        }
        return data;
    }

    private static int getData(int x, int y) {
        return x << 5 | y;
    }

    private static boolean shouldUseSsboRendering(String[] args) {
        return args.length != 0 && args[0].equals("--use-ssbo");
    }

}
