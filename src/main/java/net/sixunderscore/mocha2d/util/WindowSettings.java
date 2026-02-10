package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;

public class WindowSettings {
    private TextureFile[] textureFiles;
    private TtfFile[] ttfFiles;
    private String windowName;
    private String windowIconPath;
    private int initialWidth;
    private int initialHeight;
    private boolean resizeable;
    private int fpsCap;
    private Color clearColor;

    public WindowSettings() {
        this.textureFiles = new TextureFile[0];
        this.ttfFiles = new TtfFile[0];
        this.windowName = "Mocha2D Window";
        this.windowIconPath = "";
        this.initialWidth = 800;
        this.initialHeight = 500;
        this.resizeable = true;
        this.fpsCap = 1000;
        this.clearColor = new Color((byte) 0, (byte) 0, (byte) 0);
    }

    public void setTextureFiles(TextureFile... textureFiles) {
        this.textureFiles = textureFiles;
    }

    public void setTtfFiles(TtfFile... ttfFiles) {
        this.ttfFiles = ttfFiles;
    }

    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    public void setWindowIcon(String path) {
        this.windowIconPath = path;
    }

    public void setDimensions(int initialWidth, int initialHeight) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
    }

    public void setResizeable(boolean resizeable) {
        this.resizeable = resizeable;
    }

    public void setFPSCap(int fpsCap) {
        this.fpsCap = fpsCap;
    }

    public void setClearColor(Color color) {
        this.clearColor = color;
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

    public int getFpsCap() {
        return this.fpsCap;
    }

    public TextureFile[] getTextureFiles() {
        return this.textureFiles;
    }

    public TtfFile[] getTtfFiles() {
        return this.ttfFiles;
    }

    public Color getClearColor() {
        return this.clearColor;
    }
}
