package sk.iway.iwcm.stat.heat_map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ranks non-overlapping click regions without requiring visitor or element identifiers. */
public final class HeatMapHotspots {
    static final int CELL_SIZE = 40;
    private static final int REGION_CELLS = 3;
    private static final int LIMIT = 5;

    /** A half-open rectangle and the weighted center of its recorded clicks, in CSS pixels. */
    public record Hotspot(int x1, int y1, int x2, int y2, int x, int y, long clicks) {
        private boolean overlaps(Hotspot other) {
            return x1 < other.x2 && x2 > other.x1 && y1 < other.y2 && y2 > other.y1;
        }
    }

    /** The total includes clicks outside the ranked regions; each ranked click is counted once. */
    public record Report(long totalClicks, List<Hotspot> hotspots) { }

    private record Cell(int x, int y) { }

    private static final class Candidate {
        private long clicks;
        private double weightedX;
        private double weightedY;

        void add(long count, double x, double y) {
            clicks += count;
            weightedX += x;
            weightedY += y;
        }
    }

    private final Map<Cell, Candidate> candidates = new HashMap<>();
    private final int maximumX;
    private long totalClicks;

    HeatMapHotspots(int maximumX) {
        this.maximumX = maximumX;
    }

    /** Adds one occupied 40-pixel cell, retaining the stored weights and original coordinate sums. */
    void add(int cellX, int cellY, long clicks, double weightedX, double weightedY) {
        if (clicks <= 0) return;
        totalClicks += clicks;
        // Offset windows keep a compact cluster together even when it crosses a cell boundary.
        for (int y = Math.max(0, cellY - REGION_CELLS + 1); y <= cellY; y++) {
            for (int x = Math.max(0, cellX - REGION_CELLS + 1); x <= cellX; x++) {
                candidates.computeIfAbsent(new Cell(x, y), ignored -> new Candidate()).add(clicks, weightedX, weightedY);
            }
        }
    }

    /** Selects at most five disjoint 120-pixel regions, preferring centered windows when counts tie. */
    Report report() {
        List<Hotspot> ranked = new ArrayList<>(candidates.size());
        for (Map.Entry<Cell, Candidate> entry : candidates.entrySet()) {
            Cell cell = entry.getKey();
            Candidate value = entry.getValue();
            int x1 = cell.x() * CELL_SIZE;
            int y1 = cell.y() * CELL_SIZE;
            ranked.add(new Hotspot(x1, y1, Math.min(maximumX + 1, x1 + REGION_CELLS * CELL_SIZE),
                    Math.min(HeatMapStorage.MAX_COORDINATE + 1, y1 + REGION_CELLS * CELL_SIZE),
                    (int) Math.round(value.weightedX / value.clicks), (int) Math.round(value.weightedY / value.clicks), value.clicks));
        }
        ranked.sort(Comparator.comparingLong(Hotspot::clicks).reversed()
                .thenComparingDouble(HeatMapHotspots::centerDistance).thenComparingInt(Hotspot::y1).thenComparingInt(Hotspot::x1));
        List<Hotspot> selected = new ArrayList<>(LIMIT);
        for (Hotspot hotspot : ranked) {
            if (selected.stream().anyMatch(hotspot::overlaps)) continue;
            selected.add(hotspot);
            if (selected.size() == LIMIT) break;
        }
        return new Report(totalClicks, List.copyOf(selected));
    }

    private static double centerDistance(Hotspot hotspot) {
        double dx = (hotspot.x1() + hotspot.x2()) / 2.0 - hotspot.x();
        double dy = (hotspot.y1() + hotspot.y2()) / 2.0 - hotspot.y();
        return dx * dx + dy * dy;
    }
}
