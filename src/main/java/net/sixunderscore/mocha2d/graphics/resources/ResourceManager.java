package net.sixunderscore.mocha2d.graphics.resources;

import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFontResolution;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureAtlas;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureData;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.vulkan.util.CommandPool;
import net.sixunderscore.mocha2d.vulkan.util.SyncUtils;
import net.sixunderscore.mocha2d.graphics.resources.text.TextData;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFont;
import net.sixunderscore.mocha2d.vulkan.util.GpuBuffer;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.util.ResourceUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceManager implements AutoCloseable {
    private final Map<String, TextureAtlas> atlasMap;
    private final Map<String, BitmapFont> bitmapFontMap;
    private final long descriptorPool;
    private final long descriptorSet;
    private final long descriptorSetLayout;
    private final long linearSampler;
    private final long nearestSampler;
    private final int totalTextures;

    public ResourceManager(TextureFile[] textureFiles, TtfFile[] ttfFiles) {
        if (textureFiles.length == 0 && ttfFiles.length == 0) {
            throw new IllegalStateException("At least one Texture or TTF file must be loaded");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.atlasMap = new HashMap<>(textureFiles.length);
            this.bitmapFontMap = new HashMap<>(ttfFiles.length);
            this.totalTextures = textureFiles.length + ttfFiles.length;

            this.descriptorPool = createDescriptorPool(stack);
            this.descriptorSetLayout = createDescriptorSetLayout(stack);
            this.descriptorSet = allocateDescriptorSet(stack);

            this.linearSampler = createSampler(stack, VK14.VK_FILTER_LINEAR, VK14.VK_FILTER_LINEAR);
            this.nearestSampler = createSampler(stack, VK14.VK_FILTER_NEAREST, VK14.VK_FILTER_NEAREST);

            this.uploadTextures(stack, textureFiles, ttfFiles);
        }
    }

    private long createDescriptorPool(MemoryStack stack) {
        VkDescriptorPoolSize.Buffer descriptorPoolSizes = VkDescriptorPoolSize.calloc(1, stack);
        descriptorPoolSizes.get(0).set(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, this.totalTextures);

        VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType$Default()
                .maxSets(1)
                .pPoolSizes(descriptorPoolSizes);

        LongBuffer descriptorPoolBuff = stack.mallocLong(1);
        if (VK14.vkCreateDescriptorPool(VulkanManager.getLogicalDevice(), descriptorPoolCreateInfo, null, descriptorPoolBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create descriptor pool");
        }

        return descriptorPoolBuff.get(0);
    }

    private long createDescriptorSetLayout(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
        descriptorSetLayoutBindings.get(0).set(0, VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, this.totalTextures, VK14.VK_SHADER_STAGE_FRAGMENT_BIT, null);

        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType$Default()
                .pBindings(descriptorSetLayoutBindings);

        LongBuffer descriptorSetLayoutBuff = stack.mallocLong(1);
        if (VK14.vkCreateDescriptorSetLayout(VulkanManager.getLogicalDevice(), descriptorSetLayoutCreateInfo, null, descriptorSetLayoutBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create descriptor set layout");
        }

        return descriptorSetLayoutBuff.get(0);
    }

    private long allocateDescriptorSet(MemoryStack stack) {
        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType$Default()
                .descriptorPool(this.descriptorPool)
                .pSetLayouts(stack.longs(this.descriptorSetLayout));

        LongBuffer descriptorSetBuff = stack.mallocLong(1);
        if (VK14.vkAllocateDescriptorSets(VulkanManager.getLogicalDevice(), descriptorSetAllocateInfo, descriptorSetBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to allocate descriptor set");
        }

        return descriptorSetBuff.get(0);
    }

    private long createSampler(MemoryStack stack, int minFilter, int magFilter) {
        VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack)
                .sType$Default()
                .minFilter(minFilter)
                .magFilter(magFilter)
                .addressModeU(VK14.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .addressModeV(VK14.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .addressModeW(VK14.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .anisotropyEnable(false)
                .unnormalizedCoordinates(false)
                .compareEnable(false);

        LongBuffer samplerBuff = stack.mallocLong(1);
        if (VK14.vkCreateSampler(VulkanManager.getLogicalDevice(), samplerCreateInfo, null, samplerBuff) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create sampler");
        }

        return samplerBuff.get(0);
    }

    private void uploadTextures(MemoryStack stack, TextureFile[] textureFiles, TtfFile[] ttfFiles) {
        long imagesUploadedFence = SyncUtils.createFence(stack, false);
        List<GpuBuffer> stagingBuffersToFree = new ArrayList<>(this.totalTextures);
        int textureIndex = 0;

        try (CommandPool commandPool = new CommandPool(stack, VulkanManager.getGraphicsQueueIndex(), VK14.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)) {
            VkCommandBuffer commandBuffer = commandPool.allocateCommandBuffer(stack);

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VK14.vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo);

            for (TextureFile file : textureFiles) {
                TextureAtlas atlas = this.createTextureAtlas(stack, commandBuffer, file, textureIndex++, stagingBuffersToFree);
                this.atlasMap.put(file.resourceKey(), atlas);
            }

            for (TtfFile file : ttfFiles) {
                Map.Entry<BitmapFont, TextureAtlas> bitmapFontAndAtlas = this.createBitmapFont(stack, commandBuffer, file, textureIndex++, stagingBuffersToFree);
                this.bitmapFontMap.put(file.resourceKey(), bitmapFontAndAtlas.getKey());
                this.atlasMap.put(file.resourceKey() + "_atlas", bitmapFontAndAtlas.getValue());
            }

            VK14.vkEndCommandBuffer(commandBuffer);

            VkCommandBufferSubmitInfo.Buffer commandBufferSubmitInfo = VkCommandBufferSubmitInfo.calloc(1, stack);
            commandBufferSubmitInfo.get(0)
                    .sType$Default()
                    .commandBuffer(commandBuffer);
            VkSubmitInfo2.Buffer submitInfo = VkSubmitInfo2.calloc(1, stack);
            submitInfo.get(0)
                    .sType$Default()
                    .pCommandBufferInfos(commandBufferSubmitInfo);

            VK14.vkQueueSubmit2(VulkanManager.getGraphicsQueue(), submitInfo, imagesUploadedFence);
            VK14.vkWaitForFences(VulkanManager.getLogicalDevice(), imagesUploadedFence, true, Long.MAX_VALUE);
        }

        // Clean up fence
        VK14.vkDestroyFence(VulkanManager.getLogicalDevice(), imagesUploadedFence, null);

        // Clean up all staging buffers after textures have been uploaded
        stagingBuffersToFree.forEach(GpuBuffer::close);

        // Adding regular textures and bitmap font atlas textures to descriptor set
        VkDescriptorImageInfo.Buffer descriptorImagesInfo = VkDescriptorImageInfo.calloc(this.totalTextures, stack);

        for (TextureAtlas atlas : this.atlasMap.values()) {
            descriptorImagesInfo.get(atlas.getImageIndex())
                    .set(atlas.isPixelated() ? this.nearestSampler : this.linearSampler, atlas.getImageView(), VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.calloc(1);
        writeDescriptorSet.get(0)
                .sType$Default()
                .dstSet(this.descriptorSet)
                .dstBinding(0)
                .dstArrayElement(0)
                .descriptorCount(this.totalTextures)
                .descriptorType(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .pImageInfo(descriptorImagesInfo);

        VK14.vkUpdateDescriptorSets(VulkanManager.getLogicalDevice(), writeDescriptorSet, null);
    }

    private TextureAtlas createTextureAtlas(MemoryStack stack, VkCommandBuffer commandBuffer, TextureFile file, int textureIndex, List<GpuBuffer> stagingBuffersToFree) {
        try (TextureData textureData = ResourceUtils.loadAndDecodeImage(file.path())) {
            TextureAtlas atlas = new TextureAtlas(textureData.width(), textureData.height(), file.isPixelated(), textureIndex);
            GpuBuffer stagingImageBuffer = ResourceUtils.createStagingImageBuffer(stack, textureData);

            // Upload data from staging buffer to GPU memory
            atlas.recordUploadCommands(stack, commandBuffer, stagingImageBuffer);

            stagingBuffersToFree.add(stagingImageBuffer);

            return atlas;
        }
    }

    private Map.Entry<BitmapFont, TextureAtlas> createBitmapFont(MemoryStack stack, VkCommandBuffer commandBuffer, TtfFile file, int textureIndex, List<GpuBuffer> stagingBuffersToFree) {
        STBTTFontinfo fontInfo = STBTTFontinfo.malloc(stack);
        ByteBuffer ttfFileData = ResourceUtils.loadRawFile(file.path());

        if (!STBTruetype.stbtt_InitFont(fontInfo, ttfFileData)) {
            throw new IllegalStateException("Unable to initialize font: " + file.path());
        }

        // Font atlas dimensions
        BitmapFontResolution bitmapResolution = file.bitmapResolution();
        int charResolution = bitmapResolution.getCharResolution();
        int grayscaleSideSize = bitmapResolution.getAtlasSideSize();
        int grayscaleTotalSize = grayscaleSideSize * grayscaleSideSize;

        // Use STB to generate a grayscale font atlas
        ByteBuffer grayscaleImageBuffer = MemoryUtil.memAlloc(grayscaleTotalSize);
        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(TextData.NUM_CHARS, stack);
        STBTruetype.stbtt_BakeFontBitmap(ttfFileData, charResolution, grayscaleImageBuffer, grayscaleSideSize, grayscaleSideSize, TextData.FIRST_CHAR, charData);

        // Convert to RGBA
        try (TextureData textureData = ResourceUtils.convertGrayscaleToRGBA(grayscaleImageBuffer, grayscaleSideSize, grayscaleTotalSize, file.fontColor())) {
            TextureAtlas fontAtlas = new TextureAtlas(textureData.width(), textureData.height(), false, textureIndex);
            GpuBuffer stagingImageBuffer = ResourceUtils.createStagingImageBuffer(stack, textureData);

            // Upload data from staging buffer to GPU memory
            fontAtlas.recordUploadCommands(stack, commandBuffer, stagingImageBuffer);

            BitmapFont bitmapFont = new BitmapFont(fontAtlas, charResolution, charData, fontInfo);

            MemoryUtil.memFree(grayscaleImageBuffer);
            MemoryUtil.memFree(ttfFileData);
            stagingBuffersToFree.add(stagingImageBuffer);

            return Map.entry(bitmapFont, fontAtlas);
        }
    }

    public TextureRegion getFullTexture(String resourceKey) {
        return this.atlasMap.get(resourceKey).getFull();
    }

    public TextureRegion getTextureRegion(String resourceKey, int startX, int endX, int startY, int endY) {
        return this.atlasMap.get(resourceKey).getRegion(startX, endX, startY, endY);
    }

    public BitmapFont getBitmapFont(String resourceKey) {
        return this.bitmapFontMap.get(resourceKey);
    }

    public long getDescriptorSetLayout() {
        return this.descriptorSetLayout;
    }

    public long getDescriptorSet() {
        return this.descriptorSet;
    }

    public int getDescriptorCount() {
        return this.totalTextures;
    }

    @Override
    public void close() {
        this.atlasMap.values().forEach(TextureAtlas::close);
        VK14.vkDestroyDescriptorPool(VulkanManager.getLogicalDevice(), this.descriptorPool, null);
        VK14.vkDestroyDescriptorSetLayout(VulkanManager.getLogicalDevice(), this.descriptorSetLayout, null);
        VK14.vkDestroySampler(VulkanManager.getLogicalDevice(), this.linearSampler, null);
        VK14.vkDestroySampler(VulkanManager.getLogicalDevice(), this.nearestSampler, null);
    }
}
