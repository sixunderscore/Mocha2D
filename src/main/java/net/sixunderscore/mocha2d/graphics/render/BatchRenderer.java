package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFont;
import net.sixunderscore.mocha2d.graphics.resources.text.GlyphData;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.util.ColorUtils;
import net.sixunderscore.mocha2d.util.MathUtils;
import net.sixunderscore.mocha2d.graphics.util.*;
import net.sixunderscore.mocha2d.graphics.RenderContext;
import net.sixunderscore.mocha2d.graphics.util.OrthographicCamera;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

public class BatchRenderer implements AutoCloseable {
    public static final int NO_TRANSFORM = 0;
    public static final int NO_TINT = 0;

    private final int framesInFlight;
    private int frameInFlightIndex;
    private final GraphicsPipeline pipeline;
    private final FrameResources[] frameResources;
    private final long[] imageAvailableSemaphores;
    private final long[] renderFinishedSemaphores;
    private final long[] inFlightFences;
    private final IntBuffer imageIndexBuffer;
    private final VkClearColorValue clearColor;

    public BatchRenderer(ResourceManager resourceManager, SwapChain swapChain) {
        this.framesInFlight = swapChain.getImageCount();
        this.frameInFlightIndex = 0;
        this.frameResources = new FrameResources[this.framesInFlight];
        this.imageAvailableSemaphores = new long[this.framesInFlight];
        this.renderFinishedSemaphores = new long[this.framesInFlight];
        this.inFlightFences = new long[this.framesInFlight];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < this.framesInFlight; ++i) {
                this.frameResources[i] = new FrameResources(stack);
                this.imageAvailableSemaphores[i] = SyncUtils.createSemaphore(stack);
                this.renderFinishedSemaphores[i] = SyncUtils.createSemaphore(stack);
                this.inFlightFences[i] = SyncUtils.createFence(stack, true);
            }

            this.pipeline = new GraphicsPipeline(stack, resourceManager, swapChain, "shaders/vertex.spv", "shaders/fragment.spv");
        }

        this.imageIndexBuffer = MemoryUtil.memAllocInt(1);
        this.clearColor = VkClearColorValue.calloc();
    }

    public int addRotationTransform(float rotationRadians, float originX, float originY) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        float sin = MathUtils.lookupSin(rotationRadians);
        float cos = MathUtils.lookupCos(rotationRadians);

        return frameResources.writeTransformToBuffer(cos, sin, -sin, cos, originX, originY);
    }

    public int addScalingTransform(float scaleX, float scaleY, float originX, float originY) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        return frameResources.writeTransformToBuffer(scaleX, 0, 0, scaleY, originX, originY);
    }

    public int addScalingRotationTransform(float scaleX, float scaleY, float rotationRadians, float originX, float originY) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        float sin = MathUtils.lookupSin(rotationRadians);
        float cos = MathUtils.lookupCos(rotationRadians);

        float m00 = cos * scaleX;
        float m10 = sin * scaleX;
        float m01 = -sin * scaleY;
        float m11 = cos * scaleY;

        return frameResources.writeTransformToBuffer(m00, m10, m01, m11, originX, originY);
    }

    public int addArbitraryTransform(float m00, float m10, float m01, float m11, float originX, float originY) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        return frameResources.writeTransformToBuffer(m00, m10, m01, m11, originX, originY);
    }

    public int addTint(byte r, byte g, byte b, float a) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        return frameResources.writeTintToBuffer(r, g, b, a);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height) {
        this.addSprite(texture, x, y, width, height, NO_TRANSFORM, NO_TINT);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height, int transformIndex) {
        this.addSprite(texture, x, y, width, height, transformIndex, NO_TINT);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height, int transformIndex, int tintIndex) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        float endX = x + width;
        float endY = y + height;

        frameResources.writeQuadToBuffers(texture, x, y, endX, y, x, endY, endX, endY, transformIndex, tintIndex);
    }

    public void addText(BitmapFont bitmapFont, String text, float x, float y, float charScale) {
        this.addText(bitmapFont, text, x, y, charScale, NO_TRANSFORM, NO_TINT);
    }

    public void addText(BitmapFont bitmapFont, String text, float x, float y, float charScale, int transformIndex) {
        this.addText(bitmapFont, text, x, y, charScale, transformIndex, NO_TINT);
    }

    public void addText(BitmapFont bitmapFont, String text, float x, float y, float charScale, int transformIndex, int tintIndex) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        float cursorX = x;
        float cursorY = y;
        int strLength = text.length();
        int charResolution = bitmapFont.getCharResolution();

        for (int i = 0; i < strLength; ++i) {
            char c = text.charAt(i);

            switch (c) {
                case ' ' -> cursorX += (charResolution * charScale) / 2.5f;
                case '\t' -> cursorX += (charResolution * charScale) * 1.5f;
                case '\n' -> {
                    cursorX = x;
                    cursorY -= charResolution * charScale;
                }
                default -> {
                    GlyphData glyphData = bitmapFont.getGlyphData(c);
                    TextureRegion texture = glyphData.textureRegion();

                    float width = texture.width() * charScale;
                    float height = texture.height() * charScale;
                    float charX = cursorX;
                    float endCharX = charX + width;
                    float charY = cursorY + glyphData.descent() * charScale;
                    float endCharY = charY + height;

                    frameResources.writeQuadToBuffers(texture, charX, charY, endCharX, charY, charX, endCharY, endCharX, endCharY, transformIndex, tintIndex);

                    cursorX += glyphData.advance() * charScale;
                }
            }
        }
    }

    public void addArbitraryQuad(TextureRegion texture,
                                float bottomLeftX, float bottomLeftY,
                                float bottomRightX, float bottomRightY,
                                float topLeftX, float topLeftY,
                                float topRightX, float topRightY) {
        this.addArbitraryQuad(texture, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, topLeftX, topLeftY, topRightX, topRightY, NO_TRANSFORM, NO_TINT);
    }

    public void addArbitraryQuad(TextureRegion texture,
                                 float bottomLeftX, float bottomLeftY,
                                 float bottomRightX, float bottomRightY,
                                 float topLeftX, float topLeftY,
                                 float topRightX, float topRightY,
                                 int transformIndex) {
        this.addArbitraryQuad(texture, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, topLeftX, topLeftY, topRightX, topRightY, transformIndex, NO_TINT);
    }

    public void addArbitraryQuad(TextureRegion texture,
                                 float bottomLeftX, float bottomLeftY,
                                 float bottomRightX, float bottomRightY,
                                 float topLeftX, float topLeftY,
                                 float topRightX, float topRightY,
                                 int transformIndex, int tintIndex) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        frameResources.writeQuadToBuffers(texture, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, topLeftX, topLeftY, topRightX, topRightY, transformIndex, tintIndex);
    }

    public void draw(OrthographicCamera camera, SwapChain swapChain, ResourceManager resourceManager, ViewportScissor viewportScissor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long inFlightFence = this.inFlightFences[this.frameInFlightIndex];

            VK14.vkWaitForFences(RenderContext.getLogicalDevice(), inFlightFence, true, Long.MAX_VALUE);
            VK14.vkResetFences(RenderContext.getLogicalDevice(), inFlightFence);

            long imageAvailableSemaphore = this.imageAvailableSemaphores[this.frameInFlightIndex];

            int errCode = KHRSwapchain.vkAcquireNextImageKHR(
                    RenderContext.getLogicalDevice(),
                    swapChain.getSwapChain(),
                    Long.MAX_VALUE,
                    imageAvailableSemaphore,
                    VK14.VK_NULL_HANDLE,
                    this.imageIndexBuffer
            );

            FrameResources frameResources = this.frameResources[this.frameInFlightIndex];

            if (errCode == VK14.VK_SUCCESS || errCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                int imageIndex = this.imageIndexBuffer.get(0);

                frameResources.recordCommands(stack, swapChain, resourceManager, viewportScissor, this.pipeline, imageIndex, this.clearColor, camera);

                long renderFinishedSemaphore = this.renderFinishedSemaphores[imageIndex];
                frameResources.submitCommandBuffer(stack, imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);

                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                        .sType$Default()
                        .pImageIndices(this.imageIndexBuffer)
                        .pWaitSemaphores(stack.mallocLong(1).put(0, renderFinishedSemaphore))
                        .pSwapchains(stack.mallocLong(1).put(0, swapChain.getSwapChain()))
                        .swapchainCount(1);
                KHRSwapchain.vkQueuePresentKHR(RenderContext.getGraphicsQueue(), presentInfo);

                this.frameInFlightIndex = ++this.frameInFlightIndex % this.framesInFlight;
            }

            frameResources.reset();
        }
    }

    public long[] getInFlightFences() {
        return this.inFlightFences;
    }

    public void setClearColor(byte r, byte g, byte b) {
        this.clearColor
                .float32(0, ColorUtils.srgbToLinear(r))
                .float32(1, ColorUtils.srgbToLinear(g))
                .float32(2, ColorUtils.srgbToLinear(b));
    }

    @Override
    public void close() {
        VkDevice logicalDevice = RenderContext.getLogicalDevice();

        VK14.vkWaitForFences(logicalDevice, this.inFlightFences, true, Long.MAX_VALUE);
        VK14.vkQueueWaitIdle(RenderContext.getGraphicsQueue());

        for (int i = 0; i < this.framesInFlight; ++i) {
            this.frameResources[i].close();
            VK14.vkDestroySemaphore(logicalDevice, this.imageAvailableSemaphores[i], null);
            VK14.vkDestroySemaphore(logicalDevice, this.renderFinishedSemaphores[i], null);
            VK14.vkDestroyFence(logicalDevice, this.inFlightFences[i], null);
        }

        this.pipeline.close();

        MemoryUtil.memFree(this.imageIndexBuffer);
        this.clearColor.close();
    }
}
