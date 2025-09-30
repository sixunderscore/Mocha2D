package net.sixunderscore.mocha2d.input;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class KeyListener {
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Queue<Character> typedChars = new ArrayDeque<>();

    public KeyListener(long window) {
        GLFW.glfwSetKeyCallback(window, this::handleKeyInput);
        GLFW.glfwSetCharCallback(window, this::handleCharTyping);
    }

    private void handleKeyInput(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            this.pressedKeys.add(key);
        }
        else if (action == GLFW.GLFW_RELEASE) {
            this.pressedKeys.remove(key);
        }
    }

    private void handleCharTyping(long window, int codepoint) {
        this.typedChars.add((char) codepoint);
    }

    public boolean isKeyPressed(int keyCode) {
        return this.pressedKeys.contains(keyCode);
    }

    public Set<Integer> getPressedKeys() {
        return this.pressedKeys;
    }

    public boolean isAnyKeyPressed() {
        return !this.pressedKeys.isEmpty();
    }

    public char getTypedChar() {
        return this.typedChars.isEmpty() ? '\0' : this.typedChars.poll();
    }
}
