package net.sixunderscore.mocha2d.graphics.resources.text;

public enum BitmapFontResolution {
    NORMAL(64, 580),
    LARGE(128, 1160),
    LARGEST(256, 2320);

    private final int charResolution;
    private final int atlasSideSize;

    BitmapFontResolution(int charResolution, int atlasSideSize) {
        this.charResolution = charResolution;
        this.atlasSideSize = atlasSideSize;
    }

    public int getCharResolution() {
        return this.charResolution;
    }

    public int getAtlasSideSize() {
        return this.atlasSideSize;
    }
}
