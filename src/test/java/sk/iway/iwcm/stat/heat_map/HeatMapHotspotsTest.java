package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies spatial grouping, weighted ranking and non-overlapping totals independently of persistence. */
class HeatMapHotspotsTest {
    private static void add(HeatMapHotspots report, int x, int y, long clicks) {
        report.add(x / HeatMapHotspots.CELL_SIZE, y / HeatMapHotspots.CELL_SIZE, clicks, (double) x * clicks, (double) y * clicks);
    }

    /** A cluster crossing a grid boundary stays together and keeps weights above the integer range. */
    @Test
    void combinesNearbyCellsAndPreservesLargeWeights() {
        HeatMapHotspots hotspots = new HeatMapHotspots(1140);
        add(hotspots, 119, 199, 2_000_000_000L);
        add(hotspots, 121, 201, 2_000_000_000L);
        add(hotspots, 800, 1500, 7);
        var report = hotspots.report();
        assertEquals(4_000_000_007L, report.totalClicks());
        assertEquals(2, report.hotspots().size());
        var first = report.hotspots().get(0);
        assertEquals(4_000_000_000L, first.clicks());
        assertEquals(120, first.x());
        assertEquals(200, first.y());
        assertTrue(first.x1() <= 119 && first.x2() > 121 && first.y1() <= 199 && first.y2() > 201);
    }

    /** Adjacent candidates never claim the same click and leave unranked traffic in the total. */
    @Test
    void limitsTheRankingToFiveDisjointRegions() {
        HeatMapHotspots hotspots = new HeatMapHotspots(1140);
        for (int index = 0; index < 8; index++) add(hotspots, 200, 200 + index * 240, 8 - index);
        var report = hotspots.report();
        assertEquals(36, report.totalClicks());
        assertEquals(List.of(8L, 7L, 6L, 5L, 4L), report.hotspots().stream().map(HeatMapHotspots.Hotspot::clicks).toList());
    }

    /** Edge regions remain bounded and tied counts produce the same result regardless of input order. */
    @Test
    void handlesEmptyReportsEdgesAndStableTies() {
        assertEquals(new HeatMapHotspots.Report(0, List.of()), new HeatMapHotspots(390).report());
        HeatMapHotspots forward = new HeatMapHotspots(390);
        HeatMapHotspots reverse = new HeatMapHotspots(390);
        add(forward, 0, 0, 10);
        add(forward, 390, HeatMapStorage.MAX_COORDINATE, 10);
        add(reverse, 390, HeatMapStorage.MAX_COORDINATE, 10);
        add(reverse, 0, 0, 10);
        assertEquals(forward.report(), reverse.report());
        for (var area : forward.report().hotspots()) {
            assertTrue(area.x1() >= 0 && area.y1() >= 0);
            assertTrue(area.x2() <= 391 && area.y2() <= HeatMapStorage.MAX_COORDINATE + 1);
            assertTrue(area.x() >= area.x1() && area.x() < area.x2());
            assertTrue(area.y() >= area.y1() && area.y() < area.y2());
        }
    }
}
