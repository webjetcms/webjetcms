package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies that tile clipping never changes density or introduces seams. */
class HeatMapTileRendererTest {
    private record Point(int x, int y, long count) {
    }

    private static BufferedImage render(List<Point> points, int originX, int originY, int width, int height) {
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(originX, originY, width, height);
        points.forEach(point -> renderer.add(point.x(), point.y(), point.count()));
        return renderer.render();
    }

    @Test
    void adjacentTilesMatchOneContinuousImageIncludingBothSidesOfTheSeam() {
        List<Point> points = List.of(
                new Point(1008, 120, 4), new Point(1026, 116, 8),
                new Point(1048, 132, 1), new Point(40, 40, 3));
        BufferedImage complete = render(points, 0, 0, 2048, 256);
        BufferedImage left = render(points, 0, 0, 1024, 256);
        BufferedImage right = render(points, 1024, 0, 1024, 256);
        assertArrayEquals(complete.getRGB(0, 0, 1024, 256, null, 0, 1024), left.getRGB(0, 0, 1024, 256, null, 0, 1024));
        assertArrayEquals(complete.getRGB(1024, 0, 1024, 256, null, 0, 1024), right.getRGB(0, 0, 1024, 256, null, 0, 1024));
    }

    @Test
    void verticalTilesUseTheSameGridAndIntensityScale() {
        List<Point> points = List.of(new Point(100, 1012, 2), new Point(108, 1036, 5));
        BufferedImage complete = render(points, 0, 0, 256, 2048);
        BufferedImage lower = render(points, 0, 1024, 256, 1024);
        assertArrayEquals(complete.getRGB(0, 1024, 256, 1024, null, 0, 256), lower.getRGB(0, 0, 256, 1024, null, 0, 256));
    }

    @Test
    void emptyTileIsTransparentAndRepeatedClicksChangeIntensity() {
        BufferedImage empty = render(List.of(), 0, 0, 64, 64);
        BufferedImage single = render(List.of(new Point(32, 32, 1)), 0, 0, 64, 64);
        BufferedImage many = render(List.of(new Point(32, 32, 20)), 0, 0, 64, 64);
        assertEquals(0, empty.getRGB(32, 32));
        assertNotEquals(0, single.getRGB(32, 32));
        assertNotEquals(single.getRGB(32, 32), many.getRGB(32, 32));
    }
}
