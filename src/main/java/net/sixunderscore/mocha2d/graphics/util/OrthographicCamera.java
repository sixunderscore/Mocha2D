package net.sixunderscore.mocha2d.graphics.util;

import net.sixunderscore.mocha2d.Mocha2D;
import net.sixunderscore.mocha2d.util.MiscUtils;
import org.lwjgl.system.MemoryUtil;

public class OrthographicCamera implements AutoCloseable {
    private final long matrixPtr;

    public OrthographicCamera(float initialWindowWidth, float initialWindowHeight) {
        this.matrixPtr = MemoryUtil.nmemAllocChecked(MiscUtils.MATRIX_4F_BYTES);
        this.adjustProjection(initialWindowWidth, initialWindowHeight);
    }

    private void adjustProjection(float windowWidth, float windowHeight) {
        MemoryUtil.memSet(this.matrixPtr, 0, MiscUtils.MATRIX_4F_BYTES);

        float m22 =  1.0f / (0.1f - 100f);
        float m32 =  0.1f / (0.1f - 100f) + m22 * (-20f);

        MemoryUtil.memPutFloat(this.matrixPtr, 2.0f / windowWidth); // m00
        MemoryUtil.memPutFloat(this.matrixPtr+  5 * Float.BYTES, -2.0f / windowHeight); // m11
        MemoryUtil.memPutFloat(this.matrixPtr+ 10 * Float.BYTES, m22); // m22
        MemoryUtil.memPutFloat(this.matrixPtr+ 12 * Float.BYTES, -1.0f); // m30
        MemoryUtil.memPutFloat(this.matrixPtr+ 13 * Float.BYTES, 1.0f); // m31
        MemoryUtil.memPutFloat(this.matrixPtr+ 14 * Float.BYTES, m32); // m32
        MemoryUtil.memPutFloat(this.matrixPtr+ 15 * Float.BYTES, 1.0f); // m33
    }

    public void adjustProjection() {
        this.adjustProjection(Mocha2D.WINDOW.getWidth(), Mocha2D.WINDOW.getHeight());
    }

    public long getMatrixPointer() {
        return this.matrixPtr;
    }

    @Override
    public void close() {
        MemoryUtil.nmemFree(this.matrixPtr);
    }
}
