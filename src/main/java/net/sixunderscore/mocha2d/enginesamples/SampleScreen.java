package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.util.Screen;
import net.sixunderscore.mocha2d.Window;
import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.text.TextRenderer;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import net.sixunderscore.mocha2d.input.KeyListener;
import net.sixunderscore.mocha2d.input.MouseListener;

public class SampleScreen implements Screen {
    private TextureRegion logoTexture;
    private float spinningDegrees;
    private TextRenderer textRenderer;

    @Override
    public void init(ResourceManager resourceManager) {
        this.logoTexture = resourceManager.getFullTexture("logo");
        this.textRenderer = resourceManager.getTextRenderer("roboto");
    }

    @Override
    public void update(KeyListener keyListener, MouseListener mouseListener) {
        if (this.spinningDegrees >= 360) {
            this.spinningDegrees = 0;
        } else {
            this.spinningDegrees += Window.getDeltaTime() * 75;
        }
    }

    @Override
    public void render(BatchRenderer batch) {
        batch.addSprite(this.logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, this.spinningDegrees, Window.getWidth() / 2f, Window.getHeight() / 2f);
        this.textRenderer.renderText(batch, "FPS: " + Window.getFpsCount(), 10, Window.getHeight() - 50, 1.1f);
    }
}
