package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;

public interface Screen {
    void init(ResourceManager resourceManager);
    default void onMouseMoved(double xPos, double yPos) {}
    default void onMouseClicked(int button, int action, int mods) {}
    default void onMouseScrolled(double xOffset, double yOffset) {}
    default void onKeyPressed(int keycode, int scancode, int action, int mods) {}
    default void onCharTyped(int codepoint) {}
    void render(BatchRenderer batch);
    default void cleanUp() {}
}
