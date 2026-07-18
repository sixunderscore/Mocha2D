package net.sixunderscore.mocha2d.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanErrorHandling {
    private static long debugMessengerHandle;

    public static void createDebugMessenger(MemoryStack stack, VkInstance vkInstance) {
        VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType$Default()
                .messageSeverity(
                        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageType(
                        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                .pfnUserCallback(VulkanErrorHandling::errorCallback);

        LongBuffer debugMessengerBuffer = stack.mallocLong(1);
        if (EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance, debugCreateInfo, null, debugMessengerBuffer) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to set up debug messenger");
        }
        debugMessengerHandle = debugMessengerBuffer.get(0);
    }

    private static int errorCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("Vulkan Validation: Error type: " + getType(messageType) + " Severity: " + getSeverity(messageSeverity) +
                "\n\tMessageId: " + data.pMessageIdNameString() +
                "\n\tMessage: " + data.pMessageString());

        return VK14.VK_FALSE;
    }

    private static String getSeverity(int messageSeverity) {
        if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            return "VERBOSE";
        } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            return "INFO";
        } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            return "WARNING";
        } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            return "ERROR";
        } else {
            return "UNKNOWN";
        }
    }

    private static String getType(int messageType) {
        if ((messageType & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
            return "GENERAL";
        } else if ((messageType & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
            return "VALIDATION";
        } else if ((messageType & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
            return "PERFORMANCE";
        } else {
            return "UNKNOWN";
        }
    }

    public static void cleanUp() {
        if (debugMessengerHandle != 0) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(RenderContext.getInstance(), debugMessengerHandle, null);
        }
    }
}
