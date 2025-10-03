package net.sixunderscore.mocha2d.util;

import net.sixunderscore.mocha2d.graphics.text.FontBitmapResolution;

public record TtfFile(String path, FontBitmapResolution bitmapResolution, Color fontColor, String resourceKey) {}