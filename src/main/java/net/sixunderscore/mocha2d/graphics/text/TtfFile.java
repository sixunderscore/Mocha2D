package net.sixunderscore.mocha2d.graphics.text;

import net.sixunderscore.mocha2d.util.Color;

public record TtfFile(String path, FontBitmapResolution bitmapResolution, Color fontColor, String resourceKey) {}