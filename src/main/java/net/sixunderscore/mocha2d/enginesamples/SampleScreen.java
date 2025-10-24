package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.util.Screen;
import net.sixunderscore.mocha2d.Window;
import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFont;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.input.KeyListener;
import net.sixunderscore.mocha2d.input.MouseListener;

public class SampleScreen implements Screen {
    private TextureRegion logoTexture;
    private float t;
    private float spinningDegrees;
    private BitmapFont bitmapFont;

    @Override
    public void init(ResourceManager resourceManager) {
        this.logoTexture = resourceManager.getFullTexture("logo");
        this.bitmapFont = resourceManager.getBitmapFont("roboto");
    }

    @Override
    public void update(KeyListener keyListener, MouseListener mouseListener) {
        this.t += Window.getDeltaTime() * 0.6f;

        if (this.t >= 1f) {
            this.t = 0;
        }

        this.spinningDegrees = org.joml.Math.lerp(0, 360f, this.t);
    }

    @Override
    public void render(BatchRenderer batch) {
        batch.addSprite(this.logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, this.spinningDegrees, Window.getWidth() / 2f, Window.getHeight() / 2f);
        batch.addText(this.bitmapFont, "FPS: " + Window.getFpsCount(), 10, Window.getHeight() - 50, 1.1f);
    }
}
