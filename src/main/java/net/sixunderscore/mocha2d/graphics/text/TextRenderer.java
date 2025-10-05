package net.sixunderscore.mocha2d.graphics.text;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.textures.TextureAtlas;
import net.sixunderscore.mocha2d.graphics.textures.TextureRegion;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class TextRenderer implements AutoCloseable {
    private final TextureAtlas fontAtlas;
    private final GlyphData[] glyphDataArr;
    private final int spaceAdvance;

    public TextRenderer(TextureAtlas textureAtlas, int charResolution, STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo) {
        this.fontAtlas = textureAtlas;
        this.glyphDataArr = new GlyphData[TextData.NUM_CHARS];
        this.spaceAdvance = charResolution;
        this.loadTextureRegionsAndGlyphData(charsData, fontInfo, charResolution);
    }

    private void loadTextureRegionsAndGlyphData(STBTTBakedChar.Buffer charsData, STBTTFontinfo fontInfo, int charResolution) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float fontScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, charResolution);

            for (char c = TextData.FIRST_CHAR; c <= TextData.LAST_CHAR; ++c) {
                int index = c - TextData.FIRST_CHAR;
                STBTTBakedChar charData = charsData.get(index);
                TextureRegion charTextureRegion = this.fontAtlas.getRegion(charData.x0(), charData.x1(), charData.y0(), charData.y1());

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
        float xPos = x;
        int strLength = text.length();

        for (int i = 0; i < strLength; ++i) {
            char c = text.charAt(i);

            if (c == ' ') {
                xPos += (this.spaceAdvance * charScale) / 2.5f;
                continue;
            }

            int arrayIndex = c - TextData.FIRST_CHAR;
            GlyphData glyphData = arrayIndex >= 0 && arrayIndex < TextData.NUM_CHARS ? this.glyphDataArr[arrayIndex] : this.glyphDataArr['?' - TextData.FIRST_CHAR];
            TextureRegion textureRegion = glyphData.textureRegion();

            batch.addSprite(textureRegion, xPos, y + glyphData.descent() * charScale, textureRegion.width() * charScale, textureRegion.height() * charScale);

            xPos += glyphData.advance() * charScale;
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
