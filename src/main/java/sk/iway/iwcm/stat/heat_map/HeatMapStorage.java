package sk.iway.iwcm.stat.heat_map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

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
                    counts.computeIfAbsent(key, ignored -> new TreeMap<>()).merge(sourceWidth, result.getLong(4), Long::sum);
                });
        return counts.entrySet().stream().map(entry -> {
            int previewWidth = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .findFirst().orElseThrow().getKey();
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
        domain = normalizeDomain(domain);
        validateDates(from, to);
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || mode == null || tileX < 0 || tileY < 0
                || tileX > MAX_COORDINATE / TILE_SIZE || tileY > MAX_COORDINATE / TILE_SIZE) {
            throw new IllegalArgumentException("Invalid heatmap tile");
        }
        int cellSize = cellSize();
        String key = String.join("|", "stat_clicks_v2", domain, Integer.toString(docId), from.toString(), to.toString(),
                Integer.toString(width), mode.name(), Integer.toString(tileX), Integer.toString(tileY), Integer.toString(cellSize));
        Path folder = Path.of(Tools.getRealPath("/WEB-INF/tmp/heat_map/"));
        Path cached = folder.resolve("tile_" + digest(key) + ".png");
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
        ImageIO.write(renderer.render(), "png", output);
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

    private static void query(String domain, LocalDate from, LocalDate to, String columns, String filter,
            List<Integer> parameters, String groupBy, RowConsumer consumer) {
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
                    statement.setString(1, domain);
                    statement.setInt(2, startDay);
                    statement.setInt(3, endDay);
                    for (int i = 0; i < parameters.size(); i++) statement.setInt(i + 4, parameters.get(i));
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

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(ResultSet result) throws SQLException;
    }
}
