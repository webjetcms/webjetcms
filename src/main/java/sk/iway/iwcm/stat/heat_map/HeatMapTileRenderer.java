package sk.iway.iwcm.stat.heat_map;

import java.awt.image.BufferedImage;

/** Renders bounded regions of one globally aligned density grid. */
final class HeatMapTileRenderer {
    static final int CELL_SIZE = 4;
    static final int RADIUS = 32;
    private static final int HALO = RADIUS / CELL_SIZE;
    private static final int[] COLORS = { 0x0000cc, 0x00ffff, 0x008000, 0xffff00, 0xffa500, 0xb20000 };

    private final int originX;
    private final int originY;
    private final int width;
    private final int height;
    private final int columns;
    private final int rows;
    private final float[] density;

    HeatMapTileRenderer(int originX, int originY, int width, int height) {
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        columns = (width + CELL_SIZE - 1) / CELL_SIZE + 2 * HALO + 1;
        rows = (height + CELL_SIZE - 1) / CELL_SIZE + 2 * HALO + 1;
        density = new float[columns * rows];
    }

    void add(int documentX, int documentY, long count) {
        int x = Math.floorDiv(documentX, CELL_SIZE) - originX / CELL_SIZE + HALO;
        int y = Math.floorDiv(documentY, CELL_SIZE) - originY / CELL_SIZE + HALO;
        if (x >= 0 && x < columns && y >= 0 && y < rows) density[y * columns + x] += count;
    }

    BufferedImage render() {
        float[] kernel = new float[2 * HALO + 1];
        for (int i = -HALO; i <= HALO; i++) kernel[i + HALO] = (float) Math.exp(-i * i / 18.0);
        float[] horizontal = new float[density.length];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                float value = 0;
                for (int offset = -HALO; offset <= HALO; offset++) {
                    if (x + offset >= 0 && x + offset < columns) value += density[y * columns + x + offset] * kernel[offset + HALO];
                }
                horizontal[y * columns + x] = value;
            }
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                float value = 0;
                for (int offset = -HALO; offset <= HALO; offset++) {
                    if (y + offset >= 0 && y + offset < rows) value += horizontal[(y + offset) * columns + x] * kernel[offset + HALO];
                }
                density[y * columns + x] = value;
            }
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int row = y / CELL_SIZE + HALO;
            float fractionY = (y % CELL_SIZE) / (float) CELL_SIZE;
            for (int x = 0; x < width; x++) {
                int column = x / CELL_SIZE + HALO;
                float fractionX = (x % CELL_SIZE) / (float) CELL_SIZE;
                float upper = density[row * columns + column] * (1 - fractionX) + density[row * columns + column + 1] * fractionX;
                float lower = density[(row + 1) * columns + column] * (1 - fractionX) + density[(row + 1) * columns + column + 1] * fractionX;
                image.setRGB(x, y, color(upper * (1 - fractionY) + lower * fractionY));
            }
        }
        return image;
    }

    private static int color(float density) {
        if (density < 0.01f) return 0;
        float scale = density / (density + 2) * (COLORS.length - 1);
        int lower = Math.min(COLORS.length - 2, (int) scale);
        float fraction = scale - lower;
        int first = COLORS[lower];
        int second = COLORS[lower + 1];
        int red = Math.round((first >> 16 & 255) * (1 - fraction) + (second >> 16 & 255) * fraction);
        int green = Math.round((first >> 8 & 255) * (1 - fraction) + (second >> 8 & 255) * fraction);
        int blue = Math.round((first & 255) * (1 - fraction) + (second & 255) * fraction);
        int alpha = Math.round(210 * Math.min(1, density));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
