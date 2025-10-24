package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.graphics.resources.textures.UVs;
import net.sixunderscore.mocha2d.util.Color;
import net.sixunderscore.mocha2d.vulkan.util.*;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.OrthographicCamera;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BatchRenderer implements AutoCloseable {
    private final int framesInFlight;
    private int frameInFlightIndex;
    private final GraphicsPipeline pipeline;
    private final FrameResources[] frameResources;
    private final long[] imageAvailableSemaphores;
    private final long[] renderFinishedSemaphores;
    private final long[] inFlightFences;
    private int indexOffset;
    private final IntBuffer imageIndexBuffer;
    private final VkClearColorValue clearColor;

    public BatchRenderer(ResourceManager resourceManager, SwapChain swapChain, Color clearColor) {
        this.framesInFlight = swapChain.getImageCount();
        this.frameInFlightIndex = 0;
        this.indexOffset = 0;
        this.frameResources = new FrameResources[this.framesInFlight];
        this.imageAvailableSemaphores = new long[this.framesInFlight];
        this.renderFinishedSemaphores = new long[this.framesInFlight];
        this.inFlightFences = new long[this.framesInFlight];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int maxQuads = 0xFFFF / 4; // Unsigned short max value divided by 4 vertices in a quad
            int maxIndices = maxQuads * 6; // 6 indices in a quad (two triangles)
            int maxVertices = maxQuads * 4; // 4 vertices in a quad

            int indexBufferSizeBytes = maxIndices * Short.BYTES;
            int vertexBufferSizeBytes = maxVertices * VertexData.TOTAL_SIZE_BYTES;

            for (int i = 0; i < this.framesInFlight; ++i) {
                this.frameResources[i] = new FrameResources(stack, indexBufferSizeBytes, vertexBufferSizeBytes);
                this.imageAvailableSemaphores[i] = SyncUtils.createSemaphore(stack);
                this.renderFinishedSemaphores[i] = SyncUtils.createSemaphore(stack);
                this.inFlightFences[i] = SyncUtils.createFence(stack, true);
            }

            this.pipeline = new GraphicsPipeline(stack, resourceManager, swapChain, "assets/shaders/vertex.spv", "assets/shaders/fragment.spv");
        }

        this.imageIndexBuffer = MemoryUtil.memAllocInt(1);
        this.clearColor = VkClearColorValue.calloc()
                .float32(0, clearColor.normalizedR())
                .float32(1, clearColor.normalizedG())
                .float32(2, clearColor.normalizedB())
                .float32(3, 1f);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height) {
        this.addSprite(texture, x, y, width, height, 0, 0, 0);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height, float rotationDegrees, float pivotX, float pivotY) {
        FrameResources frameResources = this.frameResources[this.frameInFlightIndex];
        ShortBuffer mappedIndexBuffer = frameResources.getMappedIndexBuffer();
        FloatBuffer mappedVertexBuffer = frameResources.getMappedVertexBuffer();

        // ---- Writing index data for quad ----

        mappedIndexBuffer
                .put((short) this.indexOffset)        // Top-left
                .put((short) (this.indexOffset + 1))  // Top-right
                .put((short) (this.indexOffset + 2))  // Bottom-left

                .put((short) (this.indexOffset + 1))  // Top-right
                .put((short) (this.indexOffset + 3))  // Bottom-right
                .put((short) (this.indexOffset + 2)); // Bottom-left

        this.indexOffset += 4;

        // ---- Writing vertex data for quad ----

        // UV data
        UVs uvCoordinates = texture.uvCoordinates();
        Vector2f topLeft = uvCoordinates.topLeft();
        Vector2f topRight = uvCoordinates.topRight();
        Vector2f bottomLeft = uvCoordinates.bottomLeft();
        Vector2f bottomRight = uvCoordinates.bottomRight();

        int index = texture.imageIndex();

        // Rotation data
        float rotationSin = 0;
        float rotationCos = 1f;
        if (Math.abs(rotationDegrees) > 0.001f) {
            float rotationRadians = (float) Math.toRadians(rotationDegrees);
            rotationSin = org.joml.Math.sin(rotationRadians);
            rotationCos = org.joml.Math.cos(rotationRadians);
        }

        mappedVertexBuffer
                .put(x).put(y)
                .put(topLeft.x).put(topLeft.y)
                .put(index)
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        mappedVertexBuffer
                .put(x + width).put(y)
                .put(topRight.x).put(topRight.y)
                .put(index)
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        mappedVertexBuffer
                .put(x).put(y + height)
                .put(bottomLeft.x).put(bottomLeft.y)
                .put(index)
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        mappedVertexBuffer
                .put(x + width).put(y + height)
                .put(bottomRight.x).put(bottomRight.y)
                .put(index)
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);
    }

    public void draw(OrthographicCamera camera, SwapChain swapChain, ResourceManager resourceManager, ViewportScissor viewportScissor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long inFlightFence = this.inFlightFences[this.frameInFlightIndex];

            VK14.vkWaitForFences(VulkanManager.getLogicalDevice(), inFlightFence, true, Long.MAX_VALUE);
            VK14.vkResetFences(VulkanManager.getLogicalDevice(), inFlightFence);

            long imageAvailableSemaphore = this.imageAvailableSemaphores[this.frameInFlightIndex];

            KHRSwapchain.vkAcquireNextImageKHR(VulkanManager.getLogicalDevice(), swapChain.getSwapChain(), Long.MAX_VALUE, imageAvailableSemaphore, VK14.VK_NULL_HANDLE, this.imageIndexBuffer);

            int imageIndex = this.imageIndexBuffer.get(0);
            FrameResources frameResources = this.frameResources[this.frameInFlightIndex];

            frameResources.recordGraphicsCommands(stack, swapChain, resourceManager, viewportScissor, this.pipeline, imageIndex, this.clearColor, camera);

            long renderFinishedSemaphore = this.renderFinishedSemaphores[imageIndex];
            frameResources.submitCommandBuffer(stack, imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);
            frameResources.presentImageToSwapChain(stack, renderFinishedSemaphore, swapChain, this.imageIndexBuffer);

            frameResources.resetMappedBuffers();
            this.indexOffset = 0;
            this.frameInFlightIndex = ++this.frameInFlightIndex % this.framesInFlight;
        }
    }

    public long[] getInFlightFences() {
        return this.inFlightFences;
    }

    public void setClearColor(Color color) {
        this.clearColor
                .float32(0, color.normalizedR())
                .float32(1, color.normalizedG())
                .float32(2, color.normalizedB());
    }

    @Override
    public void close() {
        VkDevice logicalDevice = VulkanManager.getLogicalDevice();

        VK14.vkWaitForFences(logicalDevice, this.inFlightFences, true, Long.MAX_VALUE);
        VK14.vkQueueWaitIdle(VulkanManager.getGraphicsQueue());

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
