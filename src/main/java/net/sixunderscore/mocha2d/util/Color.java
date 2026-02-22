package net.sixunderscore.mocha2d.util;

public record Color(byte r, byte g, byte b) {
    public float normalizedLinearR() {
        return this.srgbToLinear(Byte.toUnsignedInt(this.r) / 255f);
    }

    public float normalizedLinearG() {
        return this.srgbToLinear(Byte.toUnsignedInt(this.g) / 255f);
    }

    public float normalizedLinearB() {
        return this.srgbToLinear(Byte.toUnsignedInt(this.b) / 255f);
    }

    private float srgbToLinear(float c) {
        return (c <= 0.04045f) ? c / 12.92f : (float)Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }
}
