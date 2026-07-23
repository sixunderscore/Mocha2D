package net.sixunderscore.mocha2d.enginesamples;

import net.sixunderscore.mocha2d.graphics.Window;
import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;
import net.sixunderscore.mocha2d.util.WindowSettings;

public class Main {
    static void main() {
        WindowSettings windowSettings = new WindowSettings();
        windowSettings.setTextureFiles(
                new TextureFile("sample_assets/textures/logo.jpg", false, "logo")
        );
        windowSettings.setTtfFiles(
                new TtfFile("sample_assets/fonts/Roboto-Regular.ttf", 64, "roboto")
        );
        windowSettings.setInitialScreen(new SampleScreen());
        Window.start(windowSettings);
    }
}