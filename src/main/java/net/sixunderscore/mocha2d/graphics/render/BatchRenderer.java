package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.graphics.textures.TextureRegion;
import net.sixunderscore.mocha2d.graphics.textures.UVs;
import net.sixunderscore.mocha2d.vulkan.util.*;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.OrthographicCamera;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BatchRenderer implements AutoCloseable {
    private static final int FRAMES_IN_FLIGHT = 2;
    private final int swapChainImageCount;
    private int frameIndex = 0;
    private final RenderHelper renderHelper;
    private final GraphicsPipeline pipeline;
    private final CommandPool cmdPool;
    private final VkCommandBuffer[] cmdBuffers;

    private final GpuBuffer[] stagingVertexBuffers;
    private final FloatBuffer[] mappedStagingVertexBuffers;
    private final GpuBuffer vertexBuffer;

    private final GpuBuffer[] stagingIndexBuffers;
    private final ShortBuffer[] mappedStagingIndexBuffers;
    private final GpuBuffer indexBuffer;
    private int indexOffset = 0;

    private final long[] imageAvailableSemaphores;
    private final long[] renderFinishedSemaphores;
    private final long[] inFlightFences;

    private final IntBuffer imageIndexBuffer;

    public BatchRenderer(TextureManager textureManager, SwapChain swapChain) {
        this.swapChainImageCount = swapChain.getImageCount();

        this.cmdBuffers = new VkCommandBuffer[FRAMES_IN_FLIGHT];
        this.stagingVertexBuffers = new GpuBuffer[FRAMES_IN_FLIGHT];
        this.mappedStagingVertexBuffers = new FloatBuffer[FRAMES_IN_FLIGHT];
        this.stagingIndexBuffers = new GpuBuffer[FRAMES_IN_FLIGHT];
        this.mappedStagingIndexBuffers = new ShortBuffer[FRAMES_IN_FLIGHT];

        this.imageAvailableSemaphores = new long[FRAMES_IN_FLIGHT];
        this.renderFinishedSemaphores = new long[this.swapChainImageCount];
        this.inFlightFences = new long[FRAMES_IN_FLIGHT];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int maxQuads = 0xFFFF / 4; // Unsigned short max value divided by 4 vertices in a quad
            int maxIndices = maxQuads * 6; // 6 indices in a quad (two triangles)
            int maxVertices = maxQuads * 4; // 4 vertices in a quad

            int indexBufferSizeBytes = maxIndices * Short.BYTES;
            int vertexBufferSizeBytes = maxVertices * VertexData.TOTAL_SIZE_BYTES;

            this.indexBuffer = new GpuBuffer(stack, indexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY);
            this.vertexBuffer = new GpuBuffer(stack, vertexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY);

            this.cmdPool = new CommandPool(stack, VulkanManager.getGraphicsQueueIndex(), VK14.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            for (int i = 0; i < FRAMES_IN_FLIGHT; ++i) {
                this.cmdBuffers[i] = this.cmdPool.allocateCommandBuffer(stack);

                this.stagingIndexBuffers[i] = new GpuBuffer(stack, indexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
                this.mappedStagingIndexBuffers[i] = this.stagingIndexBuffers[i].map(stack).asShortBuffer();
                this.stagingVertexBuffers[i] = new GpuBuffer(stack, vertexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
                this.mappedStagingVertexBuffers[i] = this.stagingVertexBuffers[i].map(stack).asFloatBuffer();

                this.imageAvailableSemaphores[i] = SyncUtils.createSemaphore(stack);
                this.inFlightFences[i] = SyncUtils.createFence(stack, true);
            }

            for (int i = 0; i < this.swapChainImageCount; ++i) {
                this.renderFinishedSemaphores[i] = SyncUtils.createSemaphore(stack);
            }

            this.pipeline = new GraphicsPipeline(stack, textureManager, swapChain, "assets/shaders/vertex.spv", "assets/shaders/fragment.spv");
        }

        this.renderHelper = new RenderHelper(textureManager, this.vertexBuffer);
        this.imageIndexBuffer = MemoryUtil.memAllocInt(1);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height) {
        this.addSprite(texture, x, y, width, height, 0, 0, 0);
    }

    public void addSprite(TextureRegion texture, float x, float y, float width, float height, float rotationDegrees, float pivotX, float pivotY) {
        this.mappedStagingIndexBuffers[this.frameIndex]
                .put((short) this.indexOffset)        // Top-left
                .put((short) (this.indexOffset + 1))  // Top-right
                .put((short) (this.indexOffset + 2))  // Bottom-left

                .put((short) (this.indexOffset + 1))  // Top-right
                .put((short) (this.indexOffset + 3))  // Bottom-right
                .put((short) (this.indexOffset + 2)); // Bottom-left

        this.indexOffset += 4;

        FloatBuffer stagingVertexBuffer = this.mappedStagingVertexBuffers[this.frameIndex];

        // UV data
        UVs uvCoordinates = texture.uvCoordinates();
        Vector2f topLeft = uvCoordinates.topLeft();
        Vector2f topRight = uvCoordinates.topRight();
        Vector2f bottomLeft = uvCoordinates.bottomLeft();
        Vector2f bottomRight = uvCoordinates.bottomRight();

        // Rotation data
        float rotationSin = 0;
        float rotationCos = 1f;
        if (Math.abs(rotationDegrees) > 0.0001f) {
            float rotationRadians = (float) Math.toRadians(rotationDegrees);
            rotationSin = (float) Math.sin(rotationRadians);
            rotationCos = (float) Math.cos(rotationRadians);
        }

        stagingVertexBuffer
                .put(x).put(y)
                .put(topLeft.x).put(topLeft.y)
                .put(texture.imageIndex())
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        stagingVertexBuffer
                .put(x + width).put(y)
                .put(topRight.x).put(topRight.y)
                .put(texture.imageIndex())
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        stagingVertexBuffer
                .put(x).put(y + height)
                .put(bottomLeft.x).put(bottomLeft.y)
                .put(texture.imageIndex())
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);

        stagingVertexBuffer
                .put(x + width).put(y + height)
                .put(bottomRight.x).put(bottomRight.y)
                .put(texture.imageIndex())
                .put(rotationSin).put(rotationCos)
                .put(pivotX).put(pivotY);
    }

    public void draw(OrthographicCamera camera, SwapChain swapChain, ViewportScissor viewportScissor) {
        long inFlightFence = this.inFlightFences[this.frameIndex];

        VK14.vkWaitForFences(VulkanManager.getLogicalDevice(), inFlightFence, true, Long.MAX_VALUE);
        VK14.vkResetFences(VulkanManager.getLogicalDevice(), inFlightFence);

        long imageAvailableSemaphore = this.imageAvailableSemaphores[this.frameIndex];

        KHRSwapchain.vkAcquireNextImageKHR(VulkanManager.getLogicalDevice(), swapChain.getSwapChain(), Long.MAX_VALUE, imageAvailableSemaphore, VK14.VK_NULL_HANDLE, this.imageIndexBuffer);
        int imageIndex = this.imageIndexBuffer.get(0);

        FloatBuffer mappedStagingVertexBuffer = this.mappedStagingVertexBuffers[this.frameIndex];
        int vertexBufferSizeBytes = mappedStagingVertexBuffer.position() * Float.BYTES;
        ShortBuffer mappedStagingIndexBuffer = this.mappedStagingIndexBuffers[this.frameIndex];
        int indexBufferSizeBytes = mappedStagingIndexBuffer.position() * Short.BYTES;

        VkCommandBuffer commandBuffer = this.cmdBuffers[this.frameIndex];

        VK14.vkResetCommandBuffer(commandBuffer, 0);
        VK14.vkBeginCommandBuffer(commandBuffer, this.renderHelper.getCmdBufferBeginInfo());

        this.renderHelper.recordTransferCommands(commandBuffer, this.stagingIndexBuffers[this.frameIndex], this.indexBuffer, indexBufferSizeBytes, this.stagingVertexBuffers[this.frameIndex], this.vertexBuffer, vertexBufferSizeBytes);
        this.renderHelper.recordGraphicsCommands(commandBuffer, swapChain, viewportScissor, this.indexBuffer, mappedStagingIndexBuffer.position(), this.pipeline, imageIndex, camera);

        VK14.vkEndCommandBuffer(commandBuffer);

        long renderFinishedSemaphore = this.renderFinishedSemaphores[imageIndex];

        this.renderHelper.submitGraphicsCmdBuffer(commandBuffer, imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);
        this.renderHelper.presentImageToSwapChain(swapChain, this.imageIndexBuffer, renderFinishedSemaphore);

        mappedStagingVertexBuffer.clear();
        mappedStagingIndexBuffer.clear();
        this.indexOffset = 0;
        this.frameIndex = ++this.frameIndex % FRAMES_IN_FLIGHT;
    }

    public long[] getInFlightFences() {
        return this.inFlightFences;
    }

    @Override
    public void close() {
        VkDevice logicalDevice = VulkanManager.getLogicalDevice();
        VK14.vkWaitForFences(logicalDevice, this.inFlightFences, true, Long.MAX_VALUE);
        VK14.vkQueueWaitIdle(VulkanManager.getGraphicsQueue());

        this.cmdPool.close();
        this.renderHelper.close();

        this.vertexBuffer.close();
        this.indexBuffer.close();

        for (int i = 0; i < FRAMES_IN_FLIGHT; ++i) {
            this.stagingVertexBuffers[i].close();
            this.stagingIndexBuffers[i].close();

            VK14.vkDestroyFence(logicalDevice, this.inFlightFences[i], null);
            VK14.vkDestroySemaphore(logicalDevice, this.imageAvailableSemaphores[i], null);
        }

        for (int i = 0; i < this.swapChainImageCount; ++i) {
            VK14.vkDestroySemaphore(logicalDevice, this.renderFinishedSemaphores[i], null);
        }

        this.pipeline.close();

        MemoryUtil.memFree(this.imageIndexBuffer);
    }
}
