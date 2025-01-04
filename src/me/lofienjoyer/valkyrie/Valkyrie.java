package me.lofienjoyer.valkyrie;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class Valkyrie {

    public static ExecutorService executorService;

    public static int width = 1280, height = 720;

    public static final Object lock = new Object();

    public static long windowId;

    private static Scene scene;

    public static void main(String[] args) {
        final var vsync = 1;
        System.out.println("Vsync: " + (vsync != 0));

        if (!glfwInit()) {
            System.err.println("Error loading glfw!");
            return;
        }

        windowId = glfwCreateWindow(Valkyrie.width, Valkyrie.height, "Valkyrie", 0, 0);
        glfwWindowHint(GLFW_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_VERSION_MINOR, 6);
        glfwMakeContextCurrent(windowId);
        glfwSwapInterval(vsync);
        GL.createCapabilities();

        glfwSetWindowSizeCallback(Valkyrie.windowId, (id, width, height) -> {
            Valkyrie.width = width;
            Valkyrie.height = height;
            glViewport(0, 0, width, height);
            if (scene != null)
                scene.resize(width, height);
        });

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2 - 1);

        long timer, counter = 0, frames = 0;
        timer = System.nanoTime();
        float delta = 1 / 60f;

        var input = Input.getInstance();
        input.setup(windowId);

        scene = new WorldScene();
        scene.init();

        while (!GLFW.glfwWindowShouldClose(windowId)) {
            scene.draw(delta);

            input.update();
            glfwPollEvents();
            glfwSwapBuffers(windowId);
            counter += (System.nanoTime() - timer);
            delta = (System.nanoTime() - timer) / 1000000000f;
            timer = System.nanoTime();
            frames++;
        }

        executorService.shutdownNow();
        scene.dispose();
        System.out.println("Average frame time: " + ((float) counter / 1000000f) / frames + "ms");
    }

}
