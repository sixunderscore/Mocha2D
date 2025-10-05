package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.util.Screen;
import net.sixunderscore.mocha2d.Window;
import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.text.TextRenderer;
import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.graphics.textures.TextureRegion;
import net.sixunderscore.mocha2d.input.KeyListener;
import net.sixunderscore.mocha2d.input.MouseListener;

public class SampleScreen implements Screen {
    private TextureRegion logoTexture;
    private float spinningDegrees;
    private TextRenderer textRenderer;

    @Override
    public void init(TextureManager textureManager) {
        logoTexture = textureManager.getFullTexture("logo");
        textRenderer = textureManager.getTextRenderer("roboto");
    }

    @Override
    public void update(KeyListener keyListener, MouseListener mouseListener) {
        if (spinningDegrees >= 360) {
            spinningDegrees = 0;
        } else {
            spinningDegrees += Window.getDeltaTime() * 75;
        }
    }

    @Override
    public void render(BatchRenderer batch) {
        batch.addSprite(logoTexture, Window.getWidth() / 2f - 150, Window.getHeight() / 2f - 150, 300, 300, spinningDegrees, Window.getWidth() / 2f, Window.getHeight() / 2f);
        textRenderer.renderText(batch, "FPS: " + Window.getFpsCount(), 10, Window.getHeight() - 50, 1.1f);
        textRenderer.renderText(batch, "Test Text\nNew Line!\n\n2 Lines down!!!!\tTabbed", 10, 250, 0.7f);
    }
}
