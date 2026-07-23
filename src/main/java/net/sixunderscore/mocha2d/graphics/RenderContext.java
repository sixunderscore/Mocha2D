package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.util.RenderSettings;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class RenderContext implements AutoCloseable {
    private final VkInstance instance;
    private int graphicsQueueFamilyIndex;
    private VkDevice logicalDevice;
    private VkQueue graphicsQueue;
    private long allocator;

    public RenderContext() {
        this.instance = this.createInstanceDebug();
    }

    public void init(RenderSettings settings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String gpuName = settings.getGpuName();
            VkPhysicalDevice physicalDevice = gpuName.isBlank() ? this.pickPhysicalDevice(stack) : this.getPhysicalDeviceForName(stack, gpuName);

            this.graphicsQueueFamilyIndex = this.findGraphicsQueueFamilyIndex(stack, physicalDevice);
            this.logicalDevice = this.createLogicalDevice(stack, physicalDevice);
            this.graphicsQueue = this.obtainGraphicsQueue(stack);
            this.allocator = this.createVmaAllocator(stack, physicalDevice);
        }
    }

    private VkInstance createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationName(stack.UTF8("Mocha2D"))
                    .pEngineName(stack.UTF8("Mocha2D"))
                    .apiVersion(VK14.VK_API_VERSION_1_3);

            PointerBuffer sdlExtensions = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
            if (sdlExtensions == null) {
                throw new IllegalStateException("SDL Vulkan extensions not available");
            }

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledExtensionNames(sdlExtensions);

            PointerBuffer instancePtr = stack.mallocPointer(1);
            if (VK14.vkCreateInstance(instanceCreateInfo, null, instancePtr) != VK14.VK_SUCCESS) {
                throw new IllegalStateException("Failed to create Vulkan Instance");
            }

            return new VkInstance(instancePtr.get(0), instanceCreateInfo);
        }
    }

    private VkInstance createInstanceDebug() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationName(stack.UTF8("Mocha2D"))
                    .pEngineName(stack.UTF8("Mocha2D"))
                    .apiVersion(VK14.VK_API_VERSION_1_3);

            PointerBuffer sdlExtensions = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
            if (sdlExtensions == null) {
                throw new IllegalStateException("SDL Vulkan extensions not available");
            }

            PointerBuffer allExtensions = stack.mallocPointer(sdlExtensions.remaining() + 1)
                    .put(sdlExtensions)
                    .put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
                    .flip();

            PointerBuffer validationLayers = stack.mallocPointer(1)
                    .put(stack.UTF8("VK_LAYER_KHRONOS_validation"))
                    .flip();

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledExtensionNames(allExtensions)
                    .ppEnabledLayerNames(validationLayers);

            PointerBuffer instancePtr = stack.mallocPointer(1);
            if (VK14.vkCreateInstance(instanceCreateInfo, null, instancePtr) != VK14.VK_SUCCESS) {
                throw new IllegalStateException("Failed to create Vulkan Instance");
            }

            VkInstance instance = new VkInstance(instancePtr.get(0), instanceCreateInfo);
            VulkanValidation.createDebugMessenger(stack, instance);
            return instance;
        }
    }

    public List<String> getAvailableGPUs() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer devices = this.enumeratePhysicalDevices(stack);
            int count = devices.capacity();
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            List<String> list = new ArrayList<>(count);

            for (int i = 0; i < count; ++i) {
                VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), this.instance);
                VK14.vkGetPhysicalDeviceProperties(device, properties);

                if (this.gpuSupportsSwapChain(stack, device)) {
                    list.add(properties.deviceNameString());
                }
            }

            return list;
        }
    }

    private VkPhysicalDevice pickPhysicalDevice(MemoryStack stack) {
        PointerBuffer devices = this.enumeratePhysicalDevices(stack);
        int count = devices.capacity();
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
        VkPhysicalDevice fallback = null;

        for (int i = 0; i < count; ++i) {
            VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), this.instance);
            VK14.vkGetPhysicalDeviceProperties(device, properties);

            if (this.gpuSupportsSwapChain(stack, device)) {
                int type = properties.deviceType();

                if (type == VK14.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                    return device;
                } else {
                    fallback = device;
                }
            }
        }

        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException("No SwapChain-Supporting GPU found");
    }

    private VkPhysicalDevice getPhysicalDeviceForName(MemoryStack stack, String gpuName) {
        PointerBuffer devices = this.enumeratePhysicalDevices(stack);
        int count = devices.capacity();
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);

        for (int i = 0; i < count; ++i) {
            VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), this.instance);
            VK14.vkGetPhysicalDeviceProperties(device, properties);

            if (this.gpuSupportsSwapChain(stack, device) && properties.deviceNameString().toLowerCase().contains(gpuName.toLowerCase())) {
                return device;
            }
        }

        throw new IllegalStateException("Selected GPU not found or not suitable for rendering");
    }

    private PointerBuffer enumeratePhysicalDevices(MemoryStack stack) {
        IntBuffer countBuff = stack.mallocInt(1);
        VK14.vkEnumeratePhysicalDevices(this.instance, countBuff, null);
        int count = countBuff.get(0);

        if (count == 0) {
            throw new IllegalStateException("No Vulkan-Supporting GPUs found");
        }

        PointerBuffer devices = stack.mallocPointer(count);
        VK14.vkEnumeratePhysicalDevices(this.instance, countBuff, devices);

        return devices;
    }

    private boolean gpuSupportsSwapChain(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        IntBuffer extensionsCountBuff = stack.mallocInt(1);
        VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, extensionsCountBuff, null);

        try (VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionsCountBuff.get(0))) {
            VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, extensionsCountBuff, availableExtensions);

            for (VkExtensionProperties extensionProperties : availableExtensions) {
                if (extensionProperties.extensionNameString().equals(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
                    return true;
                }
            }

            return false;
        }
    }

    private int findGraphicsQueueFamilyIndex(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        IntBuffer queueFamilyCountBuff = stack.mallocInt(1);
        VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountBuff, null);
        int queueFamilyCount = queueFamilyCountBuff.get(0);

        if (queueFamilyCount == 0) {
            throw new IllegalStateException("No queue families found");
        }

        VkQueueFamilyProperties.Buffer familyPropertiesBuffer = VkQueueFamilyProperties.calloc(queueFamilyCount, stack);
        VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountBuff, familyPropertiesBuffer);

        for (int i = 0; i < queueFamilyCount; ++i) {
            int queueFlags = familyPropertiesBuffer.get(i).queueFlags();

            if ((queueFlags & VK14.VK_QUEUE_GRAPHICS_BIT) != 0) {
                return i;
            }
        }

        throw new IllegalStateException("Found no available graphics queue family");
    }

    private VkQueue obtainGraphicsQueue(MemoryStack stack) {
        VkDeviceQueueInfo2 queueObtainInfo = VkDeviceQueueInfo2.calloc(stack)
                .sType$Default()
                .queueFamilyIndex(this.graphicsQueueFamilyIndex)
                .queueIndex(0);

        PointerBuffer queuePtr = stack.mallocPointer(1);
        VK14.vkGetDeviceQueue2(this.logicalDevice, queueObtainInfo, queuePtr);

        return new VkQueue(queuePtr.get(0), this.logicalDevice);
    }

    private VkDevice createLogicalDevice(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
        deviceQueueCreateInfos.get(0)
                .sType$Default()
                .queueFamilyIndex(this.graphicsQueueFamilyIndex)
                .pQueuePriorities(stack.floats(1.0f));

        VkPhysicalDeviceVulkan12Features vulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack)
                .sType$Default()
                .shaderSampledImageArrayNonUniformIndexing(true)
                .runtimeDescriptorArray(true)
                .bufferDeviceAddress(true);

        VkPhysicalDeviceVulkan13Features vulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack)
                .sType$Default()
                .dynamicRendering(true)
                .synchronization2(true);

        VkPhysicalDeviceFeatures2 physicalDeviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack)
                .sType$Default()
                .pNext(vulkan12Features)
                .pNext(vulkan13Features);

        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                .sType$Default()
                .pQueueCreateInfos(deviceQueueCreateInfos)
                .pNext(physicalDeviceFeatures.address())
                .ppEnabledExtensionNames(stack.pointers(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)));

        PointerBuffer devicePtr = stack.mallocPointer(1);
        if (VK14.vkCreateDevice(physicalDevice, deviceCreateInfo, null, devicePtr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create logical device");
        }

        return new VkDevice(devicePtr.get(0), physicalDevice, deviceCreateInfo);
    }

    private long createVmaAllocator(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        VmaVulkanFunctions vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack).set(this.instance, this.logicalDevice);

        VmaAllocatorCreateInfo allocationCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
                .vulkanApiVersion(VK14.VK_API_VERSION_1_3)
                .instance(this.instance)
                .physicalDevice(physicalDevice)
                .device(this.logicalDevice)
                .flags(Vma.VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT)
                .pVulkanFunctions(vmaVulkanFunctions);

        PointerBuffer allocatorPtr = stack.mallocPointer(1);
        if (Vma.vmaCreateAllocator(allocationCreateInfo, allocatorPtr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create VMA Allocator");
        }

        return allocatorPtr.get(0);
    }

    public VkInstance getInstance() {
        return this.instance;
    }

    public int getGraphicsQueueIndex() {
        return this.graphicsQueueFamilyIndex;
    }

    public VkQueue getGraphicsQueue() {
        return this.graphicsQueue;
    }

    public VkDevice getLogicalDevice() {
        return this.logicalDevice;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return this.logicalDevice.getPhysicalDevice();
    }

    public long getAllocator() {
        return this.allocator;
    }

    @Override
    public void close() {
        // #start-debug
        VulkanValidation.cleanUp();
        // #end-debug

        Vma.vmaDestroyAllocator(this.allocator);
        VK14.vkDestroyDevice(this.logicalDevice, null);
        VK14.vkDestroyInstance(this.instance, null);
    }
}
