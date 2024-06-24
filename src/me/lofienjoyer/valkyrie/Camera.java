package me.lofienjoyer.valkyrie;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Camera {

    private static final float SPEED = 128;

    private final Vector3f position;
    private float rotationY, rotationX, roll;

    boolean mouseLocked = false;
    double newX = 320;
    double newY = 180;

    double prevX = 0;
    double prevY = 0;

    boolean rotX = false;
    boolean rotY = false;

    private final Vector3f direction;

    public Camera() {
        this.position = new Vector3f(0, 33.8f, 0);
        this.direction = new Vector3f();
        updateDirection();
    }

    public void updateDirection() {
        float yaw = (float) Math.toRadians(rotationX + 90);
        float pitch = (float) Math.toRadians(rotationY);

        direction.x = (float) (Math.cos(yaw) * Math.cos(pitch));
        direction.y = (float) Math.sin(pitch);
        direction.z = (float) (Math.cos(pitch) * Math.sin(yaw));

        direction.normalize().mul(-1);
    }

    public void update(long window, float delta) {
        if (GLFW.glfwGetKey(window, GLFW_KEY_ESCAPE) == 1) {
            mouseLocked = false;
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        if (GLFW.glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS && !mouseLocked) {
            GLFW.glfwSetCursorPos(window, 320, 180);
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);

            mouseLocked = true;
        }

        if (mouseLocked){
            DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer y = BufferUtils.createDoubleBuffer(1);

            GLFW.glfwGetCursorPos(window, x, y);
            x.rewind();
            y.rewind();

            newX = x.get();
            newY = y.get();

            double deltaX = newX - 320;
            double deltaY = newY - 180;

            rotX = newX != prevX;
            rotY = newY != prevY;

            prevX = newX;
            prevY = newY;

            GLFW.glfwSetCursorPos(window, 320, 180);

            rotationX += (float) deltaX * 0.05f;
            rotationY += (float) deltaY * 0.05f;

            rotationX = rotationX % 360;
            rotationY = Math.min(Math.max(-90, rotationY), 90);
        }

        updateDirection();

        if (glfwGetKey(window, GLFW_KEY_W) != 0) {
            position.z -= Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            position.x += Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_S) != 0) {
            position.z += Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            position.x -= Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_A) != 0) {
            position.x -= Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            position.z -= Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_D) != 0) {
            position.x += Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            position.z += Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) != 0) {
            position.y += delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) != 0) {
            position.y -= delta * SPEED;
        }
    }

    public void move(float x, float y, float z) {
        position.x += x;
        position.y += y;
        position.z += z;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = position.z;
    }

    public float getRotationY() {
        return rotationY;
    }

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
    }

    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
    }

    public float getRoll() {
        return roll;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public static Matrix4f createViewMatrix(Camera camera) {
        var matrix = new Matrix4f();
        matrix.identity();
        matrix.rotate((float) Math.toRadians(camera.getRotationY()), new Vector3f(1, 0, 0));
        matrix.rotate((float) Math.toRadians(camera.getRotationX()), new Vector3f(0, 1, 0));
        Vector3f cameraPos = camera.getPosition();
        Vector3f negativeCameraPos = new Vector3f(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        matrix.translate(negativeCameraPos);
        return matrix;
    }

    public static Matrix4f createProjectionMatrix(int width, int height) {
        var projectionMatrix = new Matrix4f();
        projectionMatrix.perspective((float) Math.toRadians(80), width / (float)height, 0.25f, 4096f);
        return projectionMatrix;
    }

}
