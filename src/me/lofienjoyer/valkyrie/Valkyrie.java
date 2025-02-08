package me.lofienjoyer.valkyrie;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Valkyrie {

    private static final SimpleDateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");

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

            if (Input.isKeyJustPressed(GLFW_KEY_F2))
                takeScreenshot();

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

    private static void takeScreenshot() {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var buffer = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        new Thread(() -> {
            for (int x = image.getWidth() - 1; x >= 0; x--) {
                for (int y = image.getHeight() - 1; y >= 0; y--) {
                    int i = (x + width * y) * 4;
                    image.setRGB(x, image.getHeight() - 1 - y, (((buffer.get(i) & 0xFF) & 0x0ff) << 16) | (((buffer.get(i + 1) & 0xFF) & 0x0ff) << 8) | ((buffer.get(i + 2) & 0xFF) & 0x0ff));
                }
            }
            var calendar = Calendar.getInstance();
            var date = Date.from(calendar.toInstant());
            var fileName = SCREENSHOT_DATE_FORMAT.format(date) + ".png";
            var screenshotsFolder = new File("screenshots");
            if (!screenshotsFolder.exists())
                screenshotsFolder.mkdir();
            try {
                ImageIO.write(image, "png", Paths.get("screenshots", fileName).toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

}
