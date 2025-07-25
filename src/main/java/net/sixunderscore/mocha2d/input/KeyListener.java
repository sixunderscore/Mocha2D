package net.sixunderscore.mocha2d.input;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeyListener {
    private final Map<Integer, Integer> pressedKeysAndMods = new HashMap<>();

    public KeyListener(long window) {
        GLFW.glfwSetKeyCallback(window, this::handleKeyInput);
    }

    private void handleKeyInput(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            this.pressedKeysAndMods.put(key, mods);
        }
        else if (action == GLFW.GLFW_RELEASE) {
            this.pressedKeysAndMods.remove(key);
        }
    }

    public boolean isKeyPressed(int keyCode) {
        return this.pressedKeysAndMods.containsKey(keyCode);
    }

    public Integer getModifiers(int keyCode) {
        return this.pressedKeysAndMods.getOrDefault(keyCode, 0);
    }

    public Set<Integer> getPressedKeys() {
        return this.pressedKeysAndMods.keySet();
    }

    public boolean isAnyKeyPressed() {
        return !this.pressedKeysAndMods.isEmpty();
    }
}
