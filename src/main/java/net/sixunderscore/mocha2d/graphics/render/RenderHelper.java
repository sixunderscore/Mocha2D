package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.vulkan.util.GpuBuffer;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.OrthographicCamera;
import net.sixunderscore.mocha2d.vulkan.util.GraphicsPipeline;
import net.sixunderscore.mocha2d.vulkan.util.SwapChain;
import net.sixunderscore.mocha2d.vulkan.util.ViewportScissor;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class RenderHelper implements AutoCloseable {
    private final VkCommandBufferBeginInfo cmdBufferBeginInfo;
    private final VkBufferCopy.Buffer bufferCopy;
    private final VkClearValue clearValue;
    private final VkRenderingAttachmentInfo.Buffer colorAttachments;
    private final VkRenderingInfo renderingInfo;
    private final VkImageMemoryBarrier2.Buffer imageBarriers;
    private final VkDependencyInfo barrierDependencyInfo;
    private final VkSemaphoreSubmitInfo.Buffer waitSemaphoreSubmitInfo;
    private final VkSemaphoreSubmitInfo.Buffer signalSemaphoreSubmitInfo;
    private final VkCommandBufferSubmitInfo.Buffer cmdBufferSubmitInfo;
    private final VkSubmitInfo2.Buffer submitInfo;
    private final VkPresentInfoKHR presentInfo;

    private final LongBuffer vertexBufferHandleBuffer;
    private final LongBuffer vertexBufferOffsetBuffer;
    private final LongBuffer waitPresentSemaphoreBuffer;
    private final LongBuffer swapChainHandleBuffer;
    private final LongBuffer descriptorSetHandleBuffer;

    public RenderHelper(TextureManager textureManager, GpuBuffer vertexBuffer) {
        this.cmdBufferBeginInfo = VkCommandBufferBeginInfo.calloc()
                .sType$Default()
                .flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        this.bufferCopy = VkBufferCopy.calloc(1);

        this.clearValue = VkClearValue.calloc()
                .color(color -> color
                        .float32(0, 0)
                        .float32(1, 0)
                        .float32(2, 0)
                        .float32(3, 0)
                );

        this.colorAttachments = VkRenderingAttachmentInfo.calloc(1);
        this.colorAttachments.get(0)
                .sType$Default()
                .imageLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK14.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK14.VK_ATTACHMENT_STORE_OP_STORE)
                .clearValue(this.clearValue);

        this.renderingInfo = VkRenderingInfo.calloc()
                .sType$Default()
                .layerCount(1)
                .pColorAttachments(this.colorAttachments);

        this.imageBarriers = VkImageMemoryBarrier2.calloc(2);
        this.imageBarriers.get(0)
                .sType$Default()
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_NONE)
                .dstAccessMask(VK14.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));

        this.imageBarriers.get(1)
                .sType$Default()
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK14.VK_ACCESS_2_NONE)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .newLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));

        this.barrierDependencyInfo = VkDependencyInfo.calloc()
                .sType$Default()
                .pImageMemoryBarriers(this.imageBarriers);

        this.waitSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1);
        this.waitSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        this.signalSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1);
        this.signalSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        this.cmdBufferSubmitInfo = VkCommandBufferSubmitInfo.calloc(1);
        this.cmdBufferSubmitInfo.get(0)
                .sType$Default();

        this.submitInfo = VkSubmitInfo2.calloc(1);
        this.submitInfo.get(0)
                .sType$Default()
                .pWaitSemaphoreInfos(this.waitSemaphoreSubmitInfo)
                .pSignalSemaphoreInfos(this.signalSemaphoreSubmitInfo)
                .pCommandBufferInfos(this.cmdBufferSubmitInfo);

        this.presentInfo = VkPresentInfoKHR.calloc()
                .sType$Default()
                .swapchainCount(1);

        this.vertexBufferHandleBuffer = MemoryUtil.memAllocLong(1).put(0, vertexBuffer.getBuffer());
        this.vertexBufferOffsetBuffer = MemoryUtil.memAllocLong(1).put(0, 0);
        this.descriptorSetHandleBuffer = MemoryUtil.memAllocLong(1).put(0, textureManager.getDescriptorSet());
        this.waitPresentSemaphoreBuffer = MemoryUtil.memAllocLong(1);
        this.swapChainHandleBuffer = MemoryUtil.memAllocLong(1);
    }

    public VkCommandBufferBeginInfo getCommandBufferBeginInfo() {
        return this.cmdBufferBeginInfo;
    }

    public void recordTransferCommands(VkCommandBuffer commandBuffer, GpuBuffer stagingIndexBuffer, GpuBuffer indexBuffer, int indexBufferSizeBytes, GpuBuffer stagingVertexBuffer, GpuBuffer vertexBuffer, int vertexBufferSizeBytes) {
        if (indexBufferSizeBytes > 0) {
            this.bufferCopy.get(0).set(0, 0, indexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(commandBuffer, stagingIndexBuffer.getBuffer(), indexBuffer.getBuffer(), this.bufferCopy);
        }

        if (vertexBufferSizeBytes > 0) {
            this.bufferCopy.get(0).set(0, 0, vertexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(commandBuffer, stagingVertexBuffer.getBuffer(), vertexBuffer.getBuffer(), this.bufferCopy);
        }
    }

    public void recordGraphicsCommands(VkCommandBuffer commandBuffer, SwapChain swapChain, ViewportScissor viewportScissor, GpuBuffer indexBuffer, int indexCount, GraphicsPipeline pipeline, int imageIndex, OrthographicCamera camera) {
        this.imageBarriers.get(0).image(swapChain.getImage(imageIndex));
        this.imageBarriers.get(1).image(swapChain.getImage(imageIndex));
        VK14.vkCmdPipelineBarrier2(commandBuffer, this.barrierDependencyInfo);

        VK14.vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, camera.getBuffer());

        this.colorAttachments.get(0).imageView(swapChain.getImageView(imageIndex));
        this.renderingInfo.renderArea(viewportScissor.getScissor().get(0));

        VK14.vkCmdBeginRendering(commandBuffer, this.renderingInfo);

        VK14.vkCmdBindPipeline(commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());
        VK14.vkCmdBindDescriptorSets(commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, this.descriptorSetHandleBuffer, null);
        VK14.vkCmdSetViewport(commandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(commandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(commandBuffer, 0, this.vertexBufferHandleBuffer, this.vertexBufferOffsetBuffer);
        VK14.vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getBuffer(), 0, VK14.VK_INDEX_TYPE_UINT16);

        VK14.vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);

        VK14.vkCmdEndRendering(commandBuffer);
    }

    public void submitCommandBuffer(VkCommandBuffer commandBuffer, long waitSemaphore, long signalSemaphore, long signalFence) {
        this.waitSemaphoreSubmitInfo.get(0).semaphore(waitSemaphore);
        this.signalSemaphoreSubmitInfo.get(0).semaphore(signalSemaphore);
        this.cmdBufferSubmitInfo.get(0).commandBuffer(commandBuffer);

        VK14.vkQueueSubmit2(VulkanManager.getGraphicsQueue(), this.submitInfo, signalFence);
    }

    public void presentImageToSwapChain(SwapChain swapChain, IntBuffer imageIndex, long waitSemaphore) {
        this.presentInfo
                .pImageIndices(imageIndex)
                .pWaitSemaphores(this.waitPresentSemaphoreBuffer.put(0, waitSemaphore))
                .pSwapchains(this.swapChainHandleBuffer.put(0, swapChain.getSwapChain()));

        KHRSwapchain.vkQueuePresentKHR(VulkanManager.getGraphicsQueue(), this.presentInfo);
    }

    @Override
    public void close() {
        this.cmdBufferBeginInfo.close();
        this.bufferCopy.close();
        this.imageBarriers.close();
        this.barrierDependencyInfo.close();
        this.clearValue.close();
        this.colorAttachments.close();
        this.renderingInfo.close();
        this.waitSemaphoreSubmitInfo.close();
        this.signalSemaphoreSubmitInfo.close();
        this.cmdBufferSubmitInfo.close();
        this.submitInfo.close();
        this.presentInfo.close();

        MemoryUtil.memFree(this.vertexBufferHandleBuffer);
        MemoryUtil.memFree(this.vertexBufferOffsetBuffer);
        MemoryUtil.memFree(this.waitPresentSemaphoreBuffer);
        MemoryUtil.memFree(this.swapChainHandleBuffer);
        MemoryUtil.memFree(this.descriptorSetHandleBuffer);
    }
}
