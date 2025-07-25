package net.sixunderscore.mocha2d.vulkan.util;

import net.sixunderscore.mocha2d.Window;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

public class ViewportScissor implements AutoCloseable {
    private final VkViewport.Buffer viewport;
    private final VkRect2D.Buffer scissor;

    public ViewportScissor() {
        this.viewport = VkViewport.malloc(1);
        this.viewport.get(0).set(0, 0, Window.getWidth(), Window.getHeight(), 0, 1);
        this.scissor = VkRect2D.malloc(1);
        this.scissor.get(0).set(
                VkOffset2D.malloc().set(0, 0),
                VkExtent2D.malloc().set(Window.getWidth(), Window.getHeight())
        );
    }

    public void update() {
        this.viewport.get(0).set(0, 0, Window.getWidth(), Window.getHeight(), 0, 1);
        this.scissor.get(0).extent().set(Window.getWidth(), Window.getHeight());
    }

    public VkViewport.Buffer getViewport() {
        return this.viewport;
    }

    public VkRect2D.Buffer getScissor() {
        return this.scissor;
    }

    @Override
    public void close() {
        this.viewport.free();
        this.scissor.free();
    }
}
