package sk.iway.iwcm.stat.heat_map;

import java.awt.image.BufferedImage;

/** Renders bounded regions of one globally aligned density grid. */
final class HeatMapTileRenderer {
    static final int RADIUS = 32;
    private static final double SIGMA = 12;
    private static final int[] COLORS = { 0x0000cc, 0x00ffff, 0x008000, 0xffff00, 0xffa500, 0xb20000 };

    private final int originX;
    private final int originY;
    private final int width;
    private final int height;
    private final int cellSize;
    private final int halo;
    private final int columns;
    private final int rows;
    private final float[] density;
    private boolean smoothed;

    HeatMapTileRenderer(int originX, int originY, int width, int height, int cellSize) {
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        halo = (RADIUS + cellSize - 1) / cellSize;
        columns = (Math.floorMod(originX, cellSize) + width + cellSize - 1) / cellSize + 2 * halo + 1;
        rows = (Math.floorMod(originY, cellSize) + height + cellSize - 1) / cellSize + 2 * halo + 1;
        density = new float[columns * rows];
    }

    void add(int documentX, int documentY, long count) {
        int x = Math.floorDiv(documentX, cellSize) - Math.floorDiv(originX, cellSize) + halo;
        int y = Math.floorDiv(documentY, cellSize) - Math.floorDiv(originY, cellSize) + halo;
        if (x >= 0 && x < columns && y >= 0 && y < rows) density[y * columns + x] += count;
    }

    private void smooth() {
        if (smoothed) return;
        smoothed = true;
        float[] kernel = new float[2 * halo + 1];
        for (int i = -halo; i <= halo; i++) {
            double distance = (double) i * cellSize;
            kernel[i + halo] = (float) Math.exp(-distance * distance / (2 * SIGMA * SIGMA));
        }
        float[] horizontal = new float[density.length];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                float value = 0;
                for (int offset = -halo; offset <= halo; offset++) {
                    if (x + offset >= 0 && x + offset < columns) value += density[y * columns + x + offset] * kernel[offset + halo];
                }
                horizontal[y * columns + x] = value;
            }
        }
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                float value = 0;
                for (int offset = -halo; offset <= halo; offset++) {
                    if (y + offset >= 0 && y + offset < rows) value += horizontal[(y + offset) * columns + x] * kernel[offset + halo];
                }
                density[y * columns + x] = value;
            }
        }
    }

    /** Returns the peak on this tile's grid, including interpolation samples at its edges. */
    double maximumDensity() {
        smooth();
        double maximum = 0;
        for (int y = halo; y < rows - halo; y++) {
            for (int x = halo; x < columns - halo; x++) maximum = Math.max(maximum, density[y * columns + x]);
        }
        return maximum;
    }

    /** Uses one peak for the selected page version so clipping and scrolling cannot rescale the colors. */
    BufferedImage render(double maximumDensity) {
        if (!Double.isFinite(maximumDensity) || maximumDensity <= 0) throw new IllegalArgumentException("Invalid heatmap density scale");
        smooth();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int offsetX = Math.floorMod(originX, cellSize);
        int offsetY = Math.floorMod(originY, cellSize);
        for (int y = 0; y < height; y++) {
            int row = (offsetY + y) / cellSize + halo;
            float fractionY = ((offsetY + y) % cellSize) / (float) cellSize;
            for (int x = 0; x < width; x++) {
                int column = (offsetX + x) / cellSize + halo;
                float fractionX = ((offsetX + x) % cellSize) / (float) cellSize;
                float upper = density[row * columns + column] * (1 - fractionX) + density[row * columns + column + 1] * fractionX;
                float lower = density[(row + 1) * columns + column] * (1 - fractionX) + density[(row + 1) * columns + column + 1] * fractionX;
                image.setRGB(x, y, color((upper * (1 - fractionY) + lower * fractionY) / maximumDensity));
            }
        }
        return image;
    }

    private static int color(double relativeDensity) {
        if (relativeDensity <= 0) return 0;
        double normalized = Math.min(1, relativeDensity);
        float scale = (float) (normalized * (COLORS.length - 1));
        int lower = Math.min(COLORS.length - 2, (int) scale);
        float fraction = scale - lower;
        int first = COLORS[lower];
        int second = COLORS[lower + 1];
        int red = Math.round((first >> 16 & 255) * (1 - fraction) + (second >> 16 & 255) * fraction);
        int green = Math.round((first >> 8 & 255) * (1 - fraction) + (second >> 8 & 255) * fraction);
        int blue = Math.round((first & 255) * (1 - fraction) + (second & 255) * fraction);
        int alpha = (int) Math.round(210 * Math.min(1, 2 * Math.sqrt(normalized)));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
