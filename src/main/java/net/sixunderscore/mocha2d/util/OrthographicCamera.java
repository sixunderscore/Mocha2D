package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.Window;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class OrthographicCamera implements AutoCloseable {
    private final Matrix4f projectionViewMatrix;
    private final FloatBuffer projectionViewBuffer;

    public OrthographicCamera() {
        this.projectionViewMatrix = new Matrix4f();
        this.projectionViewBuffer = MemoryUtil.memAllocFloat(16);

        this.adjustProjection();
    }

    public void adjustProjection() {
        this.projectionViewMatrix
                .identity()
                .ortho(
                        0, Window.getWidth(),
                        Window.getHeight(), 0,
                        0.1f, 100, true
                ).lookAt(
                        0, 0, 20f,
                        0, 0, 0f,
                        0, 1f, 0
                );

        this.projectionViewMatrix.get(this.projectionViewBuffer);
    }

    public FloatBuffer getBuffer() {
        return this.projectionViewBuffer;
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.projectionViewBuffer);
    }
}
