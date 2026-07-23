package net.sixunderscore.mocha2d.graphics.render;

import net.sixunderscore.mocha2d.Mocha2D;
import net.sixunderscore.mocha2d.graphics.util.OrthographicCamera;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.util.ColorUtils;
import net.sixunderscore.mocha2d.graphics.util.*;
import org.joml.Matrix2f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.*;

public class FrameResources implements AutoCloseable {
    private static final jdk.internal.misc.Unsafe UNSAFE = jdk.internal.misc.Unsafe.getUnsafe();

    private final CommandPool commandPool;
    private final VkCommandBuffer commandBuffer;

    private final int maxQuads;
    private int writtenQuads;
    private int writtenTransforms;
    private int writtenTints;

    private final GpuBuffer indexBuffer;
    private final long mappedIndexBuffer;
    private int indexWriteOffset;
    private int indexOffset;

    private final GpuBuffer vertexBuffer;
    private final long mappedVertexBuffer;
    private int vertexWriteOffset;

    private final GpuBuffer transformBuffer;
    private final long mappedTransformBuffer;
    private int transformWriteOffset;
    private int transformIndex;
    private final long transformBufferAddress;

    private final GpuBuffer tintBuffer;
    private final long mappedTintBuffer;
    private int tintWriteOffset;
    private int tintIndex;
    private final long tintBufferAddress;

    public FrameResources(MemoryStack stack) {
        this.maxQuads = 0xFFFF / 4; // Unsigned short max value divided by 4 vertices in a quad
        int maxIndices = this.maxQuads * 6; // 6 indices in a quad (two triangles)
        int maxVertices = this.maxQuads * 4; // 4 vertices in a quad

        int indexBufferSizeBytes = maxIndices * Short.BYTES;
        int vertexBufferSizeBytes = maxVertices * VertexData.TOTAL_SIZE_BYTES;
        int transformBufferSizeBytes = this.maxQuads * (Matrix2f.BYTES + Float.BYTES * 2);
        int tintBufferSizeBytes = this.maxQuads * (Float.BYTES * 4);

        this.writtenQuads = 0;
        this.writtenTransforms = 0;
        this.writtenTints = 0;

        this.commandPool = new CommandPool(stack, Mocha2D.RENDER_CONTEXT.getGraphicsQueueIndex(), VK14.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
        this.commandBuffer = this.commandPool.allocateCommandBuffer(stack);

        this.indexBuffer = new GpuBuffer(stack, indexBufferSizeBytes, VK14.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedIndexBuffer = this.indexBuffer.map(stack);
        this.indexOffset = 0;

        this.vertexBuffer = new GpuBuffer(stack, vertexBufferSizeBytes, VK14.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedVertexBuffer = this.vertexBuffer.map(stack);

        this.transformBuffer = new GpuBuffer(stack, transformBufferSizeBytes, VK14.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedTransformBuffer = this.transformBuffer.map(stack);
        this.transformIndex = 0;

        this.writeTransformToBuffer(1f, 0, 0, 1f, 0, 0);

        VkBufferDeviceAddressInfo bufferDeviceAddressInfo = VkBufferDeviceAddressInfo.calloc(stack)
                .sType$Default()
                .buffer(this.transformBuffer.getBuffer());
        this.transformBufferAddress = VK14.vkGetBufferDeviceAddress(Mocha2D.RENDER_CONTEXT.getLogicalDevice(), bufferDeviceAddressInfo);

        this.tintBuffer = new GpuBuffer(stack, tintBufferSizeBytes, VK14.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
        this.mappedTintBuffer = this.tintBuffer.map(stack);
        this.tintIndex = 0;

        this.writeTintToBuffer((byte) 255, (byte) 255, (byte) 255, 0);

        bufferDeviceAddressInfo.buffer(this.tintBuffer.getBuffer());
        this.tintBufferAddress = VK14.vkGetBufferDeviceAddress(Mocha2D.RENDER_CONTEXT.getLogicalDevice(), bufferDeviceAddressInfo);
    }

    public int writeTransformToBuffer(float m00, float m10, float m01, float m11, float originX, float originY) {
        if (++this.writtenTransforms > this.maxQuads) {
            throw new RuntimeException("Cant add more than: " + this.maxQuads + " transforms per frame");
        }

        long ptr = this.mappedTransformBuffer + this.transformWriteOffset;

        UNSAFE.putFloat(ptr, m00); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, m10); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, m01); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, m11); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, originX); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, originY);

        this.transformWriteOffset += (6 * Float.BYTES);

        return this.transformIndex++;
    }

    public int writeTintToBuffer(byte r, byte g, byte b, float a) {
        if (++this.writtenTints > this.maxQuads) {
            throw new RuntimeException("Cant add more than: " + this.maxQuads + " tints per frame");
        }

        long ptr = this.mappedTintBuffer + this.tintWriteOffset;

        UNSAFE.putFloat(ptr, ColorUtils.srgbToLinear(r)); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, ColorUtils.srgbToLinear(g)); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, ColorUtils.srgbToLinear(b)); ptr += Float.BYTES;
        UNSAFE.putFloat(ptr, a);

        this.tintWriteOffset += (4 * Float.BYTES);

        return this.tintIndex++;
    }

    public void writeQuadToBuffers(TextureRegion texture, float bottomLeftX, float bottomLeftY, float bottomRightX, float bottomRightY, float topLeftX, float topLeftY, float topRightX, float topRightY, int transformIndex, int tintIndex) {
        if (++this.writtenQuads > this.maxQuads) {
            throw new RuntimeException("Cant add more than: " + this.maxQuads + " quads per frame");
        }

        // ---- Writing index data for quad ----
        long indexPtr = this.mappedIndexBuffer + this.indexWriteOffset;
        short currentBase = (short) this.indexOffset;

        UNSAFE.putShort(indexPtr, currentBase); indexPtr += Short.BYTES;
        UNSAFE.putShort(indexPtr, (short) (currentBase + 1)); indexPtr += Short.BYTES;
        UNSAFE.putShort(indexPtr, (short) (currentBase + 2)); indexPtr += Short.BYTES;
        UNSAFE.putShort(indexPtr, (short) (currentBase + 1)); indexPtr += Short.BYTES;
        UNSAFE.putShort(indexPtr, (short) (currentBase + 3)); indexPtr += Short.BYTES;
        UNSAFE.putShort(indexPtr, (short) (currentBase + 2));

        this.indexWriteOffset += (6 * Short.BYTES);
        this.indexOffset += 4;

        // ---- Writing vertex data for quad ----
        long vertexPtr = this.mappedVertexBuffer + this.vertexWriteOffset;
        int texIdx = texture.imageIndex();
        int transformIdx = Math.clamp(transformIndex, 0, this.transformIndex - 1);
        int tintIdx = Math.clamp(tintIndex, 0, this.tintIndex - 1);

        UNSAFE.putFloat(vertexPtr, bottomLeftX); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, bottomLeftY); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.topLeftU()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.topLeftV()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, transformIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, tintIdx); vertexPtr += Float.BYTES;

        UNSAFE.putFloat(vertexPtr, bottomRightX); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, bottomRightY); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.topRightU()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.topRightV()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, transformIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, tintIdx); vertexPtr += Float.BYTES;

        UNSAFE.putFloat(vertexPtr, topLeftX); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, topLeftY); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.bottomLeftU()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.bottomLeftV()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, transformIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, tintIdx); vertexPtr += Float.BYTES;

        UNSAFE.putFloat(vertexPtr, topRightX); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, topRightY); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.bottomRightU()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texture.bottomRightV()); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, texIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, transformIdx); vertexPtr += Float.BYTES;
        UNSAFE.putFloat(vertexPtr, tintIdx);

        this.vertexWriteOffset += (28 * Float.BYTES);
    }

    public void recordCommands(MemoryStack stack, SwapChain swapChain, ResourceManager resourceManager, ViewportScissor viewportScissor, GraphicsPipeline pipeline, int imageIndex, VkClearColorValue clearColorValue, OrthographicCamera camera) {
        VK14.vkResetCommandPool(Mocha2D.RENDER_CONTEXT.getLogicalDevice(), this.commandPool.getPool(), 0);
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
        VK14.vkCmdPushConstants(this.commandBuffer, pipeline.getLayout(), VK14.VK_SHADER_STAGE_FRAGMENT_BIT, Matrix4f.BYTES + 8, stack.longs(this.tintBufferAddress));

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
        VK14.vkCmdBindDescriptorSets(this.commandBuffer, VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayout(), 0, stack.longs(resourceManager.getDescriptorSet()), null);
        VK14.vkCmdSetViewport(this.commandBuffer, 0, viewportScissor.getViewport());
        VK14.vkCmdSetScissor(this.commandBuffer, 0, viewportScissor.getScissor());
        VK14.vkCmdBindVertexBuffers(this.commandBuffer, 0, stack.longs(this.vertexBuffer.getBuffer()), stack.longs(0));
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

        VK14.vkQueueSubmit2(Mocha2D.RENDER_CONTEXT.getGraphicsQueue(), submitInfo, signalFence);
    }

    public void reset() {
        this.writtenQuads = 0;
        this.writtenTransforms = 0;
        this.writtenTints = 0;

        this.vertexWriteOffset = 0;

        this.indexWriteOffset = 0;
        this.indexOffset = 0;

        this.transformWriteOffset = Matrix2f.BYTES + Float.BYTES * 2;
        this.transformIndex = 1;

        this.tintWriteOffset = Float.BYTES * 4;
        this.tintIndex = 1;
    }

    @Override
    public void close() {
        this.commandPool.close();
        this.indexBuffer.close();
        this.vertexBuffer.close();
        this.transformBuffer.close();
        this.tintBuffer.close();
    }
}
