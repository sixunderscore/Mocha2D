package net.sixunderscore.mocha2d.graphics.resources;

import net.sixunderscore.mocha2d.graphics.resources.text.FontBitmapResolution;
import net.sixunderscore.mocha2d.util.Color;

public record TtfFile(String path, FontBitmapResolution bitmapResolution, Color fontColor, String resourceKey) {}