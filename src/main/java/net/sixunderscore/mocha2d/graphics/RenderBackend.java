package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.vulkan.util.SwapChain;
import net.sixunderscore.mocha2d.vulkan.util.ViewportScissor;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK14;

public class RenderBackend implements AutoCloseable {
    private final long windowSurface;
    private final ViewportScissor viewportScissor;
    private final SwapChain swapChain;
    private boolean shouldRebuildSwapChain;
    private final ResourceManager resourceManager;
    private final BatchRenderer batch;

    public RenderBackend(long window, TextureFile[] textureFiles, TtfFile[] ttfFiles) {
        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new IllegalStateException("Unable to find Vulkan loader");
        }

        VulkanManager.init();

        long[] surfaceArr = new long[1];
        if (GLFWVulkan.glfwCreateWindowSurface(VulkanManager.getInstance(), window, null, surfaceArr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create Vulkan surface");
        }
        this.windowSurface = surfaceArr[0];
        this.viewportScissor = new ViewportScissor();
        this.swapChain = new SwapChain(windowSurface, viewportScissor.getScissor().extent());
        this.shouldRebuildSwapChain = false;
        this.resourceManager = new ResourceManager(textureFiles, ttfFiles);
        this.batch = new BatchRenderer(this.resourceManager, this.swapChain);
    }

    public void onWindowResize() {
        this.shouldRebuildSwapChain = true;
    }

    public void render(OrthographicCamera camera, Screen screen) {
        if (this.shouldRebuildSwapChain) {
            this.viewportScissor.update();
            this.swapChain.rebuild(batch.getInFlightFences(), this.windowSurface, this.viewportScissor.getScissor().extent());
            this.shouldRebuildSwapChain = false;
        }

        screen.render(this.batch);
        this.batch.draw(camera, this.swapChain, this.resourceManager, this.viewportScissor);
    }

    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public void setClearColor(byte r, byte g, byte b) {
        this.batch.setClearColor(r, g, b);
    }

    @Override
    public void close() {
        this.batch.close();
        this.resourceManager.close();
        this.viewportScissor.close();
        this.swapChain.close();
        KHRSurface.vkDestroySurfaceKHR(VulkanManager.getInstance(), this.windowSurface, null);
        VulkanManager.cleanUp();
    }
}
