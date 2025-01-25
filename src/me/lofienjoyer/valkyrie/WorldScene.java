package me.lofienjoyer.valkyrie;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class WorldScene implements Scene {

    public static final int SHADOW_MAP_RESOLUTION = 1024;

    public static List<Long> meshesToDelete = new ArrayList<>();
    public static List<Chunk> chunksToRender = new ArrayList<>();
    public static final List<MeshToUpdate> meshesToUpdate = new ArrayList<>();

    public static int drawLength;

    private String gpuName;
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private int quadVao, quadVbo, vaoId, vboId;
    private GpuAllocator allocator;
    private ShaderProgram program, shadowProgram, fboProgram, uiProgram, sunProgram, fontProgram;
    private Framebuffer fbo;
    private DepthFramebuffer shadowFbo;
    private Texture texture, versionTexture, sunTexture;
    private int indirectBuffer, chunkPositionBuffer, shadowIndirectBuffer, fontIndirectBuffer, fontPositionBuffer;
    private Camera camera, shadowCamera;
    private World world;
    private Timer worldTimer;
    private FrustumIntersection intersection;

    boolean wireframe = false;
    float[] saturation = new float[] { 0.5f };
    float[] timeSpeed = new float[] { 0.001f };
    float dayTime = 0.3f;
    boolean debug = false;

    private int selectedBlock = 1;

    public void init() {
        final var shouldUseSsboRendering = false;
        System.out.println("Using SSBO: " + shouldUseSsboRendering);

        gpuName = glGetString(GL_RENDERER);

        ImGui.createContext();
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGlfw.init(Valkyrie.windowId, true);
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

        quadVao = glGenVertexArrays();
        quadVbo = glGenBuffers();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, Float.BYTES * 4, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, Float.BYTES * 4, Float.BYTES * 2);

        fboProgram = new ShaderProgram("fboVertex.glsl", "fboFragment.glsl");

        fbo = new Framebuffer(Valkyrie.width, Valkyrie.height);
        shadowFbo = new DepthFramebuffer();

        int[] data = {
                getData(0, 0),
                getData(2, 2),
                getData(0, 2)
        };

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, 0, 0);

        if (shouldUseSsboRendering) {
            throw new RuntimeException("SSBO rendering not supported.");
//            allocator = new SsboAllocator(1, 4);
//            program = new ShaderProgram("vertexSSBO.glsl", "fragment.glsl");
        } else {
            program = new ShaderProgram("vertex.glsl", "fragment.glsl");
            shadowProgram = new ShaderProgram("shadowVertex.glsl", "shadowFragment.glsl");
            allocator = new VboAllocator(vaoId, 64 * 1024 * 1024);
        }

        sunProgram = new ShaderProgram("sunVertex.glsl", "sunFragment.glsl");
        sunProgram.bind();
        sunProgram.setUniform("proj", Camera.createProjectionMatrix(Valkyrie.width, Valkyrie.height));
        sunTexture = new Texture("res/textures/sun.png");

        uiProgram = new ShaderProgram("uiVertex.glsl", "uiFragment.glsl");
        fontProgram = new ShaderProgram("fontVertex.glsl", "fontFragment.glsl");
        versionTexture = new Texture("res/textures/version.png");

        texture = new Texture("res/textures/blocks/terrain.png");
        glBindTexture(GL_TEXTURE_2D, texture.getId());

        indirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);

        chunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);

        shadowIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, shadowIndirectBuffer);

        fontIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, fontIndirectBuffer);

        fontPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, fontPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, fontPositionBuffer);

        camera = new Camera();
        program.bind();
        program.setUniform("proj", Camera.createProjectionMatrix(Valkyrie.width, Valkyrie.height));

        shadowCamera = new Camera();
        shadowCamera.setPosition(new Vector3f(0, 200, 0));
        shadowProgram.bind();
        shadowProgram.setUniform("proj", Camera.createOrthoProjectionMatrix(128));

        intersection = new FrustumIntersection();

        world = new World();
        worldTimer = new Timer();
        worldTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                world.update(camera);
            }
        }, 0, 50);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public void draw(float delta) {
        if (Input.isKeyJustPressed(GLFW_KEY_ESCAPE))
            debug = !debug;

        if (!debug) {
            camera.update(Valkyrie.windowId, delta);
            if (Input.isButtonJustPressed(GLFW_MOUSE_BUTTON_1)) {
                var position = world.rayCast(camera.getPosition(), camera.getDirection(), 8, false);
                if (position != null) {
                    world.setBlock(0, position);
                }
            }

            if (Input.isButtonJustPressed(GLFW_MOUSE_BUTTON_2)) {
                var position = world.rayCast(camera.getPosition(), camera.getDirection(), 8, true);
                if (position != null) {
                    world.setBlock(selectedBlock, position);
                }
            }

            if (Input.isKeyJustPressed(GLFW_KEY_Q)) {
                selectedBlock--;
                if (selectedBlock < 0)
                    selectedBlock = 255;
            } else if (Input.isKeyJustPressed(GLFW_KEY_E)) {
                selectedBlock++;
                if (selectedBlock > 255)
                    selectedBlock = 0;
            }

            if (Input.isKeyJustPressed(GLFW_KEY_SPACE)) {
                if (camera.movement.y == 0d) {
                    camera.movement.y += 10f;
                }
            }
        } else {
            GLFW.glfwSetInputMode(Valkyrie.windowId, GLFW.GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        simulatePhysics(camera, delta, world);

        intersection.set(Camera.createProjectionMatrix(Valkyrie.width, Valkyrie.height).mul(Camera.createCompleteViewMatrix(camera)));

        // lock
        synchronized (Valkyrie.lock) {
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
        var adjustedTime = Math.min(0.8f, Math.max(0.2f, worldTime));
        var light = 1 - Math.min(Math.pow(Math.abs(((adjustedTime - 0.2f) / (0.8f - 0.2f)) * 2 - 1), 3), 1);
        var lightPow = (float) Math.sqrt(light);
        var skyColor = new Vector3f(0.125f * lightPow, 0.5f * lightPow, 0.375f * lightPow);
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1);
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
        glViewport(0, 0, Valkyrie.width, Valkyrie.height);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo.getId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        var transformMatrix = new Matrix4f();
        transformMatrix.identity();
        transformMatrix.rotate((float) (-worldTime * Math.PI * 2 - Math.PI / 2f),  new Vector3f(1, 0, 0));
        transformMatrix.rotate((float) Math.toRadians(10),  new Vector3f(0, 1, 0));
        transformMatrix.rotate((float) Math.toRadians(3),  new Vector3f(0, 1, 0));
        transformMatrix.translate(0, 0, -100);
        transformMatrix.scale(2.5f);
        glBindTexture(GL_TEXTURE_2D, sunTexture.getId());
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        sunProgram.bind();
        sunProgram.setUniform("view", Camera.createViewMatrixNoPosition(camera));
        sunProgram.setUniform("trans", transformMatrix);
        sunProgram.setUniform("camPos", camera.getPosition());
        glBindVertexArray(quadVao);
        glDisable(GL_DEPTH_TEST);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        program.bind();
        program.setUniform("view", Camera.createViewMatrix(camera));
        program.setUniform("shadowView", Camera.createViewMatrixLookingAt(shadowCamera.getPosition(), new Vector3f(camera.getPosition().x % 32, camera.getPosition().y % 32, camera.getPosition().z % 32)));
        program.setUniform("shadowProj", Camera.createOrthoProjectionMatrix(128));
        program.setUniform("lightDir", new Vector3f(shadowCameraPosition).normalize());
        program.setUniform("camPos", camera.getPosition());
        program.setUniform("camChunkPos", new Vector3i((int)(camera.getPosition().x / 32), (int)(camera.getPosition().y / 32), (int)(camera.getPosition().z / 32)));
        program.setUniform("dayTime", worldTime);
        program.setUniform("worldTime", (float) glfwGetTime() * 0.0625f);
        program.setUniform("light", lightPow);
        program.setUniform("skyColor", skyColor);
        glDisable(GL_BLEND);
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

        drawMenu(uiProgram, versionTexture);

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
                synchronized (Valkyrie.lock) {
                    ImGui.text("Chunks rendered: " + drawLength);
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
    }

    public void dispose() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        worldTimer.cancel();

        glfwDestroyWindow(Valkyrie.windowId);
    }

    public void resize(int width, int height) {
        program.bind();
        program.setUniform("proj", Camera.createProjectionMatrix(width, height));
        fbo.resize(width, height);
    }

    private void drawMenu(ShaderProgram uiProgram, Texture versionTexture) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        uiProgram.bind();
        uiProgram.setUniform("width", Valkyrie.width);
        uiProgram.setUniform("height", Valkyrie.height);

//        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, fontPositionBuffer);
//        glBufferData(GL_DRAW_INDIRECT_BUFFER, new int[] { 3, 1, 0, 0 }, GL_DYNAMIC_DRAW);

        var data = new ArrayList<Integer>();
        drawButton(0, 0, 256, 64, data);

        var array = new int[data.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = data.get(i);
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, fontIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, new int[] { 6, data.size() / 4, 0, 0 }, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, fontPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, array, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, fontPositionBuffer);

        glBindTexture(GL_TEXTURE_2D, versionTexture.getId());
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, 1, 0);

        fontProgram.bind();
        fontProgram.setUniform("width", Valkyrie.width);
        fontProgram.setUniform("height", Valkyrie.height);

        var data2 = new ArrayList<Integer>();
        drawButton(Valkyrie.width / 2 - 32 / 2, Valkyrie.height / 2 - 32 / 2, 32, 32, 254, data2);
        drawButton(Valkyrie.width / 2 - 96 / 2, Valkyrie.height - 96 - 20, 96, 96, 255, data2);

        for (int i = 0; i < 7; i++) {
            var id = selectedBlock - i + 3;
            if (id < 0 || id > 255) {
                continue;
            }
            drawButton(Valkyrie.width / 2 - 64 / 2 - (i - 3) * 70, Valkyrie.height - 64 - 20 - 16, 64, 64, BlockManager.getVoxelById(id).textureId, data2);
        }

        var array2 = new int[data2.size()];
        for (int i = 0; i < array2.length; i++) {
            array2[i] = data2.get(i);
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, fontIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, new int[] { 6, data2.size() / 5, 0, 0 }, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, fontPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, array2, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, fontPositionBuffer);

        glBindTexture(GL_TEXTURE_2D, texture.getId());
        glMultiDrawArraysIndirect(GL_TRIANGLES, 0, 1, 0);

        glDisable(GL_BLEND);
    }

    private static void drawButton(int x, int y, int width, int height, List<Integer> data) {
        data.add(x);
        data.add(y);
        data.add(width);
        data.add(height);
    }

    private static void drawButton(int x, int y, int width, int height, int texture, List<Integer> data) {
        data.add(x);
        data.add(y);
        data.add(width);
        data.add(height);
        data.add(texture);
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

    private int updateIndirectBuffer(GpuAllocator allocator, Vector3i camPos, int indirectBuffer, int chunkPositionBuffer, int shadowIndirectBuffer) {
        var indirectCmdsList = new ArrayList<Integer>();
        var chunkPositions = new ArrayList<Long>();
        var shadowIndirectCmdsList = new ArrayList<Integer>();
        allocator.getMeshes().forEach(mesh -> {
            var meshX = (mesh.getId() & 0xffff);
            var meshZ = (mesh.getId() >> 32) & 0xffff;

            if (!intersection.testAab(meshX * 32, 0, meshZ * 32, meshX * 32 + 32, 128, meshZ * 32 + 32)) {
                return;
            }

            indirectCmdsList.add(3);
            indirectCmdsList.add(mesh.getLength());
            indirectCmdsList.add(0);
            indirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));

            chunkPositions.add(mesh.getId());

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

    private void simulatePhysics(Camera camera, float delta, World world) {
        var chunk = world.getChunk((int) Math.floor(camera.getPosition().x / 32f), (int) Math.floor(camera.getPosition().z / 32f));
        if (chunk == null)
            return;

        var dimensions = new Vector3f(0.3f, 1.5f, 0.3f);
        var oldMovementY = camera.movement.y;
        camera.movement.y += -1 * delta * 30f;

        var position = camera.getPosition().sub(0, 1.5f, 0);

        position.x += (float) camera.movement.x;
        checkCollisions(new Vector3f((float) camera.movement.x, 0, 0), position, dimensions, camera.movement);

        position.y += (float) (camera.movement.y + oldMovementY) / 2 * delta;
        checkCollisions(new Vector3f(0, (float) camera.movement.y, 0), position, dimensions, camera.movement);

        position.z += (float) camera.movement.z;
        checkCollisions(new Vector3f(0, 0, (float) camera.movement.z), position, dimensions, camera.movement);

        camera.setPosition(position.add(0, 1.5f, 0));

//        camera.movement = new Vector3d();
//        camera.movement.y = movement.y;
    }

    private void checkCollisions(Vector3f vel, Vector3f position, Vector3f dimensions, Vector3d movement) {
        for (float x = position.x - dimensions.x; x <= position.x + dimensions.x; x += dimensions.x) {
            for (float y = position.y; y <= position.y + dimensions.y; y += dimensions.y / 2f) {
                for (float z = position.z - dimensions.z; z <= position.z + dimensions.z; z += dimensions.z) {
                    var voxel = world.getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));

                    if (voxel != 0) {
                        if (vel.y > 0) {
                            position.y = (int)y - dimensions.y - 1 / 128f;
                            movement.y = 0;
                        }
                        else if (vel.y < 0) {
//                            contact = true;
                            position.y = (int)y + 1;
                            movement.y = 0;
                        }

                        if (vel.x > 0) {
                            position.x = (int)Math.floor(x) - dimensions.x - 1 / 128f;
                        }
                        else if (vel.x < 0) {
                            position.x = (int)Math.floor(x) + dimensions.x + 1 + 1 / 128f;
                        }

                        if (vel.z > 0) {
                            position.z = (int)Math.floor(z) - dimensions.z - 1 / 128f;
                        }
                        else if (vel.z < 0) {
                            position.z = (int)Math.floor(z) + dimensions.z + 1 + 1 / 128f;
                        }
                    }
                }
            }
        }
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
