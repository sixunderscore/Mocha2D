package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;

public class RenderSettings {
    private String gpuName;
    private TextureFile[] textureFiles;
    private TtfFile[] ttfFiles;
    private final byte[] clearColor;
    private int fpsCap;

    public RenderSettings() {
        this.gpuName = "";
        this.textureFiles = new TextureFile[0];
        this.ttfFiles = new TtfFile[0];
        this.clearColor = new byte[]{(byte) 0, (byte) 0, (byte) 0};
        this.fpsCap = 1000;
    }

    public RenderSettings setGpuName(String gpuName) {
        this.gpuName = gpuName;
        return this;
    }

    public RenderSettings setTextureFiles(TextureFile... textureFiles) {
        this.textureFiles = textureFiles;
        return this;
    }

    public RenderSettings setTtfFiles(TtfFile... ttfFiles) {
        this.ttfFiles = ttfFiles;
        return this;
    }

    public RenderSettings setClearColor(byte r, byte g, byte b) {
        this.clearColor[0] = r;
        this.clearColor[1] = g;
        this.clearColor[2] = b;
        return this;
    }

    public RenderSettings setFPSCap(int fpsCap) {
        this.fpsCap = fpsCap;
        return this;
    }

    public String getGpuName() {
        return this.gpuName;
    }

    public TextureFile[] getTextureFiles() {
        return this.textureFiles;
    }

    public TtfFile[] getTtfFiles() {
        return this.ttfFiles;
    }

    public byte[] getClearColor() {
        return this.clearColor;
    }

    public int getFpsCap() {
        return this.fpsCap;
    }
}
