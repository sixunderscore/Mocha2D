package net.sixunderscore.mocha2d.graphics.util;

import net.sixunderscore.mocha2d.Mocha2D;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class OrthographicCamera implements AutoCloseable {
    private final Matrix4f projectionViewMatrix;
    private final FloatBuffer projectionViewBuffer;

    public OrthographicCamera(float initialWindowWidth, float initialWindowHeight) {
        this.projectionViewMatrix = new Matrix4f();
        this.projectionViewBuffer = MemoryUtil.memAllocFloat(16);

        this.adjustProjection(initialWindowWidth, initialWindowHeight);
    }

    private void adjustProjection(float windowWidth, float windowHeight) {
        this.projectionViewMatrix
                .identity()
                .ortho(
                        0, windowWidth,
                        windowHeight, 0,
                        0.1f, 100, true
                ).lookAt(
                        0, 0, 20f,
                        0, 0, 0f,
                        0, 1f, 0
                );

        this.projectionViewMatrix.get(this.projectionViewBuffer);
    }

    public void adjustProjection() {
        this.adjustProjection(Mocha2D.WINDOW.getWidth(), Mocha2D.WINDOW.getHeight());
    }

    public FloatBuffer getBuffer() {
        return this.projectionViewBuffer;
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.projectionViewBuffer);
    }
}
