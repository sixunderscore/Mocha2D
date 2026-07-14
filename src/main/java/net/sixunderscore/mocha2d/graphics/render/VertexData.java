package net.sixunderscore.mocha2d.graphics.render;

public class VertexData {
    public static final int POS_SIZE = 2;
    public static final int UV_SIZE = 2;
    public static final int TEX_INDEX_SIZE = 1;
    public static final int TRANSFORM_INDEX_SIZE = 1;
    public static final int TINT_INDEX_SIZE = 1;
    public static final int TOTAL_SIZE = POS_SIZE + UV_SIZE + TEX_INDEX_SIZE + TRANSFORM_INDEX_SIZE + TINT_INDEX_SIZE;

    public static final int POS_SIZE_BYTES = POS_SIZE * Float.BYTES;
    public static final int UV_SIZE_BYTES = UV_SIZE * Float.BYTES;
    public static final int TEX_INDEX_SIZE_BYTES = TEX_INDEX_SIZE * Float.BYTES;
    public static final int TRANSFORM_INDEX_SIZE_BYTES = TRANSFORM_INDEX_SIZE * Float.BYTES;
    public static final int TINT_INDEX_SIZE_BYTES = TINT_INDEX_SIZE * Float.BYTES;
    public static final int TOTAL_SIZE_BYTES = TOTAL_SIZE * Float.BYTES;
}
