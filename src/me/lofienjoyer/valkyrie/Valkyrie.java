package me.lofienjoyer.valkyrie;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawArraysIndirect;

public class Valkyrie {

    public static void main(String[] args) throws IOException {
        if (!glfwInit()) {
            System.err.println("Error loading glfw!");
            return;
        }

        long windowId = glfwCreateWindow(1280, 720, "Valkyrie", 0, 0);
        glfwMakeContextCurrent(windowId);

        final var vsync = 0;
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
            allocator = new SsboAllocator(1);
            program = new ShaderProgram("vertexSSBO.glsl", "fragment.glsl");
        } else {
            program = new ShaderProgram("vertex.glsl", "fragment.glsl");
            allocator = new VboAllocator(vaoId);
        }
        program.bind();

        var chunks = new ArrayList<Chunk>();
        var executor = Executors.newSingleThreadExecutor();


        var drawLength = updateIndirectBuffer(allocator);

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

        while (!glfwWindowShouldClose(windowId)) {
            camera.update(windowId, delta);
            program.setUniform("view", Camera.createViewMatrix(camera));

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

        glfwDestroyWindow(windowId);

        System.out.println("Average frame time: " + ((float) counter / 1000000f) / frames + "ms");
    }

    private static void generateWorld(ExecutorService executor, List<Chunk> chunks) {
        var chunkCount = 1024 * 8;
        var noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        noise.SetFrequency(1 / 256f);
        for (int i = 0; i < chunkCount; i++) {
            var chunk = new Chunk();
            int finalI = i;
            chunk.setMeshFuture(executor.submit(() -> {
                byte[] chunkData = new byte[32 * 32 * 32];
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        var chunkX = ((finalI / 32) % 32) * 32;
                        var chunkY = (finalI / 1024) * 32;
                        var chunkZ = (finalI % 32) * 32;
                        var height = (noise.GetNoise(x + chunkX, z + chunkZ) + 1) / 2f;
                        height *= 224;
                        height -= chunkY - 32;
                        for (int y = 0; y < Math.min(height, 32); y++) {
                            chunkData[x | y << 5 | z << 10] = 1;
                        }
                    }
                }
                chunk.setData(chunkData);
                chunk.setPosition(new Vector3i((finalI / 32) % 32, finalI / 1024, finalI % 32));
                return new GreedyMesher(chunkData).compute();
            }));

            chunks.add(chunk);
        }
    }

    private static int updateIndirectBuffer(GpuAllocator allocator) {
        var indirectCmdsList = new ArrayList<Integer>();
        var chunkCount = 1024 * 8;
        var meshes = new ArrayList<MeshInstance>();
        var chunkPositions = new ArrayList<Integer>();
        var noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Value);
        noise.SetFrequency(1 / 256f);
        var timer = System.nanoTime();
        for (int i = 0; i < chunkCount; i++) {
            byte[] chunkData = new byte[32 * 32 * 32];
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    var chunkX = ((i / 32) % 32) * 32;
                    var chunkY = (i / 1024) * 32;
                    var chunkZ = (i % 32) * 32;
                    var height = (noise.GetNoise(x + chunkX, z + chunkZ) + 1) / 2f;
                    height *= 224;
                    height -= chunkY - 32;
                    for (int y = 0; y < Math.min(height, 32); y++) {
                        chunkData[x | y << 5 | z << 10] = 1;
                    }
                }
            }
            var positionsList = new GreedyMesher(chunkData).compute();
            var chunkMesh = allocator.store(integerListToArray(positionsList));
            meshes.add(chunkMesh);
            var position = getData((i / 32) % 32, i / 1024, i % 32);
            chunkPositions.add(position);
        }
        System.out.println((System.nanoTime() - timer) / 1000000f);

        System.out.println(allocator.getFirstFreePosition());

        for (int i = 0; i < meshes.size(); i++) {
            var mesh = meshes.get(i);
            indirectCmdsList.add(3);
            indirectCmdsList.add(mesh.getLength());
            indirectCmdsList.add(0);
            indirectCmdsList.add(mesh.getIndex() / (Integer.BYTES * 2));
        }

        int[] indirectCmds = new int[indirectCmdsList.size()];
        for (int i = 0; i < indirectCmdsList.size(); i++) {
            indirectCmds[i] = indirectCmdsList.get(i);
        }

        int indirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, indirectCmds, GL_STATIC_DRAW);

        int[] chunkPositionsArray = new int[chunkPositions.size()];
        for (int i = 0; i < chunkPositions.size(); i++) {
            chunkPositionsArray[i] = chunkPositions.get(i);
        }

        int chunkPositionBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunkPositionBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPositionsArray, GL_STATIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunkPositionBuffer);

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
