package net.sixunderscore.mocha2d.util;

public record Color(byte r, byte g, byte b) {
    public float normalizedR() {
        return Byte.toUnsignedInt(this.r) / 255f;
    }

    public float normalizedG() {
        return Byte.toUnsignedInt(this.g) / 255f;
    }

    public float normalizedB() {
        return Byte.toUnsignedInt(this.b) / 255f;
    }
}
