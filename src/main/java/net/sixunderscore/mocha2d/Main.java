package net.sixunderscore.mocha2d;

import net.sixunderscore.mocha2d.enginesamples.SampleScreen;
import net.sixunderscore.mocha2d.graphics.Window;
import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFontResolution;
import net.sixunderscore.mocha2d.util.Color;
import net.sixunderscore.mocha2d.graphics.resources.TextureFile;
import net.sixunderscore.mocha2d.graphics.resources.TtfFile;
import net.sixunderscore.mocha2d.util.WindowSettings;

public class Main {
    public static void main(String[] args) {
        WindowSettings windowSettings = new WindowSettings();
        windowSettings.setTextureFiles(
                new TextureFile("sample_assets/textures/logo.jpg", false, "logo")
        );
        windowSettings.setTtfFiles(
                new TtfFile("sample_assets/fonts/Roboto-Regular.ttf", BitmapFontResolution.NORMAL, new Color((byte) 255, (byte) 255, (byte) 255), "roboto")
        );
        Window.start(windowSettings, new SampleScreen());
    }
}