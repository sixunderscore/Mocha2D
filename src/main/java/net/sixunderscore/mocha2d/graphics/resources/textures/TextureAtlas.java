package net.sixunderscore.mocha2d.graphics.resources.textures;

import net.sixunderscore.mocha2d.Mocha2D;
import net.sixunderscore.mocha2d.graphics.util.GpuBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class TextureAtlas implements AutoCloseable {
    private final int imageIndex;
    private final int width;
    private final int height;
    private final long imageView;
    private final long image;
    private final long allocation;
    private final boolean pixelated;

    public TextureAtlas(int width, int height, boolean pixelated, int imageIndex) {
        this.imageIndex = imageIndex;
        this.pixelated = pixelated;
        this.width = width;
        this.height = height;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(stack)
                    .sType$Default()
                    .imageType(VK14.VK_IMAGE_TYPE_2D)
                    .format(VK14.VK_FORMAT_R8G8B8A8_SRGB)
                    .extent(e -> e.set(this.width, this.height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK14.VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK14.VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK14.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK14.VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED);

            VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(Vma.VMA_MEMORY_USAGE_GPU_ONLY);

            LongBuffer imageBuffer = stack.mallocLong(1);
            PointerBuffer allocationBuffer = stack.mallocPointer(1);
            if (Vma.vmaCreateImage(Mocha2D.RENDER_CONTEXT.getAllocator(), imageCreateInfo, allocationCreateInfo, imageBuffer, allocationBuffer, null) != VK14.VK_SUCCESS) {
                throw new IllegalStateException("Failed to create texture");
            }

            this.image = imageBuffer.get(0);
            this.allocation = allocationBuffer.get(0);
            this.imageView = this.createImageView(stack);
        }
    }

    private long createImageView(MemoryStack stack) {
        VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType$Default()
                .image(this.image)
                .viewType(VK14.VK_IMAGE_VIEW_TYPE_2D)
                .format(VK14.VK_FORMAT_R8G8B8A8_SRGB)
                .components(c -> c.set(VK14.VK_COMPONENT_SWIZZLE_IDENTITY, VK14.VK_COMPONENT_SWIZZLE_IDENTITY, VK14.VK_COMPONENT_SWIZZLE_IDENTITY, VK14.VK_COMPONENT_SWIZZLE_IDENTITY))
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));

        LongBuffer imageViewBuff = stack.mallocLong(1);
        if (VK14.vkCreateImageView(Mocha2D.RENDER_CONTEXT.getLogicalDevice(), imageViewCreateInfo, null, imageViewBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create image view");
        }

        return imageViewBuff.get(0);
    }

    public void recordUploadCommands(MemoryStack stack, VkCommandBuffer cmdBuffer, GpuBuffer stagingImageBuffer) {
        // Pre copy barrier
        VkImageMemoryBarrier2.Buffer imageBarrier = VkImageMemoryBarrier2.calloc(1, stack);
        imageBarrier.get(0)
                .sType$Default()
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_NONE)
                .dstAccessMask(VK14.VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
                .image(this.image)
                .subresourceRange(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1));
        VkDependencyInfo dependencyInfo = VkDependencyInfo.calloc(stack)
                .sType$Default()
                .pImageMemoryBarriers(imageBarrier);
        VK14.vkCmdPipelineBarrier2(cmdBuffer, dependencyInfo);

        VkBufferImageCopy.Buffer imageCopy = VkBufferImageCopy.calloc(1, stack);
        imageCopy.get(0)
                .imageSubresource(s -> s.set(VK14.VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1))
                .imageOffset(off ->  off.set(0, 0, 0))
                .imageExtent(ext -> ext.set(this.width, this.height, 1));

        VK14.vkCmdCopyBufferToImage(cmdBuffer, stagingImageBuffer.getBuffer(), this.image, VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopy);

        // Post copy barrier
        imageBarrier.get(0)
                .srcStageMask(VK14.VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .dstStageMask(VK14.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT)
                .srcAccessMask(VK14.VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK14.VK_ACCESS_2_SHADER_READ_BIT)
                .oldLayout(VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .newLayout(VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VK14.vkCmdPipelineBarrier2(cmdBuffer, dependencyInfo);
    }

    public long getImageView() {
        return this.imageView;
    }

    public int getImageIndex() {
        return this.imageIndex;
    }

    public boolean isPixelated() {
        return this.pixelated;
    }

    public TextureRegion getFull() {
        return new TextureRegion(
                this.imageIndex,
                this.width,
                this.height,
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        );
    }

    public TextureRegion getRegion(int startX, int endX, int startY, int endY) {
        float u0 = (float) startX / (float) this.width;
        float v0 = (float) startY / (float) this.height;
        float u1 = (float) endX / (float) this.width;
        float v1 = (float) endY / (float) this.height;

        return new TextureRegion(
                this.imageIndex,
                endX - startX,
                endY - startY,
                u0, v1,
                u1, v1,
                u0, v0,
                u1, v0
        );
    }

    @Override
    public void close() {
        VK14.vkDestroyImageView(Mocha2D.RENDER_CONTEXT.getLogicalDevice(), this.imageView, null);
        Vma.vmaDestroyImage(Mocha2D.RENDER_CONTEXT.getAllocator(), this.image, this.allocation);
    }
}
