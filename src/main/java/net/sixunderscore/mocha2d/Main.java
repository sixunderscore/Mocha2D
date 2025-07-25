package net.sixunderscore.mocha2d;

import net.sixunderscore.mocha2d.enginesamples.SampleScreen;
import net.sixunderscore.mocha2d.graphics.text.FontBitmapResolution;
import net.sixunderscore.mocha2d.graphics.text.FontColors;
import net.sixunderscore.mocha2d.graphics.textures.TextureFile;
import net.sixunderscore.mocha2d.graphics.text.TtfFile;
import net.sixunderscore.mocha2d.util.WindowSettings;

public class Main {
    public static void main(String[] args) {
        WindowSettings windowSettings = new WindowSettings();
        windowSettings.setTextureFiles(
                new TextureFile("sample_assets/textures/logo.jpg", false, "logo")
        );
        windowSettings.setTtfFiles(
                new TtfFile("sample_assets/fonts/Roboto-Regular.ttf", FontBitmapResolution.NORMAL, FontColors.WHITE, "roboto")
        );
        Window.start(windowSettings, new SampleScreen());
    }
}