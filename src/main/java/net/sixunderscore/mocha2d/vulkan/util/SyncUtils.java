package net.sixunderscore.mocha2d.vulkan.util;

import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class SyncUtils {
    public static long createSemaphore(MemoryStack stack) {
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType$Default();

        LongBuffer semaphoreBuff = stack.mallocLong(1);
        if (VK14.vkCreateSemaphore(VulkanManager.getLogicalDevice(), semaphoreCreateInfo, null, semaphoreBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create semaphore");
        }

        return semaphoreBuff.get(0);
    }

    public static long createFence(MemoryStack stack, boolean signaled) {
        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
                .sType$Default();

        if (signaled) {
            fenceCreateInfo.flags(VK14.VK_FENCE_CREATE_SIGNALED_BIT);
        }

        LongBuffer fenceBuff = stack.mallocLong(1);
        if (VK14.vkCreateFence(VulkanManager.getLogicalDevice(), fenceCreateInfo, null, fenceBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create fence");
        }

        return fenceBuff.get(0);
    }
}
