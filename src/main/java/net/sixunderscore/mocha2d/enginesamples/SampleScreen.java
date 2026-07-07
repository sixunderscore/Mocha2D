package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.graphics.Screen;
import net.sixunderscore.mocha2d.graphics.Window;
import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFont;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.util.MathUtils;
import org.joml.Matrix2f;
import org.lwjgl.glfw.GLFW;

public class SampleScreen implements Screen {
    private TextureRegion logoTexture;
    private Matrix2f transform;
    private BitmapFont bitmapFont;
    private float t;
    private float rotationScalar;

    @Override
    public void init(ResourceManager resourceManager) {
        this.logoTexture = resourceManager.getFullTexture("logo");
        this.transform = new Matrix2f();
        this.bitmapFont = resourceManager.getBitmapFont("roboto");

        this.t = 0;
        this.rotationScalar = 0.6f;
    }

    @Override
    public void onKeyPressed(int keycode, int scancode, int action, int mods) {
        if (keycode == GLFW.GLFW_KEY_R) {
            if (action == GLFW.GLFW_PRESS) {
                this.rotationScalar = 1.5f;
            } else if (action == GLFW.GLFW_RELEASE) {
                this.rotationScalar = 0.6f;
            }
        }
    }

    @Override
    public void render(BatchRenderer batch) {
        if (this.t >= 1f) {
            this.t = 0;
        } else {
            this.t += Window.getDeltaTime() * this.rotationScalar;
        }

        float scale = MathUtils.lookupSin(MathUtils.lerp(0, MathUtils.PI_TIMES_2, this.t));

        this.transform.rotation(MathUtils.lerp(0, MathUtils.PI_TIMES_2, this.t))
                        .scale(scale);

        batch.addSprite(this.logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, this.transform,
                Window.getWidth() / 2f, Window.getHeight() / 2f);

        //batch.addSprite(this.logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, MathUtils.lerp(0, MathUtils.PI_TIMES_2, this.t), Window.getWidth() / 2f, Window.getHeight() / 2f);
        batch.addText(this.bitmapFont, "FPS: " + Window.getFpsCount(), 10, Window.getHeight() - 50, 1.1f);
    }
}
