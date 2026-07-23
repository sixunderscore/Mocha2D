package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.Mocha2D;
import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;
import net.sixunderscore.mocha2d.util.RenderSettings;
import net.sixunderscore.mocha2d.util.WindowSettings;

public class Main {
    static void main() {
        WindowSettings windowSettings = new WindowSettings()
                .setInitialScreen(new SampleScreen());
        Mocha2D.init(windowSettings);

        RenderSettings renderSettings = new RenderSettings()
                .setTextureFiles(
                        new TextureFile("sample_assets/textures/logo.jpg", false, "logo")
                )
                .setTtfFiles(
                        new TtfFile("sample_assets/fonts/Roboto-Regular.ttf", 64, "roboto")
                );
        Mocha2D.start(renderSettings);
    }
}