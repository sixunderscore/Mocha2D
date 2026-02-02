package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;

public interface Screen {
    void init();
    default void onMouseMoved(double xPos, double yPos) {}
    default void onMouseClicked(int button, int action, int mods) {}
    default void onMouseScrolled(double xOffset, double yOffset) {}
    default void onKeyPressed(int keycode, int scancode, int action, int mods) {}
    default void onCharTyped(int codepoint) {}
    default void onWindowResized() {}
    void render(BatchRenderer batch);
    default void cleanUp() {}
}
