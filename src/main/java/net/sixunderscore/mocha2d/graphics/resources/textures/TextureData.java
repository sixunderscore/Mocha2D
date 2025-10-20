package net.sixunderscore.mocha2d.graphics.resources.textures;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public record TextureData(ByteBuffer data, int width, int height) implements AutoCloseable {
    @Override
    public void close() {
        MemoryUtil.memFree(this.data);
    }
}
