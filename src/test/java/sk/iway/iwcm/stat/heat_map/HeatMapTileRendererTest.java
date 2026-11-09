package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies that tile clipping never changes density or introduces seams. */
class HeatMapTileRendererTest {
    private record Point(int x, int y, long count) {
    }

    private static BufferedImage render(List<Point> points, int originX, int originY, int width, int height, int cellSize) {
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(originX, originY, width, height, cellSize);
        points.forEach(point -> renderer.add(point.x(), point.y(), point.count()));
        return renderer.render(100);
    }

    @Test
    void adjacentTilesMatchOneContinuousImageIncludingBothSidesOfTheSeam() {
        List<Point> points = List.of(
                new Point(1008, 120, 4), new Point(1026, 116, 8),
                new Point(1048, 132, 1), new Point(40, 40, 3));
        for (int cellSize : List.of(4, 5, 10)) {
            BufferedImage complete = render(points, 0, 0, 2048, 256, cellSize);
            BufferedImage left = render(points, 0, 0, 1024, 256, cellSize);
            BufferedImage right = render(points, 1024, 0, 1024, 256, cellSize);
            assertArrayEquals(complete.getRGB(0, 0, 1024, 256, null, 0, 1024), left.getRGB(0, 0, 1024, 256, null, 0, 1024));
            assertArrayEquals(complete.getRGB(1024, 0, 1024, 256, null, 0, 1024), right.getRGB(0, 0, 1024, 256, null, 0, 1024));
        }
    }

    @Test
    void verticalTilesUseTheSameGridAndIntensityScale() {
        List<Point> points = List.of(new Point(100, 1012, 2), new Point(108, 1036, 5));
        for (int cellSize : List.of(4, 5, 10)) {
            BufferedImage complete = render(points, 0, 0, 256, 2048, cellSize);
            BufferedImage lower = render(points, 0, 1024, 256, 1024, cellSize);
            assertArrayEquals(complete.getRGB(0, 1024, 256, 1024, null, 0, 256), lower.getRGB(0, 0, 256, 1024, null, 0, 256));
        }
    }

    /** Low and high click concentrations remain blue and red instead of both saturating. */
    @Test
    void distinguishesSmallAndLargeSharesOnTheSameReportScale() {
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(0, 0, 256, 128, 4);
        renderer.add(64, 64, 5026);
        renderer.add(192, 64, 276310);
        assertEquals(276310, renderer.maximumDensity());
        BufferedImage image = renderer.render(renderer.maximumDensity());
        int quiet = image.getRGB(64, 64);
        int busy = image.getRGB(192, 64);
        assertTrue((quiet & 255) > (quiet >> 16 & 255), "The small concentration must stay blue");
        assertEquals(0xb20000, busy & 0xffffff, "Only the report peak reaches red");
        assertTrue((quiet >>> 24) > 0, "Smaller concentrations must remain visible");
    }

    /** The reference includes nearby weighted cells, even when their combined peak lies between them. */
    @Test
    void measuresTheSmoothedPeakRatherThanTheLargestIndividualCell() {
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(0, 0, 128, 128, 4);
        renderer.add(56, 64, 100);
        renderer.add(72, 64, 100);
        assertTrue(renderer.maximumDensity() > 150);
        BufferedImage image = renderer.render(renderer.maximumDensity());
        assertEquals(0xb20000, image.getRGB(64, 64) & 0xffffff);
    }
}
