package net.sixunderscore.mocha2d.graphics.resources.textures;

public record TextureRegion(
        int imageIndex,
        int width,
        int height,
        float topLeftU, float topLeftV,
        float topRightU, float topRightV,
        float bottomLeftU, float bottomLeftV,
        float bottomRightU, float bottomRightV
) {}
