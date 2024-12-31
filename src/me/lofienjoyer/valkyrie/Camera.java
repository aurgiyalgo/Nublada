package me.lofienjoyer.valkyrie;

import imgui.ImGui;
import imgui.flag.ImGuiMouseCursor;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Camera {

    private static final float SPEED = 5;

    private final Vector3d position;
    private double rotationY, rotationX, roll;
    public Vector3d movement;

    boolean mouseLocked = false;
    double newX = 320;
    double newY = 180;

    double prevX = 0;
    double prevY = 0;

    boolean rotX = false;
    boolean rotY = false;

    private final Vector3f direction;

    public Camera() {
        this.position = new Vector3d(2 * 32 * 32, 128, 2 * 32 * 32);
        this.direction = new Vector3f();
        this.movement = new Vector3d();
        rotationY = 90;
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
            GLFW.glfwSetCursorPos(window, 640, 360);
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

            double deltaX = newX - 640;
            double deltaY = newY - 360;

            rotX = newX != prevX;
            rotY = newY != prevY;

            prevX = newX;
            prevY = newY;

            GLFW.glfwSetCursorPos(window, 640, 360);

            rotationX += deltaX * 0.05f;
            rotationY += deltaY * 0.05f;

            rotationX = rotationX % 360;
            rotationY = Math.min(Math.max(-90, rotationY), 90);
        }

        updateDirection();

        if (glfwGetKey(window, GLFW_KEY_W) != 0) {
            movement.z -= Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            movement.x += Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_S) != 0) {
            movement.z += Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            movement.x -= Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_A) != 0) {
            movement.x -= Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            movement.z -= Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

        if (glfwGetKey(window, GLFW_KEY_D) != 0) {
            movement.x += Math.cos(Math.toRadians(rotationX)) * delta * SPEED;
            movement.z += Math.sin(Math.toRadians(rotationX)) * delta * SPEED;
        }

//        if (glfwGetKey(window, GLFW_KEY_SPACE) != 0) {
//            movement.y += delta * SPEED;
//        }
//
//        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) != 0) {
//            movement.y -= delta * SPEED;
//        }
    }

    public void move(float x, float y, float z) {
        position.x += x;
        position.y += y;
        position.z += z;
    }

    public Vector3f getPosition() {
        return new Vector3f((float) position.x, (float) position.y, (float) position.z);
    }

    public void setPosition(Vector3f position) {
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = position.z;
    }

    public double getRotationY() {
        return rotationY;
    }

    public double getRotationX() {
        return rotationX;
    }

    public void setRotationX(double rotationX) {
        this.rotationX = rotationX;
    }

    public void setRotationY(double rotationY) {
        this.rotationY = rotationY;
    }

    public double getRoll() {
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
        var cameraPos = camera.position;
        Vector3f negativeCameraPos = new Vector3f((float) (-cameraPos.x % 32), (float) (-cameraPos.y % 32), (float) (-cameraPos.z % 32));
        matrix.translate(negativeCameraPos);
        return matrix;
    }

    public static Matrix4f createViewMatrixLookingAt(Vector3f position, Vector3f lookAt) {
        var matrix = new Matrix4f();
        return matrix.lookAt(position, new Vector3f(), new Vector3f(0, 1, 0));
    }

    public static Matrix4f createProjectionMatrix(int width, int height) {
        var projectionMatrix = new Matrix4f();
        projectionMatrix.perspective((float) Math.toRadians(80), width / (float)height, 0.25f, 4096f);
        return projectionMatrix;
    }

    public static Matrix4f createOrthoProjectionMatrix(int side) {
        var projectionMatrix = new Matrix4f();
        projectionMatrix.ortho(-side/2, side/2, -side/2, side/2, 0.25f, 1024f);
        return projectionMatrix;
    }

}
