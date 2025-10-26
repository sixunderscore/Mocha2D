package net.sixunderscore.mocha2d.input;

import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

public class MouseListener {
    private final Vector2d mousePos;
    private double scrollXOffset;
    private double scrollYOffset;
    private boolean leftButtonPressed;
    private boolean rightButtonPressed;

    public MouseListener(long window) {
        this.mousePos = new Vector2d();
        this.scrollXOffset = 0;
        this.scrollYOffset = 0;
        this.leftButtonPressed = false;
        this.rightButtonPressed = false;

        GLFW.glfwSetCursorPosCallback(window, (window2, xPos, yPos) -> this.mousePos.set(xPos, yPos));
        GLFW.glfwSetMouseButtonCallback(window, (window2, button, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    this.leftButtonPressed = true;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                    this.rightButtonPressed = true;
                }
            } else if (action == GLFW.GLFW_RELEASE) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    this.leftButtonPressed = false;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                    this.rightButtonPressed = false;
                }
            }
        });
        GLFW.glfwSetScrollCallback(window, (window2, xOffset, yOffset) -> {
            this.scrollXOffset = xOffset;
            this.scrollYOffset = yOffset;
        });
    }

    public double xPos() {
        return this.mousePos.x;
    }

    public double yPos() {
        return this.mousePos.y;
    }

    public Vector2d pos() {
        return this.mousePos;
    }

    public boolean leftButtonPressed() {
        return this.leftButtonPressed;
    }

    public boolean rightButtonPressed() {
        return this.rightButtonPressed;
    }

    public double scrollXOffset() {
        double offset = this.scrollXOffset;
        this.scrollYOffset = 0;
        return offset;
    }

    public double scrollYOffset() {
        double offset = this.scrollYOffset;
        this.scrollYOffset = 0;
        return offset;
    }
}
