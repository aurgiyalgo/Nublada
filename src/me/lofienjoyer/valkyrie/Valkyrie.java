package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31C.GL_MAX_UNIFORM_BLOCK_SIZE;
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

        executorService = Executors.newFixedThreadPool(1);
        var world = new World();

        generateWorld(world);

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
        Vector3i lastPos = null;
        boolean wireframe = false;

        while (!glfwWindowShouldClose(windowId)) {
            camera.update(windowId, delta);
            program.setUniform("view", Camera.createViewMatrix(camera));

            var chunks = world.getChunks();
            checkFutures(chunks, allocator);
            var drawLength = updateIndirectBuffer(chunks, indirectBuffer, chunkPositionBuffer);

            var pos = camera.getPosition();
            var currentPos = new Vector3i((int) Math.floor(pos.x / 32), (int) Math.floor(pos.y / 32), (int) Math.floor(pos.z / 32));
            if (!currentPos.equals(lastPos)) {
                lastPos = currentPos;

                System.out.println("last pos");
                var currentChunk = world.getChunk(currentPos.x, currentPos.y, currentPos.z);
                if (currentChunk != null) {
                    currentChunk.setDirty(true);
                }
            }

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

    private static void generateWorld(World world) {
        final var worldSide = 8;
        final var worldHeight = 8;
        var chunkCount = worldSide * worldSide * worldHeight;
        for (int i = 0; i < chunkCount; i++) {
            world.loadChunk((i / worldSide) % worldSide, i / (worldSide * worldSide), i % worldSide);
        }
    }

    private static void checkFutures(Collection<Chunk> chunks, GpuAllocator allocator) {
        chunks.forEach(chunk -> {

            if (chunk.isDirty()) {
                var meshFuture = chunk.getMeshFuture();
                if (meshFuture != null)
                    meshFuture.cancel(true);

                chunk.setMeshFuture(executorService.submit(() -> {
                    return new GreedyMesher(chunk, chunk.getWorld()).compute();
                }));
                chunk.setDirty(false);
            }

            if (chunk.getMeshFuture() == null || !chunk.getMeshFuture().isDone())
                return;

            try {
                var positionsList = chunk.getMeshFuture().get();
                updateChunkMesh(allocator, chunk, positionsList);
                chunk.setMeshFuture(null);
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
        var chunkPositions = new ArrayList<Integer>();
        for (Chunk chunk : chunks) {
            var mesh = chunk.getMesh();
            if (mesh == null)
                continue;

            indirectCmdsList.add(3);
            indirectCmdsList.add(mesh.getLength());
            indirectCmdsList.add(0);
            indirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));
            var position = chunk.getPosition();
            chunkPositions.add(getData(position.x, position.y, position.z));
        }

        int[] chunkPositionsArray = new int[chunkPositions.size()];
        for (int i = 0; i < chunkPositions.size(); i++) {
            chunkPositionsArray[i] = chunkPositions.get(i);
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
