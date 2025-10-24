package net.sixunderscore.mocha2d.graphics.resources;

import net.sixunderscore.mocha2d.graphics.resources.text.BitmapFontResolution;
import net.sixunderscore.mocha2d.util.Color;

public record TtfFile(String path, BitmapFontResolution bitmapResolution, Color fontColor, String resourceKey) {}