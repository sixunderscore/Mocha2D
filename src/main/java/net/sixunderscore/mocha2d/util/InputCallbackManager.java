package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.Screen;
import net.sixunderscore.mocha2d.graphics.Window;
import org.lwjgl.glfw.*;

public class InputCallbackManager {
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWCharCallback charCallback;

    public InputCallbackManager(long window, Screen initialScreen) {
        this.setCallbacks(window, initialScreen);
    }

    public void setCallbacks(long window, Screen screen) {
        this.cleanUp();

        // Mouse
        // We flip cursor y coordinate
        this.cursorPosCallback = GLFW.glfwSetCursorPosCallback(window, (window2, xPos, yPos) -> {
            double fbX = xPos * Window.getXScale();
            double fbY = yPos * Window.getYScale();
            screen.onMouseMoved(fbX, (Window.getHeight() - fbY));
        });
        this.mouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, (window2, button, action, mods) -> screen.onMouseClicked(button, action, mods));
        this.scrollCallback = GLFW.glfwSetScrollCallback(window, (window2, xOffset, yOffset) -> screen.onMouseScrolled(xOffset, yOffset));

        // Keyboard
        this.keyCallback = GLFW.glfwSetKeyCallback(window, (window2, keycode, scancode, action, mods) -> screen.onKeyPressed(keycode, scancode, action, mods));
        this.charCallback = GLFW.glfwSetCharCallback(window, (window2, codepoint) -> screen.onCharTyped(codepoint));
    }

    private void cleanUp() {
        if (this.cursorPosCallback != null) {
            this.cursorPosCallback.close();
        }
        if (this.mouseButtonCallback != null) {
            this.mouseButtonCallback.close();
        }
        if (this.scrollCallback != null) {
            this.scrollCallback.close();
        }
        if (this.keyCallback != null) {
            this.keyCallback.close();
        }
        if (this.charCallback != null) {
            this.charCallback.close();
        }
    }
}
