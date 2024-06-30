package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawArraysIndirect;

public class Valkyrie {

    public static ExecutorService executorService;

    public static void main(String[] args) throws IOException {
        if (!glfwInit()) {
            System.err.println("Error loading glfw!");
            return;
        }

        long windowId = glfwCreateWindow(1280, 720, "Valkyrie", 0, 0);
        glfwMakeContextCurrent(windowId);

        final var vsync = 1;
        System.out.println("Vsync: " + (vsync == 1));
        glfwSwapInterval(vsync);
        GL.createCapabilities();

        glClearColor(1, 0, 0.75f, 1);

        var shouldUseSsboRendering = shouldUseSsboRendering(args);
        System.out.println("Using SSBO: " + shouldUseSsboRendering);

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
            allocator = new SsboAllocator(1, 16);
            program = new ShaderProgram("vertexSSBO.glsl", "fragment.glsl");
        } else {
            program = new ShaderProgram("vertex.glsl", "fragment.glsl");
            allocator = new VboAllocator(vaoId, 16);
        }
        program.bind();

        var texture = new Texture("res/textures/blocks/tiles.png");
        glBindTexture(GL_TEXTURE_2D, texture.getId());

        int indirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);

        int chunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);

        executorService = Executors.newFixedThreadPool(3);
        var world = new World();

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        long timer = System.nanoTime();
        long counter = 0;
        long frames = 0;

        var camera = new Camera();
        program.setUniform("proj", Camera.createProjectionMatrix(1280, 720));

        glfwSetWindowSizeCallback(windowId, (id, width, height) -> {
            glViewport(0, 0, width, height);
            program.setUniform("proj", Camera.createProjectionMatrix(width, height));
        });

        var delta = 1 / 60f;
        boolean wireframe = false;

        while (!glfwWindowShouldClose(windowId)) {
            camera.update(windowId, delta);
            program.setUniform("view", Camera.createViewMatrix(camera));

            if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
                var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, false);
                if (position != null) {
                    world.setBlock(0, position);
                }
            }

            if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS) {
                var position = world.rayCast(camera.getPosition(), camera.getDirection(), 256, true);
                if (position != null) {
                    world.setBlock(5, position);
                }
            }

            updateWorld(world, camera);
            var chunks = world.getChunks();
            checkFutures(chunks, allocator, world);
            var drawLength = updateIndirectBuffer(chunks, indirectBuffer, chunkPositionBuffer);

            if (glfwGetKey(windowId, GLFW_KEY_P) == 1)
                wireframe = !wireframe;

            glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glMultiDrawArraysIndirect(GL_TRIANGLE_FAN, 0, drawLength, 0);

            glfwPollEvents();
            glfwSwapBuffers(windowId);
//            System.out.println(1 / ((System.nanoTime() - timer) / 1000000000f));
            counter += (System.nanoTime() - timer);
            delta = (System.nanoTime() - timer) / 1000000000f;
            timer = System.nanoTime();
            frames++;
        }

        executorService.shutdownNow();

        glfwDestroyWindow(windowId);

        System.out.println("Average frame time: " + ((float) counter / 1000000f) / frames + "ms");
    }

    private static void updateWorld(World world, Camera camera) {
        final var worldSide = 16;
        final var worldHeight = 8;
        var cameraX = (int)Math.floor(camera.getPosition().x / 32);
        var cameraY = (int)Math.floor(camera.getPosition().y / 32);
        var cameraZ = (int)Math.floor(camera.getPosition().z / 32);
        world.getChunks().stream().toList().forEach(chunk -> {
            var position = chunk.getPosition();
            if (Math.abs(position.x - cameraX) > worldSide/2 + 2 || Math.abs(position.z - cameraZ) > worldSide/2 + 2 || Math.abs(position.y - cameraY) > worldHeight/2 + 2)
                world.unloadChunk(position.x, position.y, position.z);
        });

        for (int x = -worldSide/2; x <= worldSide/2; x++) {
            for (int z = -worldSide/2; z <= worldSide/2; z++) {
                for (int y = -worldHeight/2; y <= worldHeight/2; y++) {
                    var chunkX = cameraX - x;
                    var chunkY = cameraY - y;
                    var chunkZ = cameraZ - z;
                    var chunk = world.getChunk(chunkX, chunkY, chunkZ);
                    if (chunk == null)
                        world.loadChunk(chunkX, chunkY, chunkZ);
                }
            }
        }
//        var chunkCount = worldSide * worldSide * worldHeight;
//        for (int i = 0; i < chunkCount; i++) {
//            world.loadChunk((i / worldSide) % worldSide - worldSide / 2, i / (worldSide * worldSide), i % worldSide - worldSide / 2);
//        }
    }

    private static void checkFutures(Collection<Chunk> chunks, GpuAllocator allocator, World world) {
        chunks.forEach(chunk -> {
            if (chunk.isDirty()) {
                if (!chunk.isPriority()) {
                    chunk.getFutures().forEach(future -> future.cancel(true));
                    chunk.getFutures().clear();
                }
                chunk.getFutures().add(executorService.submit(() -> {
                    return new GreedyMesher(chunk, chunk.getWorld()).compute();
                }));
                chunk.setDirty(false);
            }

            var futures = chunk.getFutures();
            if (futures.isEmpty())
                return;

            var firstFuture = chunk.getFutures().getFirst();
            if (!firstFuture.isDone())
                return;

            try {
                var positionsList = firstFuture.get();
                updateChunkMesh(allocator, chunk, positionsList);
                futures.removeFirst();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void updateChunkMesh(GpuAllocator allocator, Chunk chunk, List<Integer> positionsList) {
        var chunkMesh = chunk.getMesh();
        if (chunkMesh == null) {
            chunk.setMesh(allocator.store(integerListToArray(positionsList)));
        } else {
            allocator.update(chunkMesh, integerListToArray(positionsList));
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

    private static int[] integerListToArray(List<Integer> list) {
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
