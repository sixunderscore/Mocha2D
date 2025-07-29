package net.sixunderscore.mocha2d.util;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceLoader {
    public static ByteBuffer loadRawFile(String path) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("File " + path + " not found");
            }

            return MemoryUtil.memAlloc(stream.available()).put(stream.readAllBytes()).flip();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + path + ": " + e);
        }
    }
}
