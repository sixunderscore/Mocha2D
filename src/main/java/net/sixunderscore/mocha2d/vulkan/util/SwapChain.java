package net.sixunderscore.mocha2d.vulkan.util;

import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class SwapChain implements AutoCloseable {
    private final int imageCount;
    private final int imageFormat;
    private final boolean vSyncEnabled;
    private long swapChain;
    private long[] images;
    private long[] imageViews;

    public SwapChain(long surface, VkExtent2D extent, boolean vSyncEnabled) {
        this(surface, extent, vSyncEnabled, VK14.VK_NULL_HANDLE);
    }

    public SwapChain(long surface, VkExtent2D extent, boolean vSyncEnabled, long oldSwapChain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.imageCount = this.getImageCount(stack, surface);
            this.imageFormat = this.getImageFormat(stack, surface);
            this.vSyncEnabled = vSyncEnabled;

            this.swapChain = this.createSwapChain(stack, surface, extent, oldSwapChain);
            this.images = this.getSwapChainImages();
            this.imageViews = this.createImageViews(stack);
        }
    }

    private int getImageCount(MemoryStack stack, long surface) {
        VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(VulkanManager.getPhysicalDevice(), surface, surfaceCapabilities);

        int max = surfaceCapabilities.maxImageCount();
        int preferred = surfaceCapabilities.minImageCount() + 1;

        if (max > 0 && preferred > max) {
            return max;
        }

        return preferred;
    }

    private int getImageFormat(MemoryStack stack, long surface) {
        VkPhysicalDevice physicalDevice = VulkanManager.getPhysicalDevice();

        IntBuffer formatCount = stack.mallocInt(1);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null);

        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(formatCount.get(0), stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, formats);

        int fallbackFormat = 0;

        for (VkSurfaceFormatKHR format : formats) {
            int formatType = format.format();

            if (formatType == VK14.VK_FORMAT_R8G8B8A8_SRGB) {
                return formatType;
            } else if (formatType == VK14.VK_FORMAT_B8G8R8A8_SRGB) {
                fallbackFormat = formatType;
            }
        }

        if (fallbackFormat != 0) {
            return fallbackFormat;
        } else {
            throw new IllegalStateException("GPU does not support VK_FORMAT_R8G8B8A8_SRGB or VK_FORMAT_B8G8R8A8_SRGB for SwapChain images");
        }
    }

    private long createSwapChain(MemoryStack stack, long surface, VkExtent2D extent2D, long oldSwapChain) {
        VkSwapchainCreateInfoKHR swapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType$Default()
                .surface(surface)
                .minImageCount(this.imageCount)
                .imageFormat(this.imageFormat)
                .imageColorSpace(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .imageExtent(extent2D)
                .imageArrayLayers(1)
                .imageUsage(VK14.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .imageSharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(this.vSyncEnabled ? KHRSurface.VK_PRESENT_MODE_FIFO_KHR : KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR)
                .clipped(true)
                .oldSwapchain(oldSwapChain);

        LongBuffer swapChainBuff = stack.mallocLong(1);
        if (KHRSwapchain.vkCreateSwapchainKHR(VulkanManager.getLogicalDevice(), swapChainCreateInfo, null, swapChainBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create SwapChain");
        }

        return swapChainBuff.get(0);
    }

    private long[] getSwapChainImages() {
        int[] swapChainImageCount = new int[1];
        KHRSwapchain.vkGetSwapchainImagesKHR(VulkanManager.getLogicalDevice(), this.swapChain, swapChainImageCount, null);

        long[] swapChainImages = new long[swapChainImageCount[0]];
        KHRSwapchain.vkGetSwapchainImagesKHR(VulkanManager.getLogicalDevice(), this.swapChain, swapChainImageCount, swapChainImages);

        return swapChainImages;
    }

    private long[] createImageViews(MemoryStack stack) {
        long[] imageViews = new long[this.imageCount];

        VkComponentMapping componentMapping = VkComponentMapping.calloc(stack)
                .r(VK14.VK_COMPONENT_SWIZZLE_IDENTITY)
                .g(VK14.VK_COMPONENT_SWIZZLE_IDENTITY)
                .b(VK14.VK_COMPONENT_SWIZZLE_IDENTITY)
                .a(VK14.VK_COMPONENT_SWIZZLE_IDENTITY);

        VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc(stack)
                .aspectMask(VK14.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType$Default()
                .viewType(VK14.VK_IMAGE_VIEW_TYPE_2D)
                .format(this.imageFormat)
                .components(componentMapping)
                .subresourceRange(subresourceRange);

        LongBuffer imageViewBuff = stack.mallocLong(1);

        for (int i = 0; i < this.imageCount; ++i) {
            imageViewCreateInfo.image(this.images[i]);
            if (VK14.vkCreateImageView(VulkanManager.getLogicalDevice(), imageViewCreateInfo, null, imageViewBuff) != VK14.VK_SUCCESS) {
                throw new IllegalStateException("Failed to create SwapChain image view");
            }

            imageViews[i] = imageViewBuff.get(0);
        }

        return imageViews;
    }

    public void rebuild(long[] waitFences, long surface, VkExtent2D extent) {
        VK14.vkWaitForFences(VulkanManager.getLogicalDevice(), waitFences, true, Long.MAX_VALUE);

        long oldSwapChain = this.swapChain;
        long[] oldImageViews = this.imageViews;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.swapChain = this.createSwapChain(stack, surface, extent, oldSwapChain);
            this.images = this.getSwapChainImages();
            this.imageViews = this.createImageViews(stack);
        }

        this.cleanUp(oldImageViews, oldSwapChain);
    }

    public long getSwapChain() {
        return this.swapChain;
    }

    public long getImage(int index) {
        return this.images[index];
    }

    public long getImageView(int index) {
        return this.imageViews[index];
    }

    public int getImageCount() {
        return this.imageCount;
    }

    public int getImageFormat() {
        return this.imageFormat;
    }

    private void cleanUp(long[] imageViews, long swapChain) {
        VkDevice logicalDevice = VulkanManager.getLogicalDevice();

        for (long imageView : imageViews) {
            VK14.vkDestroyImageView(logicalDevice, imageView, null);
        }
        KHRSwapchain.vkDestroySwapchainKHR(logicalDevice, swapChain, null);
    }

    @Override
    public void close() {
        this.cleanUp(this.imageViews, this.swapChain);
    }
}
