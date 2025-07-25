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
    private final VkCommandBufferBeginInfo cmdBufferBeginInfo = VkCommandBufferBeginInfo.calloc();
    private final VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1);
    private final VkRenderingAttachmentInfo.Buffer colorAttachments = VkRenderingAttachmentInfo.calloc(1);
    private final VkRenderingInfo renderingInfo = VkRenderingInfo.calloc();
    private final VkImageMemoryBarrier2.Buffer imageBarriers = VkImageMemoryBarrier2.calloc(2);
    private final VkDependencyInfo barrierDependencyInfo = VkDependencyInfo.calloc();
    private final VkSemaphoreSubmitInfo.Buffer waitSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1);
    private final VkSemaphoreSubmitInfo.Buffer signalSemaphoreSubmitInfo = VkSemaphoreSubmitInfo.calloc(1);
    private final VkCommandBufferSubmitInfo.Buffer cmdBufferSubmitInfo = VkCommandBufferSubmitInfo.calloc(1);
    private final VkSubmitInfo2.Buffer submitInfo = VkSubmitInfo2.calloc(1);
    private final VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc();

    private final LongBuffer vertexBufferHandleBuffer;
    private final LongBuffer vertexBufferOffsetBuffer;
    private final LongBuffer waitPresentSemaphoreBuffer;
    private final LongBuffer swapChainHandleBuffer;
    private final LongBuffer descriptorSetHandleBuffer;

    public RenderHelper(TextureManager textureManager, GpuBuffer vertexBuffer) {
        this.cmdBufferBeginInfo
                .sType$Default()
                .flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        this.colorAttachments.get(0)
                .sType$Default()
                .imageLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK14.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK14.VK_ATTACHMENT_STORE_OP_STORE)
                .clearValue(VkClearValue.calloc());

        this.renderingInfo
                .sType$Default()
                .layerCount(1)
                .pColorAttachments(this.colorAttachments);

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

        this.barrierDependencyInfo
                .sType$Default()
                .pImageMemoryBarriers(this.imageBarriers);

        this.waitSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        this.signalSemaphoreSubmitInfo.get(0)
                .sType$Default()
                .stageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);

        this.cmdBufferSubmitInfo.get(0)
                .sType$Default();

        this.submitInfo.get(0)
                .sType$Default()
                .pWaitSemaphoreInfos(this.waitSemaphoreSubmitInfo)
                .pSignalSemaphoreInfos(this.signalSemaphoreSubmitInfo)
                .pCommandBufferInfos(this.cmdBufferSubmitInfo);

        this.presentInfo
                .sType$Default()
                .swapchainCount(1);

        this.vertexBufferHandleBuffer = MemoryUtil.memAllocLong(1).put(0, vertexBuffer.getBuffer());
        this.vertexBufferOffsetBuffer = MemoryUtil.memAllocLong(1).put(0, 0);
        this.descriptorSetHandleBuffer = MemoryUtil.memAllocLong(1).put(0, textureManager.getDescriptorSet());
        this.waitPresentSemaphoreBuffer = MemoryUtil.memAllocLong(1);
        this.swapChainHandleBuffer = MemoryUtil.memAllocLong(1);
    }

    public VkCommandBufferBeginInfo getCmdBufferBeginInfo() {
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

    public void recordGraphicsCommands(VkCommandBuffer graphicsCommandBuffer, SwapChain swapChain, ViewportScissor viewportScissor, GpuBuffer indexBuffer, int indexCount, GraphicsPipeline pipeline, int imageIndex, OrthographicCamera camera) {
        this.imageBarriers.get(0).image(swapChain.getImage(imageIndex));
        this.imageBarriers.get(1).image(swapChain.getImage(imageIndex));
        VK14.vkCmdPipelineBarrier2(graphicsCommandBuffer, this.barrierDependencyInfo);

        VK14.vkCmdPushConstants(graphicsCommandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, camera.getBuffer());

        this.colorAttachments.get(0).imageView(swapChain.getImageView(imageIndex));
        this.renderingInfo.renderArea(viewportScissor.getScissor().get(0));

        VK14.vkCmdBeginRendering(graphicsCommandBuffer, this.renderingInfo);

        VK14.vkCmdBindPipeline(graphicsCommandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());
        VK14.vkCmdBindDescriptorSets(graphicsCommandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, this.descriptorSetHandleBuffer, null);
        VK14.vkCmdSetViewport(graphicsCommandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(graphicsCommandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(graphicsCommandBuffer, 0, this.vertexBufferHandleBuffer, this.vertexBufferOffsetBuffer);
        VK14.vkCmdBindIndexBuffer(graphicsCommandBuffer, indexBuffer.getBuffer(), 0, VK14.VK_INDEX_TYPE_UINT16);

        VK14.vkCmdDrawIndexed(graphicsCommandBuffer, indexCount, 1, 0, 0, 0);

        VK14.vkCmdEndRendering(graphicsCommandBuffer);
    }

    public void submitGraphicsCmdBuffer(VkCommandBuffer graphicsCmdBuffer, long waitSemaphore, long signalSemaphore, long signalFence) {
        this.waitSemaphoreSubmitInfo.get(0).semaphore(waitSemaphore);
        this.signalSemaphoreSubmitInfo.get(0).semaphore(signalSemaphore);
        this.cmdBufferSubmitInfo.get(0).commandBuffer(graphicsCmdBuffer);

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
        this.cmdBufferBeginInfo.free();
        this.bufferCopy.free();
        this.imageBarriers.free();
        this.barrierDependencyInfo.free();
        this.colorAttachments.free();
        this.renderingInfo.free();
        this.waitSemaphoreSubmitInfo.free();
        this.signalSemaphoreSubmitInfo.free();
        this.cmdBufferSubmitInfo.free();
        this.submitInfo.free();

        MemoryUtil.memFree(this.vertexBufferHandleBuffer);
        MemoryUtil.memFree(this.vertexBufferOffsetBuffer);
        MemoryUtil.memFree(this.waitPresentSemaphoreBuffer);
        MemoryUtil.memFree(this.swapChainHandleBuffer);
        MemoryUtil.memFree(this.descriptorSetHandleBuffer);
    }
}
