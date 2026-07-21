package net.sixunderscore.mocha2d.graphics;

import org.lwjgl.vulkan.VkPhysicalDevice;

public class GPU {
    private final VkPhysicalDevice handle;
    private final String name;
    private final boolean isIntegrated;

    public GPU(VkPhysicalDevice handle, String name, boolean isIntegrated) {
        this.handle = handle;
        this.name = name;
        this.isIntegrated = isIntegrated;
    }

    protected VkPhysicalDevice getHandle() {
        return this.handle;
    }

    public String getName() {
        return this.name;
    }

    public boolean isIntegrated() {
        return this.isIntegrated;
    }
}
