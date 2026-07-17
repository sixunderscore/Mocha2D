package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;

public interface RenderBackend extends AutoCloseable {
    void onWindowResize();
    void render(OrthographicCamera camera, Screen screen);
    ResourceManager getResourceManager();
    void setClearColor(byte r, byte g, byte b);
    void close();
}
