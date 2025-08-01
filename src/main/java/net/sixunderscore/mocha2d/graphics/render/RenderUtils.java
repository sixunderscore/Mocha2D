package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.vulkan.util.GpuBuffer;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.OrthographicCamera;
import net.sixunderscore.mocha2d.vulkan.util.GraphicsPipeline;
import net.sixunderscore.mocha2d.vulkan.util.SwapChain;
import net.sixunderscore.mocha2d.vulkan.util.ViewportScissor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

public class RenderUtils {
    public static void recordTransferCommands(MemoryStack stack, VkCommandBuffer commandBuffer, GpuBuffer stagingIndexBuffer, GpuBuffer indexBuffer, int indexBufferSizeBytes, GpuBuffer stagingVertexBuffer, GpuBuffer vertexBuffer, int vertexBufferSizeBytes) {
        VkBufferCopy.Buffer bufferCopy = VkBufferCopy.malloc(1, stack);

        if (indexBufferSizeBytes > 0) {
            bufferCopy.get(0).set(0, 0, indexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(commandBuffer, stagingIndexBuffer.getBuffer(), indexBuffer.getBuffer(), bufferCopy);
        }

        if (vertexBufferSizeBytes > 0) {
            bufferCopy.get(0).set(0, 0, vertexBufferSizeBytes);
            VK14.vkCmdCopyBuffer(commandBuffer, stagingVertexBuffer.getBuffer(), vertexBuffer.getBuffer(), bufferCopy);
        }
    }

    public static void recordGraphicsCommands(MemoryStack stack, VkCommandBuffer commandBuffer, SwapChain swapChain, TextureManager textureManager, ViewportScissor viewportScissor, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, int indexCount, GraphicsPipeline pipeline, int imageIndex, OrthographicCamera camera) {
        VkImageMemoryBarrier2.Buffer imageBarriers = VkImageMemoryBarrier2.calloc(2, stack);
        imageBarriers.get(0)
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
        imageBarriers.get(1)
                .sType$Default()
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK14.VK_ACCESS_2_NONE)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .newLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .image(swapChain.getImage(imageIndex))
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));
        VkDependencyInfo barrierDependencyInfo = VkDependencyInfo.calloc(stack)
                .sType$Default()
                .pImageMemoryBarriers(imageBarriers);
        VK14.vkCmdPipelineBarrier2(commandBuffer, barrierDependencyInfo);

        VK14.vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, camera.getBuffer());

        VkClearValue clearValue = VkClearValue.calloc(stack)
                .color(clear -> clear
                        .float32(0, 0)
                        .float32(1, 0)
                        .float32(2, 0)
                        .float32(3, 0)
                );
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
        VK14.vkCmdBeginRendering(commandBuffer, renderingInfo);

        VK14.vkCmdBindPipeline(commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());
        VK14.vkCmdBindDescriptorSets(commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, stack.mallocLong(1).put(0, textureManager.getDescriptorSet()), null);
        VK14.vkCmdSetViewport(commandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(commandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(commandBuffer, 0, stack.mallocLong(1).put(0, vertexBuffer.getBuffer()), stack.mallocLong(1).put(0, 0));
        VK14.vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getBuffer(), 0, VK14.VK_INDEX_TYPE_UINT16);

        VK14.vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);

        VK14.vkCmdEndRendering(commandBuffer);
    }

    public static void submitCommandBuffer(MemoryStack stack, VkCommandBuffer commandBuffer, long waitSemaphore, long signalSemaphore, long signalFence) {
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
                .commandBuffer(commandBuffer);

        VkSubmitInfo2.Buffer submitInfo = VkSubmitInfo2.calloc(1, stack);
        submitInfo.get(0)
                .sType$Default()
                .pWaitSemaphoreInfos(waitSemaphoreSubmitInfo)
                .pSignalSemaphoreInfos(signalSemaphoreSubmitInfo)
                .pCommandBufferInfos(commandBufferSubmitInfo);

        VK14.vkQueueSubmit2(VulkanManager.getGraphicsQueue(), submitInfo, signalFence);
    }

    public static void presentImageToSwapChain(MemoryStack stack, SwapChain swapChain, IntBuffer imageIndex, long waitSemaphore) {
        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType$Default()
                .pImageIndices(imageIndex)
                .pWaitSemaphores(stack.mallocLong(1).put(0, waitSemaphore))
                .pSwapchains(stack.mallocLong(1).put(0, swapChain.getSwapChain()))
                .swapchainCount(1);

        KHRSwapchain.vkQueuePresentKHR(VulkanManager.getGraphicsQueue(), presentInfo);
    }
}
