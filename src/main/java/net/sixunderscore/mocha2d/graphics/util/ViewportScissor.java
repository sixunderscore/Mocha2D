package net.sixunderscore.mocha2d.graphics.util;

import net.sixunderscore.mocha2d.graphics.Window;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

public class ViewportScissor implements AutoCloseable {
    private final VkViewport.Buffer viewport;
    private final VkRect2D.Buffer scissor;

    public ViewportScissor() {
        int width = Window.getWidth();
        int height = Window.getHeight();

        this.viewport = VkViewport.malloc(1);
        this.viewport.get(0).set(0, 0, width, height, 0, 1);
        this.scissor = VkRect2D.malloc(1);
        this.scissor.get(0).set(
                VkOffset2D.malloc().set(0, 0),
                VkExtent2D.malloc().set(width, height)
        );
    }

    public void update() {
        int width = Window.getWidth();
        int height = Window.getHeight();

        this.viewport.get(0).set(0, 0, width, height, 0, 1);
        this.scissor.get(0).extent().set(width, height);
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
