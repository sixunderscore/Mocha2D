package net.sixunderscore.mocha2d.graphics.util;

import net.sixunderscore.mocha2d.graphics.RenderContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

public class CommandPool implements AutoCloseable {
    private final long pool;

    public CommandPool(MemoryStack stack, int queueFamilyIndex, int flags) {
        VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType$Default()
                .queueFamilyIndex(queueFamilyIndex)
                .flags(flags);

        LongBuffer commandPoolBuff = stack.mallocLong(1);
        if (VK14.vkCreateCommandPool(RenderContext.getLogicalDevice(), commandPoolCreateInfo, null, commandPoolBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create command pool");
        }

        this.pool = commandPoolBuff.get(0);
    }

    public VkCommandBuffer allocateCommandBuffer(MemoryStack stack) {
        VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType$Default()
                .commandBufferCount(1)
                .commandPool(this.pool)
                .level(VK14.VK_COMMAND_BUFFER_LEVEL_PRIMARY);

        PointerBuffer commandBufferPtr = stack.mallocPointer(1);
        if (VK14.vkAllocateCommandBuffers(RenderContext.getLogicalDevice(), allocateInfo, commandBufferPtr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to allocate command buffer from pool");
        }

        return new VkCommandBuffer(commandBufferPtr.get(0), RenderContext.getLogicalDevice());
    }

    public long getPool() {
        return this.pool;
    }

    @Override
    public void close() {
        VK14.vkDestroyCommandPool(RenderContext.getLogicalDevice(), this.pool, null);
    }
}
