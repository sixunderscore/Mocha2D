package net.sixunderscore.mocha2d.graphics.util;

import net.sixunderscore.mocha2d.graphics.RenderContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.LongBuffer;

public class GpuBuffer implements AutoCloseable {
    private final long buffer;
    private final long allocation;
    private boolean isMapped;

    public GpuBuffer(MemoryStack stack, long sizeBytes, int usage, int memoryUsage) {
        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                .sType$Default()
                .size(sizeBytes)
                .usage(usage)
                .sharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE);

        VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(memoryUsage);

        LongBuffer bufferPtr = stack.mallocLong(1);
        PointerBuffer allocationPtr = stack.mallocPointer(1);
        if (Vma.vmaCreateBuffer(RenderContext.getAllocator(), bufferCreateInfo, allocationCreateInfo, bufferPtr, allocationPtr, null) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create buffer");
        }

        this.buffer = bufferPtr.get(0);
        this.allocation = allocationPtr.get(0);
        this.isMapped = false;
    }

    public long map(MemoryStack stack) {
        if (this.isMapped) {
            throw new IllegalStateException("Buffer can only be mapped once");
        }

        PointerBuffer mappedBufferPtr = stack.mallocPointer(1);
        Vma.vmaMapMemory(RenderContext.getAllocator(), this.allocation, mappedBufferPtr);
        this.isMapped = true;

        return mappedBufferPtr.get(0);
    }

    public long getBuffer() {
        return this.buffer;
    }

    @Override
    public void close() {
        if (this.isMapped) {
            Vma.vmaUnmapMemory(RenderContext.getAllocator(), this.allocation);
        }

        Vma.vmaDestroyBuffer(RenderContext.getAllocator(), this.buffer, this.allocation);
    }
}
