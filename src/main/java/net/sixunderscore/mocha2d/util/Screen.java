package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;

public interface Screen {
    void init(ResourceManager resourceManager);
    default void onMouseMoved(float xPos, float yPos) {}
    default void onMouseClicked(byte button, boolean pressed) {}
    default void onMouseScrolled(float xOffset, float yOffset) {}
    default void onKeyPressed(int keycode, int scancode, boolean pressed, short mods) {}
    default void onTextTyped(String text) {}
    default void onWindowResized() {}
    void render(BatchRenderer batch);
    default void cleanUp() {}
}
