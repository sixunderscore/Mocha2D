package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.Mocha2D;
import net.sixunderscore.mocha2d.util.Screen;
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
            this.t += Mocha2D.WINDOW.getDeltaTime() * this.rotationScalar;
        }

        float rad = MathUtils.lerp(0, MathUtils.PI_TIMES_2, this.t);
        float sin = MathUtils.lookupSin(rad);

        int transformIndex = batch.addScalingRotationTransform(sin, sin, rad, Mocha2D.WINDOW.getWidth() / 2f, Mocha2D.WINDOW.getHeight() / 2f);
        int tintIndex = batch.addTint((byte) 255, (byte) 0, (byte) 0, Math.abs(sin));

        batch.addSprite(this.logoTexture, Mocha2D.WINDOW.getWidth() / 2f - 150, Mocha2D.WINDOW.getHeight() / 2f - 150, 300, 300, transformIndex);
        batch.addText(this.bitmapFont, "FPS: " + Mocha2D.WINDOW.getFpsCount(), 10, Mocha2D.WINDOW.getHeight() - 50, 1.1f, BatchRenderer.NO_TRANSFORM, tintIndex);
    }
}
