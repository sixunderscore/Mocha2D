package net.sixunderscore.mocha2d.graphics.resources.text;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureAtlas;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureRegion;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class TextRenderer {
    private final GlyphData[] glyphDataArr;
    private final int charResolution;

    public TextRenderer(TextureAtlas fontAtlas, int charResolution, STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo) {
        this.glyphDataArr = new GlyphData[TextData.NUM_CHARS];
        this.charResolution = charResolution;
        this.loadTextureRegionsAndGlyphData(fontAtlas, charsData, fontInfo, charResolution);
    }

    private void loadTextureRegionsAndGlyphData(TextureAtlas fontAtlas, STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo, int charResolution) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float fontScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, charResolution);

            for (char c = TextData.FIRST_CHAR; c <= TextData.LAST_CHAR; ++c) {
                int index = c - TextData.FIRST_CHAR;
                STBTTBakedChar charData = charsData.get(index);
                TextureRegion charTextureRegion = fontAtlas.getRegion(charData.x0(), charData.x1(), charData.y0(), charData.y1());

                this.glyphDataArr[index] = new GlyphData(charTextureRegion, this.getCharDescent(stack, fontInfo, c, fontScale), charData.xadvance());
            }
        }
    }

    private float getCharDescent(MemoryStack stack, STBTTFontinfo fontInfo, char codepoint, float fontScale) {
        IntBuffer x0 = stack.mallocInt(1);
        IntBuffer y0 = stack.mallocInt(1);
        IntBuffer x1 = stack.mallocInt(1);
        IntBuffer y1 = stack.mallocInt(1);

        STBTruetype.stbtt_GetCodepointBox(fontInfo, codepoint, x0, y0, x1, y1);

        return (float) y0.get(0) * fontScale;
    }

    public void renderText(BatchRenderer batch, String text, float x, float y, float charScale) {
        float cursorX = x;
        float cursorY = y;
        int strLength = text.length();

        for (int i = 0; i < strLength; ++i) {
            char c = text.charAt(i);

            switch (c) {
                case ' ' -> cursorX += (this.charResolution * charScale) / 2.5f;
                case '\t' -> cursorX += (this.charResolution * charScale) * 1.5f;
                case '\n' -> {
                    cursorX = x;
                    cursorY -= this.charResolution * charScale;
                }
                default -> {
                    int arrayIndex = c - TextData.FIRST_CHAR;
                    GlyphData glyphData = arrayIndex >= 0 && arrayIndex < TextData.NUM_CHARS ? this.glyphDataArr[arrayIndex] : this.glyphDataArr['?' - TextData.FIRST_CHAR];
                    TextureRegion textureRegion = glyphData.textureRegion();

                    batch.addSprite(textureRegion, cursorX, cursorY + glyphData.descent() * charScale, textureRegion.width() * charScale, textureRegion.height() * charScale);

                    cursorX += glyphData.advance() * charScale;
                }
            }
        }
    }
}
