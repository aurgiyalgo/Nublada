package me.lofienjoyer.valkyrie;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
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
        if (shouldUseSsboRendering) {
            allocator = new SsboAllocator(1, 4);
            program = new ShaderProgram("vertexSSBO.glsl", "fragment.glsl");
        } else {
            program = new ShaderProgram("vertex.glsl", "fragment.glsl");
            allocator = new VboAllocator(vaoId, 4);
        }
        program.bind();

        var texture = new Texture("res/textures/blocks/tiles.png");
        glBindTexture(GL_TEXTURE_2D, texture.getId());

        int indirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);

        int chunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);

        var camera = new Camera();
        program.setUniform("proj", Camera.createProjectionMatrix(width, height));

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

            program.bind();
            program.setUniform("view", Camera.createViewMatrix(camera));

            // lock
            List<Chunk> chunks;
            int drawLength;
            synchronized (lock) {
                uploadMeshes(allocator);
                deletePendingMeshes(allocator);
                drawLength = updateIndirectBuffer(chunksToRender, indirectBuffer, chunkPositionBuffer);
            }

            if (Input.isKeyJustPressed(GLFW_KEY_P))
                wireframe = !wireframe;

            glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

            glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());

            dayTime += delta * timeSpeed[0];
            var worldTime = (float) (Math.sin(dayTime * Math.PI) + 1) / 2f;
            glClearColor(0.125f * (worldTime * 0.9f), 0.5f * (worldTime * 0.9f), 0.75f * (worldTime * 0.9f), 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            program.setUniform("dayTime", worldTime);
            program.setUniform("worldTime", (float) glfwGetTime() * 0.0625f);
            glBindTexture(GL_TEXTURE_2D, texture.getId());
            glEnable(GL_DEPTH_TEST);
            glBindVertexArray(vaoId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glMultiDrawArraysIndirect(GL_TRIANGLE_FAN, 0, drawLength, 0);

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
                ImGui.text("FPS: " + 1 / ((System.nanoTime() - timer) / 1000000000f));
                ImGui.text("Frame time: " + (System.nanoTime() - timer) / 1000000000f);
                ImGui.text("Chunk meshes");
                ImGui.sameLine();
                ImGui.progressBar((float) allocator.getFirstFreePosition() / (4 * 1024 * 1024 * Integer.BYTES));
                ImGui.text(allocator.getFirstFreePosition() + " / " + (4 * 1024 * 1024 * Integer.BYTES));
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
        for (int i = 0; i < Math.min(meshesToDelete.size(), 10); i++) {
            var chunk = meshesToDelete.removeFirst();
            if (chunk.getMesh() != null)
                allocator.delete(chunk.getMesh());
        }
    }

    private static void uploadMeshes(GpuAllocator allocator) {
        for (int i = 0; i < Math.min(meshesToUpdate.size(), 2); i++) {
            var meshToUpdate = meshesToUpdate.removeFirst();
            var mesh = meshToUpdate.chunk().getMesh();
            if (mesh == null) {
                meshToUpdate.chunk().setMesh(allocator.store(meshToUpdate.data()));
            } else {
                allocator.update(mesh, meshToUpdate.data());
            }
        }
    }

    private static int updateIndirectBuffer(Collection<Chunk> chunks, int indirectBuffer, int chunkPositionBuffer) {
        var indirectCmdsList = new ArrayList<Integer>();
        var chunkPositions = new ArrayList<Vector3i>();
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
        }

        int[] chunkPositionsArray = new int[chunkPositions.size() * 3];
        for (int i = 0; i < chunkPositions.size(); i++) {
            var position = chunkPositions.get(i);
            chunkPositionsArray[i * 3] = position.x;
            chunkPositionsArray[i * 3 + 1] = position.y;
            chunkPositionsArray[i * 3 + 2] = position.z;
        }

        int[] indirectCmds = new int[indirectCmdsList.size()];
        for (int i = 0; i < indirectCmdsList.size(); i++) {
            indirectCmds[i] = indirectCmdsList.get(i);
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectCmds, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionsArray, GL_DYNAMIC_DRAW);

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

    private static int getData(int x, int y, int z) {
        return z << 14 | x << 7 | y;
    }

    private static void toPosition(int data) {
        System.out.println(((data >> 7) & 0x1f) + " " + (data & 0x1f) + " " + ((data >> 14) & 0x1f));
    }

    private static boolean shouldUseSsboRendering(String[] args) {
        return args.length != 0 && args[0].equals("--use-ssbo");
    }

}
