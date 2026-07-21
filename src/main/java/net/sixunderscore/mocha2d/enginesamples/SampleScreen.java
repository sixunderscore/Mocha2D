package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.util.Screen;
import net.sixunderscore.mocha2d.graphics.Window;
import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFont;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.util.MathUtils;
import org.lwjgl.sdl.SDLKeycode;

public class SampleScreen implements Screen {
    private TextureRegion logoTexture;
    private BitmapFont bitmapFont;
    private float t;
    private float rotationScalar;

    @Override
    public void init(ResourceManager resourceManager) {
        this.logoTexture = resourceManager.getFullTexture("logo");
        this.bitmapFont = resourceManager.getBitmapFont("roboto");
        this.t = 0;
        this.rotationScalar = 0.6f;
    }

    @Override
    public void onKeyPressed(int keycode, int scancode, boolean pressed, short mods) {
        if (keycode == SDLKeycode.SDLK_R) {
            if (pressed) {
                this.rotationScalar = 1.5f;
            } else {
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

        float rad = MathUtils.lerp(0, MathUtils.PI_TIMES_2, this.t);
        float sin = MathUtils.lookupSin(rad);

        int transformIndex = batch.addScalingRotationTransform(sin, sin, rad, Window.getWidth() / 2f, Window.getHeight() / 2f);
        int tintIndex = batch.addTint((byte) 255, (byte) 0, (byte) 0, Math.abs(sin));

        batch.addSprite(this.logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, transformIndex);
        batch.addText(this.bitmapFont, "FPS: " + Window.getFpsCount(), 10, Window.getHeight() - 50, 1.1f, BatchRenderer.NO_TRANSFORM, tintIndex);
    }
}
