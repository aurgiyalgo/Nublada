package me.lofienjoyer.valkyrie;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Vector3d;
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

    public static final int SHADOW_MAP_RESOLUTION = 1024;

    public static List<Long> meshesToDelete = new ArrayList<>();
    public static List<Chunk> chunksToRender = new ArrayList<>();
    public static final List<MeshToUpdate> meshesToUpdate = new ArrayList<>();

    public static int drawLength;

    public static void main(String[] args) throws IOException {
        final var vsync = 1;
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

        final var gpuName = glGetString(GL_RENDERER);

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
            allocator = new VboAllocator(vaoId, 256 * 1024 * 1024);
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
        var saturation = new float[] { 0.5f };
        var timeSpeed = new float[] { 0.005f };
        var dayTime = 0f;
        var debug = false;

        while (!glfwWindowShouldClose(windowId)) {
            if (Input.isKeyJustPressed(GLFW_KEY_F3))
                debug = !debug;

            if (!debug) {
                camera.update(windowId, delta);

                if (Input.isButtonJustPressed(GLFW_MOUSE_BUTTON_1)) {
                    var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, false);
                    if (position != null) {
                        world.setBlock(0, position);
                    }
                }

                if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_2)) {
                    var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, true);
                    if (position != null) {
                        world.setBlock(6, position);
                        world.setLight(random.nextInt(0xfff + 1), position);
                    }
                }
            }

            simulatePhysics(camera, delta, world);

            // lock
            synchronized (lock) {
                uploadMeshes(allocator);
                deletePendingMeshes(allocator);
                for (int i = 0; i < 2; i++) {
                    allocator.optimizeBuffer(64);
                }
                updateIndirectBuffer(allocator, new Vector3i((int) (camera.getPosition().x / 32), 0, (int) (camera.getPosition().z / 32)), indirectBuffer, chunkPositionBuffer, shadowIndirectBuffer);
            }

            if (Input.isKeyJustPressed(GLFW_KEY_P))
                wireframe = !wireframe;

            glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

            glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());

            dayTime += delta * timeSpeed[0];
            var worldTime = dayTime % 1;
            var timeOfDay = 1 - (Math.abs(worldTime * 2 - 1) + 1) / 2f;
            var light = 1 - (Math.min(Math.abs(worldTime * 3 - 1.5f), 1) + 1) / 2f;
            var lightPow = (float) Math.sqrt(light);
            glClearColor(0.125f * lightPow, 0.5f * lightPow, 0.75f * lightPow, 1);
            var sunAngle = Math.PI * 2 * worldTime - Math.PI / 2;

            // Shadow pass
//            glViewport(0, 0, 1024, 1024);
//            glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo.getId());
//            glClear(GL_DEPTH_BUFFER_BIT);
            var shadowCameraPosition = new Vector3f();
            shadowCameraPosition.x += (float) Math.cos(sunAngle) * 25;
            shadowCameraPosition.y += (float) Math.sin(sunAngle) * 512;
            shadowCameraPosition.z += (float) Math.cos(sunAngle) * 512;
//            shadowCamera.setPosition(new Vector3f(shadowCameraPosition));
//            shadowProgram.bind();
//            shadowProgram.setUniform("view", Camera.createViewMatrixLookingAt(shadowCamera.getPosition(), new Vector3f(camera.getPosition().x % 32, camera.getPosition().y % 32, camera.getPosition().z % 32)));
//            shadowProgram.setUniform("camChunkPos", new Vector3i((int)(camera.getPosition().x / 32), (int)(camera.getPosition().y / 32), (int)(camera.getPosition().z / 32)));
//            glBindTexture(GL_TEXTURE_2D, texture.getId());
//            glDisable(GL_CULL_FACE);
//            glEnable(GL_DEPTH_TEST);
//            glBindVertexArray(vaoId);
//            glBindBuffer(GL_ARRAY_BUFFER, vboId);
//            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, shadowIndirectBuffer);
//            glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
//            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);
//            glMultiDrawArraysIndirect(GL_TRIANGLE_FAN, 0, drawLength, 0);
//            glEnable(GL_CULL_FACE);

            // Color pass
            glViewport(0, 0, width, height);
            glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            program.bind();
            program.setUniform("view", Camera.createViewMatrix(camera));
            program.setUniform("shadowView", Camera.createViewMatrixLookingAt(shadowCamera.getPosition(), new Vector3f(camera.getPosition().x % 32, camera.getPosition().y % 32, camera.getPosition().z % 32)));
            program.setUniform("shadowProj", Camera.createOrthoProjectionMatrix(128));
            program.setUniform("lightDir", new Vector3f(shadowCameraPosition).normalize());
            program.setUniform("camPos", camera.getPosition());
            program.setUniform("camChunkPos", new Vector3i((int)(camera.getPosition().x / 32), (int)(camera.getPosition().y / 32), (int)(camera.getPosition().z / 32)));
            program.setUniform("dayTime", worldTime);
            program.setUniform("worldTime", (float) glfwGetTime() * 0.0625f);
            program.setUniform("timeOfDay", timeOfDay);
            program.setUniform("light", (float) Math.pow(light, 0.3));
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

            if (debug) {
                imGuiGlfw.newFrame();
                ImGui.newFrame();

                ImGui.begin("Debug");

                ImGui.beginTabBar("tabs");

                if (ImGui.beginTabItem("Data")) {
                    ImGui.text("GPU: " + gpuName);
                    ImGui.text("FPS: " + 1 / delta);
                    ImGui.text("Frame time: " + delta);
                    ImGui.text("World time: " + worldTime);
                    ImGui.text("Sun angle: " + sunAngle);
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

                ImGui.end();

                ImGui.render();
                imGuiGl3.renderDrawData(ImGui.getDrawData());
            }

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
            allocator.delete(meshesToDelete.removeFirst());
        }
    }

    private static void uploadMeshes(GpuAllocator allocator) {
        for (int i = 0; i < Math.min(meshesToUpdate.size(), 32); i++) {
            var meshToUpdate = meshesToUpdate.removeFirst();
            allocator.update(meshToUpdate.id(), meshToUpdate.data());
        }
    }

    private static int updateIndirectBuffer(GpuAllocator allocator, Vector3i camPos, int indirectBuffer, int chunkPositionBuffer, int shadowIndirectBuffer) {
        var indirectCmdsList = new ArrayList<Integer>();
        var chunkPositions = new ArrayList<Long>();
        var shadowIndirectCmdsList = new ArrayList<Integer>();
        allocator.getMeshes().forEach(mesh -> {
            indirectCmdsList.add(3);
            indirectCmdsList.add(mesh.getLength());
            indirectCmdsList.add(0);
            indirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));

            chunkPositions.add(mesh.getId());
            var meshX = (mesh.getId() & 0xffff);
            var meshZ = (mesh.getId() >> 32) & 0xffff;

            shadowIndirectCmdsList.add(3);
            if (meshX > camPos.x + 5 || meshX < camPos.x - 5 || meshZ > camPos.z + 5 || meshZ < camPos.z - 5) {
                shadowIndirectCmdsList.add(0);
            } else {
                shadowIndirectCmdsList.add(mesh.getLength());
            }

            shadowIndirectCmdsList.add(0);
            shadowIndirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));
        });

        int[] chunkPositionsArray = new int[chunkPositions.size() * 2];
        for (int i = 0; i < chunkPositions.size(); i++) {
            var position = chunkPositions.get(i);
            chunkPositionsArray[i * 2] = (int) (position & 0xffffffff);
            chunkPositionsArray[i * 2 + 1] = (int) (position >> 32);
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

        drawLength = indirectCmds.length / 4;
        return indirectCmds.length / 4;
    }

    private static void simulatePhysics(Camera camera, float delta, World world) {
        var movementVector = new Vector3f((float) camera.movement.x, (float) camera.movement.y, (float) camera.movement.z).add(new Vector3f(0, -4f, 0).mul(delta));

        checkX(camera.getPosition(), movementVector, world);
        checkY(camera.getPosition(), movementVector, world);
        checkZ(camera.getPosition(), movementVector, world);

        camera.setPosition(camera.getPosition().add(movementVector));
        camera.movement = new Vector3d();
    }

    private static boolean checkX(Vector3f position, Vector3f movement, World world) {
        if (Math.floor(position.x) == Math.floor(position.x + movement.x + 0.25f * ValkyrieMath.signum(movement.x)))
            return false;

        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 1; z++) {
                var block = world.getBlock(position.add(movement.x + 0.25f * ValkyrieMath.signum(movement.x), y - 0.75f, z));
                if (block != 0) {
                    movement.x = 0;
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkY(Vector3f position, Vector3f movement, World world) {
        if (Math.floor(position.y) == Math.floor(position.y + movement.y + 0.75f * ValkyrieMath.signum(movement.y)))
            return false;

        for (int x = 0; x < 1; x++) {
            for (int z = 0; z < 1; z++) {
                var block = world.getBlock(position.add(x, movement.y + 0.75f * ValkyrieMath.signum(movement.y), z));
                if (block != 0) {
                    movement.y = 0;
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkZ(Vector3f position, Vector3f movement, World world) {
        if (Math.floor(position.z) == Math.floor(position.z + movement.z + 0.25f * ValkyrieMath.signum(movement.z)))
            return false;

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 1; x++) {
                var block = world.getBlock(position.add(x, y - 0.75f, movement.z + 0.25f * ValkyrieMath.signum(movement.z)));
                if (block != 0) {
                    movement.z = 0;
                    return true;
                }
            }
        }

        return false;
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
