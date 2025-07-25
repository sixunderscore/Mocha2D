package net.sixunderscore.mocha2d.input;

import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

public class MouseListener {
    private final Vector2d mousePos = new Vector2d();
    private double scrollXOffset, scrollYOffset;
    private boolean leftButtonPressed = false;
    private boolean rightButtonPressed = false;

    public MouseListener(long window) {
        GLFW.glfwSetMouseButtonCallback(window, this::handleMouseButtons);
        GLFW.glfwSetCursorPosCallback(window, this::handleCursorMovement);
        GLFW.glfwSetScrollCallback(window, this::handleScrollWheel);
    }

    private void handleMouseButtons(long window, int button, int action, int mods) {
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
    }

    private void handleCursorMovement(long window, double xPosition, double yPosition) {
        this.mousePos.set(xPosition, yPosition);
    }

    private void handleScrollWheel(long window, double xOffset, double yOffset) {
        this.scrollXOffset = xOffset;
        this.scrollYOffset = yOffset;
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
