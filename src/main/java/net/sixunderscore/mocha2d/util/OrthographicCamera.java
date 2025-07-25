package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.Window;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class OrthographicCamera implements AutoCloseable {
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionViewMatrix;
    private final FloatBuffer projectionViewBuffer;
    private final Vector2f position;

    public OrthographicCamera() {
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.projectionViewMatrix = new Matrix4f();
        this.projectionViewBuffer = MemoryUtil.memAllocFloat(16);
        this.position = new Vector2f();

        this.adjustProjection();
    }

    public void adjustProjection() {
        this.projectionMatrix.identity().ortho(
                0, Window.getWidth(),
                Window.getHeight(), 0,
                0.1f, 100, true
        );

        this.viewMatrix.identity().lookAt(
                position.x, position.y, 20f,     // Position
                position.x, position.y, 0f,    // Target (where the camera is looking at)
                0, 1f, 0                          // Rotation
        );

        this.projectionMatrix.mul(this.viewMatrix, this.projectionViewMatrix);
        this.projectionViewMatrix.get(this.projectionViewBuffer);
    }

    public FloatBuffer getProjectionViewBuffer() {
        return projectionViewBuffer;
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.projectionViewBuffer);
    }
}
