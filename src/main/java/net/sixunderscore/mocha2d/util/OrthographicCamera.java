package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.Window;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class OrthographicCamera implements AutoCloseable {
    private final Matrix4f projectionViewMatrix;
    private final FloatBuffer projectionViewBuffer;
    private final Vector2f position;

    public OrthographicCamera() {
        this.projectionViewMatrix = new Matrix4f();
        this.projectionViewBuffer = MemoryUtil.memAllocFloat(16);
        this.position = new Vector2f();

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
                        position.x, position.y, 20f,     // Position
                        position.x, position.y, 0f,    // Target (where the camera is looking at)
                        0, 1f, 0                          // Rotation
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
