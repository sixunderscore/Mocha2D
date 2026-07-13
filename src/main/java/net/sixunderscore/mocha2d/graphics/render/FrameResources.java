package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.OrthographicCamera;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.vulkan.util.*;
import org.joml.Matrix2f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class FrameResources implements AutoCloseable {
    private final CommandPool commandPool;
    private final VkCommandBuffer commandBuffer;
    private final GpuBuffer indexBuffer;
    private final MemorySegment mappedIndexBuffer;
    private int indexWriteOffset;
    private int indexOffset;
    private final GpuBuffer vertexBuffer;
    private final MemorySegment mappedVertexBuffer;
    private int vertexWriteOffset;
    private final GpuBuffer transformBuffer;
    private final MemorySegment mappedTransformBuffer;
    private int transformWriteOffset;
    private int transformIndex;
    private final long transformBufferAddress;

    public FrameResources(MemoryStack stack) {
        final int maxQuads = 0xFFFF / 4; // Unsigned short max value divided by 4 vertices in a quad
        final int maxIndices = maxQuads * 6; // 6 indices in a quad (two triangles)
        final int maxVertices = maxQuads * 4; // 4 vertices in a quad

        final int indexBufferSizeBytes = maxIndices * Short.BYTES;
        final int vertexBufferSizeBytes = maxVertices * VertexData.TOTAL_SIZE_BYTES;
        final int transformBufferSizeBytes = maxQuads * (Matrix2f.BYTES + Float.BYTES * 2);

        this.commandPool = new CommandPool(stack, VulkanManager.getGraphicsQueueIndex(), VK14.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
        this.commandBuffer = this.commandPool.allocateCommandBuffer(stack);

        this.indexBuffer = new GpuBuffer(stack, indexBufferSizeBytes, VK14.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedIndexBuffer = MemorySegment.ofAddress(this.indexBuffer.map(stack)).reinterpret(indexBufferSizeBytes);
        this.indexOffset = 0;

        this.vertexBuffer = new GpuBuffer(stack, vertexBufferSizeBytes, VK14.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedVertexBuffer = MemorySegment.ofAddress(this.vertexBuffer.map(stack)).reinterpret(vertexBufferSizeBytes);

        this.transformBuffer = new GpuBuffer(stack, transformBufferSizeBytes, VK14.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedTransformBuffer = MemorySegment.ofAddress(this.transformBuffer.map(stack)).reinterpret(transformBufferSizeBytes);
        this.transformIndex = 0;

        this.writeTransformToBuffer(1f, 0, 0, 1f, 0, 0);

        VkBufferDeviceAddressInfo bufferDeviceAddressInfo = VkBufferDeviceAddressInfo.calloc(stack)
                .sType$Default()
                .buffer(this.transformBuffer.getBuffer());
        this.transformBufferAddress = VK14.vkGetBufferDeviceAddress(VulkanManager.getLogicalDevice(), bufferDeviceAddressInfo);
    }

    public int writeTransformToBuffer(float m00, float m10, float m01, float m11, float originX, float originY) {
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset, m00);
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset + 4,  m10);
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset + 8,  m01);
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset + 12, m11);
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset + 16, originX);
        this.mappedTransformBuffer.set(ValueLayout.JAVA_FLOAT, this.transformWriteOffset + 20, originY);

        this.transformWriteOffset += (6 * Float.BYTES);

        return this.transformIndex++;
    }

    public void writeQuadToBuffers(TextureRegion texture,
                                   float bottomLeftX, float bottomLeftY,
                                   float bottomRightX, float bottomRightY,
                                   float topLeftX, float topLeftY,
                                   float topRightX, float topRightY,
                                   int transformIndex) {
        // ---- Writing index data for quad ----
        short currentBase = (short) this.indexOffset;

        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset, currentBase);
        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset + 2, (short) (currentBase + 1));
        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset + 4, (short) (currentBase + 2));
        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset + 6, (short) (currentBase + 1));
        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset + 8, (short) (currentBase + 3));
        this.mappedIndexBuffer.set(ValueLayout.JAVA_SHORT, this.indexWriteOffset + 10, (short) (currentBase + 2));

        this.indexWriteOffset += (6 * Short.BYTES);
        this.indexOffset += 4;

        // ---- Writing vertex data for quad ----
        int texIndex = texture.imageIndex();

        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset, bottomLeftX);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 4, bottomLeftY);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 8, texture.topLeftU());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 12, texture.topLeftV());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 16, texIndex);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 20, transformIndex);

        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 24, bottomRightX);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 28, bottomRightY);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 32, texture.topRightU());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 36, texture.topRightV());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 40, texIndex);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 44, transformIndex);

        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 48, topLeftX);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 52, topLeftY);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 56, texture.bottomLeftU());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 60, texture.bottomLeftV());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 64, texIndex);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 68, transformIndex);

        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 72, topRightX);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 76, topRightY);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 80, texture.bottomRightU());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 84, texture.bottomRightV());
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 88, texIndex);
        this.mappedVertexBuffer.set(ValueLayout.JAVA_FLOAT, this.vertexWriteOffset + 92, transformIndex);

        this.vertexWriteOffset += (24 * Float.BYTES);
    }

    public void recordGraphicsCommands(MemoryStack stack, SwapChain swapChain, ResourceManager resourceManager, ViewportScissor viewportScissor, GraphicsPipeline pipeline, int imageIndex, VkClearColorValue clearColorValue, OrthographicCamera camera) {
        VK14.vkResetCommandPool(VulkanManager.getLogicalDevice(), this.commandPool.getPool(), 0);
        VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType$Default()
                .flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        VK14.vkBeginCommandBuffer(this.commandBuffer, commandBufferBeginInfo);

        // Acquire barrier
        VkImageMemoryBarrier2.Buffer imageBarrier = VkImageMemoryBarrier2.calloc(1, stack);
        imageBarrier.get(0)
                .sType$Default()
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_NONE)
                .dstAccessMask(VK14.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .image(swapChain.getImage(imageIndex))
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));
        VkDependencyInfo barrierDependencyInfo = VkDependencyInfo.calloc(stack)
                .sType$Default()
                .pImageMemoryBarriers(imageBarrier);
        VK14.vkCmdPipelineBarrier2(this.commandBuffer, barrierDependencyInfo);

        VK14.vkCmdPushConstants(this.commandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, camera.getBuffer());
        VK14.vkCmdPushConstants(this.commandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_VERTEX_BIT, Matrix4f.BYTES, stack.longs(this.transformBufferAddress));

        VkClearValue clearValue = VkClearValue.calloc(stack).color(clearColorValue);
        VkRenderingAttachmentInfo.Buffer colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack);
        colorAttachments.get(0)
                .sType$Default()
                .imageView(swapChain.getImageView(imageIndex))
                .imageLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK14.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK14.VK_ATTACHMENT_STORE_OP_STORE)
                .clearValue(clearValue);
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType$Default()
                .renderArea(viewportScissor.getScissor().get(0))
                .layerCount(1)
                .pColorAttachments(colorAttachments);
        VK14.vkCmdBeginRendering(this.commandBuffer, renderingInfo);

        VK14.vkCmdBindPipeline(this.commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());
        VK14.vkCmdBindDescriptorSets(this.commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, stack.mallocLong(1).put(0, resourceManager.getDescriptorSet()), null);
        VK14.vkCmdSetViewport(this.commandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(this.commandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(this.commandBuffer, 0, stack.mallocLong(1).put(0, this.vertexBuffer.getBuffer()), stack.mallocLong(1).put(0, 0));
        VK14.vkCmdBindIndexBuffer(this.commandBuffer, this.indexBuffer.getBuffer(), 0, VK14.VK_INDEX_TYPE_UINT16);

        VK14.vkCmdDrawIndexed(this.commandBuffer, this.indexWriteOffset / Short.BYTES, 1, 0, 0, 0);

        VK14.vkCmdEndRendering(this.commandBuffer);

        // Present barrier
        imageBarrier.get(0)
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK14.VK_ACCESS_2_NONE)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .newLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        VK14.vkCmdPipelineBarrier2(this.commandBuffer, barrierDependencyInfo);

        VK14.vkEndCommandBuffer(this.commandBuffer);
    }

    public void submitCommandBuffer(MemoryStack stack, long waitSemaphore, long signalSemaphore, long signalFence) {
        VkSemaphoreSubmitInfo.Buffer waitSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1, stack);
        waitSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .semaphore(waitSemaphore)
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        VkSemaphoreSubmitInfo.Buffer signalSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1, stack);
        signalSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .semaphore(signalSemaphore)
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        VkCommandBufferSubmitInfo.Buffer commandBufferSubmitInfo = VkCommandBufferSubmitInfo.calloc(1, stack);
        commandBufferSubmitInfo.get(0)
                .sType$Default()
                .commandBuffer(this.commandBuffer);

        VkSubmitInfo2.Buffer submitInfo = VkSubmitInfo2.calloc(1, stack);
        submitInfo.get(0)
                .sType$Default()
                .pWaitSemaphoreInfos(waitSemaphoreSubmitInfo)
                .pSignalSemaphoreInfos(signalSemaphoreSubmitInfo)
                .pCommandBufferInfos(commandBufferSubmitInfo);

        VK14.vkQueueSubmit2(VulkanManager.getGraphicsQueue(), submitInfo, signalFence);
    }

    public void reset() {
        this.vertexWriteOffset = 0;
        this.indexWriteOffset = 0;
        this.indexOffset = 0;
        this.transformWriteOffset = Matrix2f.BYTES + Float.BYTES * 2;
        this.transformIndex = 1;
    }

    @Override
    public void close() {
        this.commandPool.close();
        this.indexBuffer.close();
        this.vertexBuffer.close();
        this.transformBuffer.close();
    }
}
