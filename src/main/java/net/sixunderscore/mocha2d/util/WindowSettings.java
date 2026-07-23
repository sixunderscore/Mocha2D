package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;

public class WindowSettings {
    private String windowName;
    private String windowIconPath;
    private int initialWidth;
    private int initialHeight;
    private boolean resizeable;
    private Screen initialScreen;

    public WindowSettings() {
        this.windowName = "Mocha2D Window";
        this.windowIconPath = "";
        this.initialWidth = 800;
        this.initialHeight = 500;
        this.resizeable = true;
        this.initialScreen = new Screen() {
            @Override
            public void init(ResourceManager resourceManager) {}

            @Override
            public void render(BatchRenderer batch) {}
        };
    }

    public WindowSettings setWindowName(String windowName) {
        this.windowName = windowName;
        return this;
    }

    public WindowSettings setWindowIcon(String path) {
        this.windowIconPath = path;
        return this;
    }

    public WindowSettings setDimensions(int initialWidth, int initialHeight) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        return this;
    }

    public WindowSettings setResizeable(boolean resizeable) {
        this.resizeable = resizeable;
        return this;
    }

    public WindowSettings setInitialScreen(Screen initialScreen) {
        this.initialScreen = initialScreen;
        return this;
    }

    public String getWindowName() {
        return this.windowName;
    }

    public String getWindowIconPath() {
        return this.windowIconPath;
    }

    public int getInitialWidth() {
        return this.initialWidth;
    }

    public int getInitialHeight() {
        return this.initialHeight;
    }

    public boolean isResizeable() {
        return this.resizeable;
    }

    public Screen getInitialScreen() {
        return this.initialScreen;
    }
}
