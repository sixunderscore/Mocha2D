package net.sixunderscore.mocha2d.util;

public class WindowSettings {
    private TextureFile[] textureFiles = new TextureFile[0];
    private TtfFile[] ttfFiles = new TtfFile[0];
    private String windowName = "Mocha2D Window";
    private String windowIconPath = "";
    private int initialWidth = 800;
    private int initialHeight = 500;
    private boolean resizeable = true;
    private boolean vSyncEnabled = false;
    private int fpsCap = 1000;
    private Color clearColor = new Color((byte) 0, (byte) 0, (byte) 0);

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

    public void setVSyncEnabled() {
        this.vSyncEnabled = true;
        this.fpsCap = 0;
    }

    public void setVSyncDisabled(int fpsCap) {
        this.vSyncEnabled = false;
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

    public boolean isVSyncEnabled() {
        return this.vSyncEnabled;
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
