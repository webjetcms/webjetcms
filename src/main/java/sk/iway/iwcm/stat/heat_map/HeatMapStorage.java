package sk.iway.iwcm.stat.heat_map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.codec.digest.DigestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.StatWriteBuffer;

/** Stores anonymous click events and reads domain-scoped, width-specific heatmaps. */
public final class HeatMapStorage {
    public static final int TILE_SIZE = 1024;
    public static final int MAX_VIEWPORT_WIDTH = 16384;
    public static final int MAX_COORDINATE = 1_000_000;
    public static final String TABLE_NAME = "stat_clicks";

    // SQL Server sums INT columns as INT. DECIMAL also supports Oracle.
    static final String SUM_CLICKS = "SUM(CAST(count AS DECIMAL(19,0)))";
    private static final Map<DensityKey, CachedDensity> DENSITY_SCALES = Collections.synchronizedMap(new LRUMap<>(64));

    private record DensityKey(String domain, int docId, LocalDate from, LocalDate to, int width,
            HeatMapCoordinateMode mode, int cellSize) { }
    private record TilePosition(int x, int y) { }
    private record WeightedPoint(int x, int y, long count) { }

    private static final class CachedDensity {
        private double maximum;
        private long expiresAt;

        synchronized double get(DensityKey key) {
            if (maximum == 0 || System.currentTimeMillis() >= expiresAt) {
                maximum = readMaximumDensity(key);
                expiresAt = System.currentTimeMillis() + Math.max(0, Constants.getInt("statHeatMapImageTimeout", 300)) * 1000L;
            }
            return maximum;
        }
    }

    /** Returns the current cell size in CSS pixels, falling back to four outside the supported range. */
    static int cellSize() {
        int size = Constants.getInt("statHeatMapCellSize", 4);
        return size >= 1 && size <= TILE_SIZE ? size : 4;
    }

    /**
     * Maps a pixel coordinate to the same grid cell for aggregation and tile rendering.
     *
     * @param coordinate trusted SQL column or qualified column name
     * @param cellSize cell size captured for the current aggregation run or tile request
     * @return SQL expression for the zero-based cell index
     */
    static String cellSql(String coordinate, int cellSize) {
        return "FLOOR(" + coordinate + " / " + cellSize + ".0)";
    }

    private HeatMapStorage() {
    }

    /**
     * Queues a click for the partition containing its event date in the statistics timezone.
     * The caller checks consent, source-document eligibility and the event delivery age.
     *
     * @param event validated click
     * @param domain canonical domain name of the source document
     */
    public static void record(HeatMapEvent event, String domain) {
        LocalDate date = Instant.ofEpochSecond(event.epochSeconds()).atZone(ZoneId.systemDefault()).toLocalDate();
        if (date.getYear() < 2000 || date.getYear() > 9999) throw new IllegalArgumentException("Invalid heatmap event date");
        String suffix = suffix(YearMonth.from(date));
        HeatMapCoordinateMode mode = event.containerWidth() > 0 ? HeatMapCoordinateMode.CONTAINER : HeatMapCoordinateMode.DOCUMENT;
        int width = event.containerWidth() > 0 ? event.containerWidth() : event.viewportWidth();
        StatWriteBuffer.addIdempotent("INSERT INTO " + TABLE_NAME + suffix
                + " (event_id, domain_name, document_id, day_of_month, viewport_width, x, y, coordinate_mode, source_viewport_width) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                TABLE_NAME, suffix, event.eventId(), normalizeDomain(domain), event.docId(), date.getDayOfMonth(),
                width, event.x(), event.y(), mode.databaseValue(), event.viewportWidth());
    }

    /**
     * Sums click weights by document across the monthly tables in the date range.
     *
     * @param domain domain whose clicks are included
     * @param from first day to include
     * @param to last day to include
     * @return click totals keyed by document ID
     */
    public static Map<Integer, Long> getPageCounts(String domain, LocalDate from, LocalDate to) {
        Map<Integer, Long> counts = new TreeMap<>();
        query(domain, from, to, "document_id, " + SUM_CLICKS, "", List.of(), " GROUP BY document_id",
                result -> counts.merge(result.getInt(1), result.getLong(2), Long::sum));
        return counts;
    }

    /** A coordinate group and its most frequent source viewport, with ties resolved toward the smaller viewport. */
    public record WidthCount(int width, HeatMapCoordinateMode mode, long clicks, int previewWidth) { }

    private record WidthKey(int width, HeatMapCoordinateMode mode) { }

    /** Recorded click weights inside a rectangle and across its complete map group. */
    public record AreaCount(long clicks, long totalClicks) { }

    /**
     * Ranks the busiest disjoint areas using the same domain, page, date and coordinate group as the map.
     * Stored click weights are preserved across monthly partitions and historical aggregation.
     *
     * @param domain authorized reporting domain
     * @param docId authorized page
     * @param from first included day
     * @param to last included day
     * @param width coordinate group width
     * @param mode document or container coordinates
     * @return up to five ranked regions and the total number of valid recorded clicks
     */
    public static HeatMapHotspots.Report getHotspots(String domain, int docId, LocalDate from, LocalDate to, int width,
            HeatMapCoordinateMode mode) {
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || mode == null) throw new IllegalArgumentException("Invalid heatmap group");
        int maximumX = mode == HeatMapCoordinateMode.CONTAINER ? width : MAX_COORDINATE;
        HeatMapHotspots hotspots = new HeatMapHotspots(maximumX);
        String xCell = cellSql("x", HeatMapHotspots.CELL_SIZE);
        String yCell = cellSql("y", HeatMapHotspots.CELL_SIZE);
        query(domain, from, to, xCell + ", " + yCell + ", " + SUM_CLICKS
                + ", SUM(CAST(x AS DECIMAL(28,0)) * count), SUM(CAST(y AS DECIMAL(28,0)) * count)",
                " AND document_id = ? AND viewport_width = ? AND coordinate_mode = ? AND x BETWEEN 0 AND ?"
                        + " AND y BETWEEN 0 AND " + MAX_COORDINATE + " AND count > 0",
                List.of(docId, width, mode.databaseValue(), maximumX), " GROUP BY " + xCell + ", " + yCell,
                result -> hotspots.add(result.getInt(1), result.getInt(2), result.getLong(3), result.getDouble(4), result.getDouble(5)));
        return hotspots.report();
    }

    /**
     * Counts an area using stored coordinates and weights, including compacted historical clicks.
     * Rectangle starts are inclusive and ends are exclusive. Both totals use the same query per month.
     *
     * @param domain authorized reporting domain
     * @param docId authorized page
     * @param from first included day
     * @param to last included day
     * @param width coordinate group width
     * @param mode document or container coordinates
     * @param x1 inclusive left edge
     * @param y1 inclusive top edge
     * @param x2 exclusive right edge
     * @param y2 exclusive bottom edge
     * @return selected and total recorded click counts
     */
    public static AreaCount getAreaCount(String domain, int docId, LocalDate from, LocalDate to, int width,
            HeatMapCoordinateMode mode, int x1, int y1, int x2, int y2) {
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || mode == null
                || x1 < 0 || y1 < 0 || x2 <= x1 || y2 <= y1 || x2 > MAX_COORDINATE + 1 || y2 > MAX_COORDINATE + 1) {
            throw new IllegalArgumentException("Invalid heatmap area");
        }
        long[] counts = new long[2];
        query(domain, from, to,
                "SUM(CASE WHEN x >= ? AND x < ? AND y >= ? AND y < ? THEN CAST(count AS DECIMAL(19,0)) ELSE 0 END), " + SUM_CLICKS,
                List.of(x1, x2, y1, y2), " AND document_id = ? AND viewport_width = ? AND coordinate_mode = ?",
                List.of(docId, width, mode.databaseValue()), "", result -> {
                    counts[0] += result.getLong(1);
                    counts[1] += result.getLong(2);
                });
        return new AreaCount(counts[0], counts[1]);
    }

    /**
     * Groups clicks by coordinate mode and width and selects the most frequent source viewport for each group.
     *
     * @param domain domain whose clicks are included
     * @param docId source document ID
     * @param from first day to include
     * @param to last day to include
     * @return groups ordered by descending click total, then by width and mode
     */
    public static List<WidthCount> getWidths(String domain, int docId, LocalDate from, LocalDate to) {
        if (docId <= 0) throw new IllegalArgumentException("Invalid document identifier");
        Map<WidthKey, Map<Integer, Long>> counts = new HashMap<>();
        query(domain, from, to, "viewport_width, coordinate_mode, source_viewport_width, " + SUM_CLICKS,
                " AND document_id = ?", List.of(docId),
                " GROUP BY viewport_width, coordinate_mode, source_viewport_width", result -> {
                    WidthKey key = new WidthKey(result.getInt(1), HeatMapCoordinateMode.fromDatabase(result.getInt(2)));
                    int sourceWidth = result.getInt(3);
                    if (sourceWidth == 0) sourceWidth = key.width();
                    counts.computeIfAbsent(key, ignored -> new HashMap<>()).merge(sourceWidth, result.getLong(4), Long::sum);
                });
        return counts.entrySet().stream().map(entry -> {
            int previewWidth = entry.getValue().entrySet().stream()
                    .min(Map.Entry.<Integer, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .orElseThrow().getKey();
            long clicks = entry.getValue().values().stream().mapToLong(Long::longValue).sum();
            return new WidthCount(entry.getKey().width(), entry.getKey().mode(), clicks, previewWidth);
        }).sorted(Comparator.comparingLong(WidthCount::clicks).reversed().thenComparingInt(WidthCount::width)
                .thenComparing(WidthCount::mode)).toList();
    }

    /**
     * Renders or reads a cached PNG tile, including contributions from clicks just outside its edges.
     *
     * @param domain domain whose clicks are included
     * @param docId source document ID
     * @param from first day to include
     * @param to last day to include
     * @param width width of the coordinate group in pixels
     * @param mode coordinate system used when recording the clicks
     * @param tileX zero-based horizontal tile index
     * @param tileY zero-based vertical tile index
     * @return transparent PNG tile with the shared heatmap intensity scale
     * @throws IOException if the PNG cannot be encoded or its cache cannot be accessed
     */
    public static byte[] getTile(String domain, int docId, LocalDate from, LocalDate to, int width,
            HeatMapCoordinateMode mode, int tileX, int tileY) throws IOException {
        return getTile(domain, docId, from, to, width, mode, tileX, tileY,
                getMaximumDensity(domain, docId, from, to, width, mode));
    }

    /**
     * Renders a tile using the peak for its page, selected version interval and coordinate group.
     *
     * @param domain authorized reporting domain
     * @param docId authorized page
     * @param from first day whose clicks are rendered
     * @param to last day whose clicks are rendered
     * @param width coordinate group width
     * @param mode document or container coordinates
     * @param tileX horizontal tile index
     * @param tileY vertical tile index
     * @param maximumDensity peak smoothed density for the same page, day range, width and mode as the tile
     * @return transparent PNG tile
     * @throws IOException if the PNG or its cache cannot be written
     */
    public static byte[] getTile(String domain, int docId, LocalDate from, LocalDate to, int width,
            HeatMapCoordinateMode mode, int tileX, int tileY, double maximumDensity) throws IOException {
        domain = normalizeDomain(domain);
        validateDates(from, to);
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || mode == null || tileX < 0 || tileY < 0
                || tileX > MAX_COORDINATE / TILE_SIZE || tileY > MAX_COORDINATE / TILE_SIZE
                || !Double.isFinite(maximumDensity) || maximumDensity <= 0) {
            throw new IllegalArgumentException("Invalid heatmap tile");
        }
        int cellSize = cellSize();
        String key = String.join("|", "stat_clicks_v3", domain, Integer.toString(docId), from.toString(), to.toString(),
                Integer.toString(width), mode.name(), Integer.toString(tileX), Integer.toString(tileY), Integer.toString(cellSize),
                Double.toString(maximumDensity));
        Path folder = Path.of(Tools.getRealPath("/WEB-INF/tmp/heat_map/"));
        Path cached = folder.resolve("tile_" + DigestUtils.sha256Hex(key) + ".png");
        int timeout = Math.max(0, Constants.getInt("statHeatMapImageTimeout", 300));
        try {
            if (timeout > 0 && Files.isRegularFile(cached)
                    && System.currentTimeMillis() - Files.getLastModifiedTime(cached).toMillis() < timeout * 1000L) {
                return Files.readAllBytes(cached);
            }
        } catch (java.nio.file.NoSuchFileException ignored) {
            // The existing image cleaner may remove a tile between checking and reading it.
        }
        int originX = tileX * TILE_SIZE;
        int originY = tileY * TILE_SIZE;
        int overlap = ((HeatMapTileRenderer.RADIUS + cellSize - 1) / cellSize + 1) * cellSize;
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(originX, originY, TILE_SIZE, TILE_SIZE, cellSize);
        String xCell = cellSql("x", cellSize);
        String yCell = cellSql("y", cellSize);
        query(domain, from, to, xCell + ", " + yCell + ", " + SUM_CLICKS, " AND document_id = ? AND viewport_width = ? AND coordinate_mode = ?"
                + " AND x BETWEEN ? AND ? AND y BETWEEN ? AND ?",
                List.of(docId, width, mode.databaseValue(), Math.max(0, originX - overlap), originX + TILE_SIZE + overlap,
                        Math.max(0, originY - overlap), originY + TILE_SIZE + overlap),
                " GROUP BY " + xCell + ", " + yCell,
                result -> renderer.add(result.getInt(1) * cellSize, result.getInt(2) * cellSize, result.getLong(3)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(renderer.render(maximumDensity), "png", output);
        byte[] png = output.toByteArray();
        if (timeout > 0) {
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, "tile_", ".tmp");
            try {
                Files.write(temporary, png);
                try {
                    Files.move(temporary, cached, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return png;
    }

    /**
     * Returns a cached peak after smoothing all clicks in the selected interval on the same grid used by its tiles.
     * Each comparison pane supplies its own version interval so its strongest concentration reaches red.
     *
     * @param domain authorized reporting domain
     * @param docId authorized page
     * @param from first day included for the selected version, or the report start when all versions are selected
     * @param to last day included for the selected version, or the report end when all versions are selected
     * @param width coordinate group width
     * @param mode document or container coordinates
     * @return positive reference density, or one for an empty report
     */
    public static double getMaximumDensity(String domain, int docId, LocalDate from, LocalDate to, int width, HeatMapCoordinateMode mode) {
        validateDates(from, to);
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || mode == null) throw new IllegalArgumentException("Invalid heatmap group");
        DensityKey key = new DensityKey(normalizeDomain(domain), docId, from, to, width, mode, cellSize());
        return DENSITY_SCALES.computeIfAbsent(key, ignored -> new CachedDensity()).get(key);
    }

    /** Keeps the sparse click cells in memory and smooths only one bounded tile at a time. */
    private static double readMaximumDensity(DensityKey key) {
        Map<TilePosition, List<WeightedPoint>> points = new HashMap<>();
        Set<TilePosition> tiles = new HashSet<>();
        String xCell = cellSql("x", key.cellSize());
        String yCell = cellSql("y", key.cellSize());
        query(key.domain(), key.from(), key.to(), xCell + ", " + yCell + ", " + SUM_CLICKS,
                " AND document_id = ? AND viewport_width = ? AND coordinate_mode = ? AND x BETWEEN 0 AND " + MAX_COORDINATE
                        + " AND y BETWEEN 0 AND " + MAX_COORDINATE,
                List.of(key.docId(), key.width(), key.mode().databaseValue()), " GROUP BY " + xCell + ", " + yCell, result -> {
                    int x = result.getInt(1) * key.cellSize();
                    int y = result.getInt(2) * key.cellSize();
                    TilePosition tile = new TilePosition(x / TILE_SIZE, y / TILE_SIZE);
                    points.computeIfAbsent(tile, ignored -> new ArrayList<>()).add(new WeightedPoint(x, y, result.getLong(3)));
                    // The peak may fall across an edge, including in a neighboring tile with no click cells.
                    int overlap = ((HeatMapTileRenderer.RADIUS + key.cellSize() - 1) / key.cellSize() + 1) * key.cellSize();
                    for (int ty = Math.max(0, y - overlap) / TILE_SIZE; ty <= (y + overlap) / TILE_SIZE; ty++) {
                        for (int tx = Math.max(0, x - overlap) / TILE_SIZE; tx <= (x + overlap) / TILE_SIZE; tx++) tiles.add(new TilePosition(tx, ty));
                    }
                });
        double maximum = 1;
        for (TilePosition tile : tiles) {
            HeatMapTileRenderer renderer = new HeatMapTileRenderer(tile.x() * TILE_SIZE, tile.y() * TILE_SIZE, TILE_SIZE, TILE_SIZE, key.cellSize());
            // Large configurable cells can extend the interpolation halo across two tiles.
            int reach = (HeatMapTileRenderer.RADIUS + 2 * key.cellSize() + TILE_SIZE - 1) / TILE_SIZE;
            for (int y = tile.y() - reach; y <= tile.y() + reach; y++) {
                for (int x = tile.x() - reach; x <= tile.x() + reach; x++) {
                    for (WeightedPoint point : points.getOrDefault(new TilePosition(x, y), List.of())) renderer.add(point.x(), point.y(), point.count());
                }
            }
            maximum = Math.max(maximum, renderer.maximumDensity());
        }
        return maximum;
    }

    private static void query(String domain, LocalDate from, LocalDate to, String columns, String filter,
            List<Integer> parameters, String groupBy, RowConsumer consumer) {
        query(domain, from, to, columns, List.of(), filter, parameters, groupBy, consumer);
    }

    private static void query(String domain, LocalDate from, LocalDate to, String columns, List<Integer> columnParameters,
            String filter, List<Integer> parameters, String groupBy, RowConsumer consumer) {
        domain = normalizeDomain(domain);
        validateDates(from, to);
        YearMonth firstMonth = YearMonth.from(from);
        YearMonth lastMonth = YearMonth.from(to);
        try (Connection connection = DBPool.getConnection()) {
            for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
                int startDay = month.equals(firstMonth) ? from.getDayOfMonth() : 1;
                int endDay = month.equals(lastMonth) ? to.getDayOfMonth() : month.lengthOfMonth();
                String sql = "SELECT " + columns + " FROM " + TABLE_NAME + suffix(month)
                        + " WHERE domain_name = ? AND day_of_month BETWEEN ? AND ?" + filter + groupBy;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int parameter = 1;
                    for (int value : columnParameters) statement.setInt(parameter++, value);
                    statement.setString(parameter++, domain);
                    statement.setInt(parameter++, startDay);
                    statement.setInt(parameter++, endDay);
                    for (int value : parameters) statement.setInt(parameter++, value);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) consumer.accept(result);
                    }
                } catch (SQLException exception) {
                    if (!isMissingTable(exception)) throw exception;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot read heatmap statistics", exception);
        }
    }

    static boolean isMissingTable(SQLException exception) {
        return "42S02".equals(exception.getSQLState()) || "42P01".equals(exception.getSQLState())
                || exception.getErrorCode() == 942 || exception.getErrorCode() == 208;
    }

    static String suffix(YearMonth month) {
        return "_" + month.getYear() + "_" + month.getMonthValue();
    }

    private static String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("Invalid heatmap date range");
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(ResultSet result) throws SQLException;
    }
}
