package net.sixunderscore.mocha2d.vulkan.util;

import net.sixunderscore.mocha2d.graphics.render.VertexData;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.ResourceUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class GraphicsPipeline implements AutoCloseable {
    private final long pipelineLayout;
    private final long pipeline;

    public GraphicsPipeline(MemoryStack stack, ResourceManager resourceManager, SwapChain swapChain, String vertexShaderPath, String fragmentShaderPath) {
        this.pipelineLayout = this.createPipelineLayout(stack, resourceManager);
        this.pipeline = this.createPipeline(stack, swapChain, vertexShaderPath, fragmentShaderPath);
    }

    private long createPipelineLayout(MemoryStack stack, ResourceManager resourceManager) {
        // Pass combined view + projection matrix as push constant
        VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
        pushConstantRanges.get(0).set(VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, 16 * Float.BYTES);

        VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType$Default()
                .pSetLayouts(stack.longs(resourceManager.getDescriptorSetLayout()))
                .pPushConstantRanges(pushConstantRanges);

        LongBuffer pipeLineLayoutBuff = stack.mallocLong(1);
        if (VK14.vkCreatePipelineLayout(VulkanManager.getLogicalDevice(), layoutCreateInfo, null, pipeLineLayoutBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create pipeline layout");
        }

        return pipeLineLayoutBuff.get(0);
    }

    private long createPipeline(MemoryStack stack, SwapChain swapChain, String vertexShaderPath, String fragmentShaderPath) {
        long vertexShader = createShaderModule(stack, ResourceUtils.loadRawFile(vertexShaderPath));
        long fragmentShader = createShaderModule(stack, ResourceUtils.loadRawFile(fragmentShaderPath));

        VkPipelineShaderStageCreateInfo.Buffer shaderStageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        shaderStageCreateInfos.get(0)
                .sType$Default()
                .stage(VK14.VK_SHADER_STAGE_VERTEX_BIT)
                .module(vertexShader)
                .pName(stack.ASCII("main"));

        shaderStageCreateInfos.get(1)
                .sType$Default()
                .stage(VK14.VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(fragmentShader)
                .pName(stack.ASCII("main"));

        // Total vertex buffer data
        VkVertexInputBindingDescription.Buffer vertexBindingDescriptions = VkVertexInputBindingDescription.malloc(1, stack);
        vertexBindingDescriptions.get(0).set(0, VertexData.TOTAL_SIZE_BYTES, VK14.VK_VERTEX_INPUT_RATE_VERTEX);

        // How to interpret total vertex buffer data
        VkVertexInputAttributeDescription.Buffer vertexAttributeDescriptions = VkVertexInputAttributeDescription.malloc(5, stack);
        vertexAttributeDescriptions.get(0).set(0, 0, VK14.VK_FORMAT_R32G32_SFLOAT, 0);
        vertexAttributeDescriptions.get(1).set(1, 0, VK14.VK_FORMAT_R32G32_SFLOAT, VertexData.POS_SIZE_BYTES);
        vertexAttributeDescriptions.get(2).set(2, 0, VK14.VK_FORMAT_R32_SFLOAT, VertexData.POS_SIZE_BYTES + VertexData.UV_SIZE_BYTES);
        vertexAttributeDescriptions.get(3).set(3, 0, VK14.VK_FORMAT_R32G32_SFLOAT, VertexData.POS_SIZE_BYTES + VertexData.UV_SIZE_BYTES + VertexData.TEX_INDEX_SIZE_BYTES);
        vertexAttributeDescriptions.get(4).set(4, 0, VK14.VK_FORMAT_R32G32_SFLOAT, VertexData.POS_SIZE_BYTES + VertexData.UV_SIZE_BYTES + VertexData.TEX_INDEX_SIZE_BYTES + VertexData.ROTATION_SIN_AND_COS_SIZE_BYTES);

        VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .pVertexBindingDescriptions(vertexBindingDescriptions)
                .pVertexAttributeDescriptions(vertexAttributeDescriptions)
                .sType$Default();

        // What kind of geometry we will be drawing
        VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType$Default()
                .topology(VK14.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

        // What properties of the pipeline will be mutable
        VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType$Default()
                .pDynamicStates(stack.ints(VK14.VK_DYNAMIC_STATE_VIEWPORT, VK14.VK_DYNAMIC_STATE_SCISSOR));

        VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType$Default()
                .viewportCount(1)
                .scissorCount(1);

        VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType$Default()
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK14.VK_POLYGON_MODE_FILL)
                .lineWidth(1.0f)
                .cullMode(VK14.VK_CULL_MODE_BACK_BIT)
                .frontFace(VK14.VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .depthBiasEnable(false);

        // Anti aliasing (Disable)
        VkPipelineMultisampleStateCreateInfo multiSampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType$Default()
                .rasterizationSamples(VK14.VK_SAMPLE_COUNT_1_BIT)
                .sampleShadingEnable(false);

        // Transparency
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        colorBlendAttachments.get(0)
                .colorWriteMask(VK14.VK_COLOR_COMPONENT_R_BIT | VK14.VK_COLOR_COMPONENT_G_BIT | VK14.VK_COLOR_COMPONENT_B_BIT | VK14.VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(true)
                .srcColorBlendFactor(VK14.VK_BLEND_FACTOR_SRC_ALPHA)
                .dstColorBlendFactor(VK14.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                .colorBlendOp(VK14.VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK14.VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK14.VK_BLEND_FACTOR_ZERO)
                .alphaBlendOp(VK14.VK_BLEND_OP_ADD);

        VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType$Default()
                .logicOpEnable(false)
                .pAttachments(colorBlendAttachments);

        // Specify color attachments format (Dynamic rendering)
        VkPipelineRenderingCreateInfo renderingCreateInfo = VkPipelineRenderingCreateInfo.calloc(stack)
                .sType$Default()
                .colorAttachmentCount(1)
                .pColorAttachmentFormats(stack.ints(swapChain.getImageFormat()));

        VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType$Default()
                .pStages(shaderStageCreateInfos)
                .pVertexInputState(vertexInputStateCreateInfo)
                .pInputAssemblyState(inputAssemblyStateCreateInfo)
                .pViewportState(viewportStateCreateInfo)
                .pRasterizationState(rasterizationStateCreateInfo)
                .pMultisampleState(multiSampleStateCreateInfo)
                .pColorBlendState(colorBlendStateCreateInfo)
                .pDynamicState(dynamicStateCreateInfo)
                .pNext(renderingCreateInfo)
                .layout(this.pipelineLayout);

        LongBuffer pipelineBuff = stack.mallocLong(1);
        if (VK14.vkCreateGraphicsPipelines(VulkanManager.getLogicalDevice(), VK14.VK_NULL_HANDLE, pipelineCreateInfo, null, pipelineBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create graphics pipeline");
        }

        VK14.vkDestroyShaderModule(VulkanManager.getLogicalDevice(), vertexShader, null);
        VK14.vkDestroyShaderModule(VulkanManager.getLogicalDevice(), fragmentShader, null);

        return pipelineBuff.get(0);
    }

    private long createShaderModule(MemoryStack stack, ByteBuffer shaderCode) {
        VkShaderModuleCreateInfo shaderCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType$Default()
                .pCode(shaderCode);

        LongBuffer shaderBuff = stack.mallocLong(1);
        if (VK14.vkCreateShaderModule(VulkanManager.getLogicalDevice(), shaderCreateInfo, null, shaderBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create shader module");
        }

        return shaderBuff.get(0);
    }

    public long getPipeline() {
        return this.pipeline;
    }

    public long getLayout() {
        return this.pipelineLayout;
    }

    @Override
    public void close() {
        VK14.vkDestroyPipelineLayout(VulkanManager.getLogicalDevice(), this.pipelineLayout, null);
        VK14.vkDestroyPipeline(VulkanManager.getLogicalDevice(), this.pipeline, null);
    }
}
