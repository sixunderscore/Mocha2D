package net.sixunderscore.mocha2d.input;

import net.sixunderscore.mocha2d.Window;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

public class KeyListener {
    private final Set<Integer> pressedKeys;
    private final Queue<Character> typedChars;
    private float timeSinceLastCharTyped;

    public KeyListener(long window) {
        this.pressedKeys = new HashSet<>();
        this.typedChars = new ArrayDeque<>();
        this.timeSinceLastCharTyped = 0;

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
        this.timeSinceLastCharTyped = 0;
    }

    public void timedClearCharBuffer() {
        if (!this.typedChars.isEmpty()) {
            this.timeSinceLastCharTyped += Window.getDeltaTime();

            if (this.timeSinceLastCharTyped >= 1f) {
                this.typedChars.clear();
                this.timeSinceLastCharTyped = 0;
            }
        }
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

    public void consumeAllTypedChars(Consumer<Character> consumer) {
        while (!this.typedChars.isEmpty()) {
            consumer.accept(this.typedChars.poll());
        }
    }

    public boolean hasTypedChars() {
        return !this.typedChars.isEmpty();
    }
}
