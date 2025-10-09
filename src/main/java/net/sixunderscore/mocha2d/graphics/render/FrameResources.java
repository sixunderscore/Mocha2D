package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.util.OrthographicCamera;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.vulkan.util.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class FrameResources implements AutoCloseable {
    private final CommandPool commandPool;
    private final VkCommandBuffer commandBuffer;
    private final GpuBuffer stagingIndexBuffer;
    private final ShortBuffer mappedStagingIndexBuffer;
    private final GpuBuffer stagingVertexBuffer;
    private final FloatBuffer mappedStagingVertexBuffer;

    public FrameResources(MemoryStack stack, int indexBufferSizeBytes, int vertexBufferSizeBytes) {
        this.commandPool = new CommandPool(stack, VulkanManager.getGraphicsQueueIndex(), VK14.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
        this.commandBuffer = this.commandPool.allocateCommandBuffer(stack);

        this.stagingIndexBuffer = new GpuBuffer(stack, indexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedStagingIndexBuffer = this.stagingIndexBuffer.map(stack).asShortBuffer();
        this.stagingVertexBuffer = new GpuBuffer(stack, vertexBufferSizeBytes, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedStagingVertexBuffer = this.stagingVertexBuffer.map(stack).asFloatBuffer();
    }

    public void recordGraphicsAndTransferCommands(MemoryStack stack, GpuBuffer indexBuffer, GpuBuffer vertexBuffer, SwapChain swapChain, TextureManager textureManager, ViewportScissor viewportScissor, GraphicsPipeline pipeline, int imageIndex, VkClearColorValue clearColorValue, OrthographicCamera camera) {
        VK14.vkResetCommandPool(VulkanManager.getLogicalDevice(), this.commandPool.getPool(), 0);
        VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType$Default()
                .flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        VK14.vkBeginCommandBuffer(this.commandBuffer, commandBufferBeginInfo);

        // ---- Transfer commands ----

        VkBufferCopy.Buffer bufferCopy = VkBufferCopy.malloc(1, stack);

        int indexBufferSizeBytes = this.mappedStagingIndexBuffer.position() * Short.BYTES;
        if (indexBufferSizeBytes > 0) {
            bufferCopy.get(0).set(0, 0, indexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(this.commandBuffer, this.stagingIndexBuffer.getBuffer(), indexBuffer.getBuffer(), bufferCopy);
        }

        int vertexBufferSizeBytes = this.mappedStagingVertexBuffer.position() * Float.BYTES;
        if (vertexBufferSizeBytes > 0) {
            bufferCopy.get(0).set(0, 0, vertexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(this.commandBuffer, this.stagingVertexBuffer.getBuffer(), vertexBuffer.getBuffer(), bufferCopy);
        }

        // ---- Graphics commands ----

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
        VK14.vkCmdBindDescriptorSets(this.commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, stack.mallocLong(1).put(0, textureManager.getDescriptorSet()), null);
        VK14.vkCmdSetViewport(this.commandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(this.commandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(this.commandBuffer, 0, stack.mallocLong(1).put(0, vertexBuffer.getBuffer()), stack.mallocLong(1).put(0, 0));
        VK14.vkCmdBindIndexBuffer(this.commandBuffer, indexBuffer.getBuffer(), 0, VK14.VK_INDEX_TYPE_UINT16);

        int indexCount = this.mappedStagingIndexBuffer.position();
        VK14.vkCmdDrawIndexed(this.commandBuffer, indexCount, 1, 0, 0, 0);

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

    public void presentImageToSwapChain(MemoryStack stack, SwapChain swapChain, IntBuffer imageIndex, long waitSemaphore) {
        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType$Default()
                .pImageIndices(imageIndex)
                .pWaitSemaphores(stack.mallocLong(1).put(0, waitSemaphore))
                .pSwapchains(stack.mallocLong(1).put(0, swapChain.getSwapChain()))
                .swapchainCount(1);

        KHRSwapchain.vkQueuePresentKHR(VulkanManager.getGraphicsQueue(), presentInfo);
    }

    public void resetMappedBuffers() {
        this.mappedStagingVertexBuffer.clear();
        this.mappedStagingIndexBuffer.clear();
    }

    public ShortBuffer getMappedStagingIndexBuffer() {
        return this.mappedStagingIndexBuffer;
    }

    public FloatBuffer getMappedStagingVertexBuffer() {
        return this.mappedStagingVertexBuffer;
    }

    @Override
    public void close() {
        this.commandPool.close();
        this.stagingIndexBuffer.close();
        this.stagingVertexBuffer.close();
    }
}
