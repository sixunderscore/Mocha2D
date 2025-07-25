package net.sixunderscore.mocha2d.graphics.render;

public class VertexData {
    public static final int POS_SIZE = 2;
    public static final int UV_SIZE = 2;
    public static final int TEX_INDEX_SIZE = 1;
    public static final int ROTATION_SIN_AND_COS_SIZE = 2;
    public static final int PIVOT_POS_SIZE = 2;
    public static final int TOTAL_SIZE = POS_SIZE + UV_SIZE + TEX_INDEX_SIZE + ROTATION_SIN_AND_COS_SIZE + PIVOT_POS_SIZE;

    public static final int POS_SIZE_BYTES = POS_SIZE * Float.BYTES;
    public static final int UV_SIZE_BYTES = UV_SIZE * Float.BYTES;
    public static final int TEX_INDEX_SIZE_BYTES = TEX_INDEX_SIZE * Float.BYTES;
    public static final int ROTATION_SIN_AND_COS_SIZE_BYTES = ROTATION_SIN_AND_COS_SIZE * Float.BYTES;
    public static final int PIVOT_POS_SIZE_BYTES = PIVOT_POS_SIZE * Float.BYTES;
    public static final int TOTAL_SIZE_BYTES = TOTAL_SIZE * Float.BYTES;
}
