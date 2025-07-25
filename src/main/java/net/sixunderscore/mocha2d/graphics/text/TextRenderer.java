package net.sixunderscore.mocha2d.graphics.text;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.textures.TextureAtlas;
import net.sixunderscore.mocha2d.graphics.textures.TextureRegion;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import java.util.HashMap;
import java.util.Map;

public class TextRenderer implements AutoCloseable {
    private final TextureAtlas fontAtlas;
    private final Map<Character, TextureRegion> textureRegionMap = new HashMap<>(TextData.NUM_CHARS);
    private final Map<Character, GlyphData> glyphDataMap = new HashMap<>(TextData.NUM_CHARS);
    private final int spaceAdvance;

    public TextRenderer(TextureAtlas textureAtlas, int charResolution, STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo) {
        this.fontAtlas = textureAtlas;
        this.spaceAdvance = charResolution;
        this.loadTextureRegionsAndGlyphData(charsData, fontInfo, charResolution);
    }

    private void loadTextureRegionsAndGlyphData(STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo, int charResolution) {
        float fontScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, charResolution);
        int i = 0;

        for (char c = TextData.FIRST_CHAR; c <= TextData.LAST_CHAR; ++c) {
            STBTTBakedChar charData = charsData.get(i++);

            this.textureRegionMap.put(c, this.fontAtlas.getRegion(charData.x0(), charData.x1(), charData.y0(), charData.y1()));
            this.glyphDataMap.put(c, new GlyphData(this.getCharDescent(fontInfo, c, fontScale), charData.xadvance()));
        }
    }

    private float getCharDescent(STBTTFontinfo fontInfo, char codepoint, float fontScale) {
        int[] x0 = new int[1];
        int[] y0 = new int[1];
        int[] x1 = new int[1];
        int[] y1 = new int[1];

        if (STBTruetype.stbtt_GetCodepointBox(fontInfo, codepoint, x0, y0, x1, y1)) {
            return y0[0] * fontScale;
        }

        return 0;
    }

    public void renderText(BatchRenderer batch, String text, float x, float y, float charScale) {
        float xPos = x;
        int strLength = text.length();

        for (int i = 0; i < strLength; ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                xPos += (this.spaceAdvance * charScale) / 2.5f;
                continue;
            }

            TextureRegion textureRegion = this.textureRegionMap.get(c);
            GlyphData glyphData = this.glyphDataMap.get(c);

            if (textureRegion != null) {
                batch.addSprite(textureRegion, xPos, y + glyphData.descent() * charScale, textureRegion.width() * charScale, textureRegion.height() * charScale);

                xPos += glyphData.advance() * charScale;
            }
        }
    }

    public TextureAtlas getFontAtlas() {
        return this.fontAtlas;
    }

    @Override
    public void close() {
        this.fontAtlas.close();
    }
}
