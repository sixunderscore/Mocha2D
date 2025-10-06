package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.textures.TextureData;
import net.sixunderscore.mocha2d.vulkan.util.GpuBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK14;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceUtils {
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

    public static TextureData loadAndDecodeImage(String imagePath) {
        ByteBuffer rawImage = loadRawFile(imagePath);
        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];

        ByteBuffer imageData = STBImage.stbi_load_from_memory(rawImage, width, height, channels, STBImage.STBI_rgb_alpha);
        MemoryUtil.memFree(rawImage);

        if (imageData == null) {
            throw new RuntimeException("Failed to decode image: " + imagePath);
        }

        return new TextureData(imageData, width[0], height[0]);
    }

    public static TextureData convertGrayscaleToRGBA(ByteBuffer grayscaleImage, int grayscaleSideSize, int grayscaleTotalSize, Color color) {
        ByteBuffer rgbaBuffer = MemoryUtil.memAlloc(grayscaleTotalSize * 4);

        for (int i = 0; i < grayscaleTotalSize; ++i) {
            rgbaBuffer.put(color.r()).put(color.g()).put(color.b()).put(grayscaleImage.get(i));
        }

        return new TextureData(rgbaBuffer.flip(), grayscaleSideSize, grayscaleSideSize);
    }

    public static GpuBuffer createStagingImageBuffer(MemoryStack stack, TextureData textureData) {
        int imageBufferSizeBytes = (textureData.width() * textureData.height()) * 4;
        GpuBuffer stagingImageBuffer = new GpuBuffer(stack, imageBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        ByteBuffer mappedStagingImageBuffer = stagingImageBuffer.map(stack);
        mappedStagingImageBuffer.clear().put(textureData.data());

        return stagingImageBuffer;
    }
}
