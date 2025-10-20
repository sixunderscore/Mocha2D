package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.input.KeyListener;
import net.sixunderscore.mocha2d.input.MouseListener;

public interface Screen {
    void init(ResourceManager resourceManager);

    void update(KeyListener keyListener, MouseListener mouseListener);

    void render(BatchRenderer batch);

    default void cleanUp() {}
}
