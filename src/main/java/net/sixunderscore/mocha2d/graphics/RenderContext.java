package net.sixunderscore.mocha2d.graphics;

import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class RenderContext {
    private static VkInstance instance;
    private static int graphicsQueueFamilyIndex;
    private static VkDevice logicalDevice;
    private static VkQueue graphicsQueue;
    private static long allocator;

    public static void init(MemoryStack stack) {
        instance = createInstance(stack);
        VkPhysicalDevice physicalDevice = pickPhysicalDevice(stack);
        graphicsQueueFamilyIndex = findGraphicsQueueFamilyIndex(stack, physicalDevice);
        logicalDevice = createLogicalDevice(stack, physicalDevice);
        graphicsQueue = obtainGraphicsQueue(stack);
        allocator = createVmaAllocator(stack, physicalDevice);
    }

    private static VkInstance createInstance(MemoryStack stack) {
        VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
                .sType$Default()
                .pApplicationName(stack.UTF8("Mocha2D"))
                .pEngineName(stack.UTF8("Mocha2D"))
                .apiVersion(VK14.VK_API_VERSION_1_3);

        PointerBuffer sdlExtensions = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
        if (sdlExtensions == null) {
            throw new IllegalStateException("SDL Vulkan extensions not available");
        }

        // #start-debug
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
        // #end-debug

        /* #start-release
        VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                .sType$Default()
                .pApplicationInfo(applicationInfo)
                .ppEnabledExtensionNames(sdlExtensions);
        #end-release */

        PointerBuffer instancePtr = stack.mallocPointer(1);
        if (VK14.vkCreateInstance(instanceCreateInfo, null, instancePtr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create Vulkan Instance");
        }

        // #start-debug
        VkInstance instance = new VkInstance(instancePtr.get(0), instanceCreateInfo);
        VulkanErrorHandling.createDebugMessenger(stack, instance);
        return instance;
        // #end-debug

        /* #start-release
        return new VkInstance(instancePtr.get(0), instanceCreateInfo);
        #end-release */
    }

    private static VkPhysicalDevice pickPhysicalDevice(MemoryStack stack) {
        IntBuffer countBuff = stack.mallocInt(1);
        VK14.vkEnumeratePhysicalDevices(instance, countBuff, null);
        int count = countBuff.get(0);

        if (count == 0) {
            throw new IllegalStateException("No Vulkan-Supporting GPUs found");
        }

        VkPhysicalDevice fallback = null;

        PointerBuffer devices = stack.mallocPointer(count);
        VK14.vkEnumeratePhysicalDevices(instance, countBuff, devices);

        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);

        for (int i = 0; i < count; ++i) {
            VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), instance);
            VK14.vkGetPhysicalDeviceProperties(device, properties);

            if (gpuSupportsSwapChain(stack, device)) {
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

    private static boolean gpuSupportsSwapChain(MemoryStack stack, VkPhysicalDevice physicalDevice) {
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

    private static int findGraphicsQueueFamilyIndex(MemoryStack stack, VkPhysicalDevice physicalDevice) {
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

    private static VkQueue obtainGraphicsQueue(MemoryStack stack) {
        VkDeviceQueueInfo2 queueObtainInfo = VkDeviceQueueInfo2.calloc(stack)
                .sType$Default()
                .queueFamilyIndex(graphicsQueueFamilyIndex)
                .queueIndex(0);

        PointerBuffer queuePtr = stack.mallocPointer(1);
        VK14.vkGetDeviceQueue2(logicalDevice, queueObtainInfo, queuePtr);

        return new VkQueue(queuePtr.get(0), logicalDevice);
    }

    private static VkDevice createLogicalDevice(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
        deviceQueueCreateInfos.get(0)
                .sType$Default()
                .queueFamilyIndex(graphicsQueueFamilyIndex)
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

    private static long createVmaAllocator(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        VmaVulkanFunctions vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack).set(instance, logicalDevice);

        VmaAllocatorCreateInfo allocationCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
                .vulkanApiVersion(VK14.VK_API_VERSION_1_3)
                .instance(instance)
                .physicalDevice(physicalDevice)
                .device(logicalDevice)
                .flags(Vma.VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT)
                .pVulkanFunctions(vmaVulkanFunctions);

        PointerBuffer allocatorPtr = stack.mallocPointer(1);
        if (Vma.vmaCreateAllocator(allocationCreateInfo, allocatorPtr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create VMA Allocator");
        }

        return allocatorPtr.get(0);
    }

    public static VkInstance getInstance() {
        return instance;
    }

    public static int getGraphicsQueueIndex() {
        return graphicsQueueFamilyIndex;
    }

    public static VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public static VkDevice getLogicalDevice() {
        return logicalDevice;
    }

    public static VkPhysicalDevice getPhysicalDevice() {
        return logicalDevice.getPhysicalDevice();
    }

    public static long getAllocator() {
        return allocator;
    }

    public static void cleanUp() {
        // #start-debug
        VulkanErrorHandling.cleanUp();
        // #end-debug

        Vma.vmaDestroyAllocator(allocator);
        VK14.vkDestroyDevice(logicalDevice, null);
        VK14.vkDestroyInstance(instance, null);
    }
}