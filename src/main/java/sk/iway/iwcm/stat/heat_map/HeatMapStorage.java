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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PARTITION = Pattern.compile("stat_clicks_([0-9]{4})_([1-9]|1[0-2])");

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
        StatWriteBuffer.addIdempotent("INSERT INTO " + TABLE_NAME + suffix
                + " (event_id, domain_name, document_id, day_of_month, viewport_width, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
                TABLE_NAME, suffix, event.eventId(), normalizeDomain(domain), event.docId(), date.getDayOfMonth(),
                event.viewportWidth(), event.x(), event.y());
    }

    /** Returns document click totals for an inclusive date range within one domain. */
    public static Map<Integer, Long> getPageCounts(String domain, LocalDate from, LocalDate to) {
        Map<Integer, Long> counts = new TreeMap<>();
        query(domain, from, to, "document_id, COUNT(*)", "", List.of(), " GROUP BY document_id",
                result -> counts.merge(result.getInt(1), result.getLong(2), Long::sum));
        return counts;
    }

    /** Returns click totals for each exact CSS viewport width of a document. */
    public static Map<Integer, Long> getWidths(String domain, int docId, LocalDate from, LocalDate to) {
        if (docId <= 0) throw new IllegalArgumentException("Invalid document identifier");
        Map<Integer, Long> counts = new TreeMap<>();
        query(domain, from, to, "viewport_width, COUNT(*)", " AND document_id = ?", List.of(docId),
                " GROUP BY viewport_width", result -> counts.merge(result.getInt(1), result.getLong(2), Long::sum));
        return counts;
    }

    /**
     * Returns a transparent PNG tile with a fixed intensity scale shared by every tile.
     * Tile indices are multiplied by {@link #TILE_SIZE}; neighboring clicks contribute across tile edges.
     *
     * @throws IOException if the PNG cannot be encoded or its cache cannot be accessed
     */
    public static byte[] getTile(String domain, int docId, LocalDate from, LocalDate to, int width, int tileX, int tileY) throws IOException {
        domain = normalizeDomain(domain);
        validateDates(from, to);
        if (docId <= 0 || width < 1 || width > MAX_VIEWPORT_WIDTH || tileX < 0 || tileY < 0
                || tileX > MAX_COORDINATE / TILE_SIZE || tileY > MAX_COORDINATE / TILE_SIZE) {
            throw new IllegalArgumentException("Invalid heatmap tile");
        }
        String key = String.join("|", "stat_clicks", domain, Integer.toString(docId), from.toString(), to.toString(),
                Integer.toString(width), Integer.toString(tileX), Integer.toString(tileY));
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
        int overlap = HeatMapTileRenderer.RADIUS + HeatMapTileRenderer.CELL_SIZE;
        HeatMapTileRenderer renderer = new HeatMapTileRenderer(originX, originY, TILE_SIZE, TILE_SIZE);
        String xCell = "FLOOR(x / " + HeatMapTileRenderer.CELL_SIZE + ".0)";
        String yCell = "FLOOR(y / " + HeatMapTileRenderer.CELL_SIZE + ".0)";
        query(domain, from, to, xCell + ", " + yCell + ", COUNT(*)", " AND document_id = ? AND viewport_width = ?"
                + " AND x BETWEEN ? AND ? AND y BETWEEN ? AND ?",
                List.of(docId, width, Math.max(0, originX - overlap), originX + TILE_SIZE + overlap,
                        Math.max(0, originY - overlap), originY + TILE_SIZE + overlap),
                " GROUP BY " + xCell + ", " + yCell,
                result -> renderer.add(result.getInt(1) * HeatMapTileRenderer.CELL_SIZE,
                        result.getInt(2) * HeatMapTileRenderer.CELL_SIZE, result.getLong(3)));
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
        try (Connection connection = DBPool.getConnection()) {
            for (YearMonth month : partitions(connection, from, to)) {
                int startDay = month.equals(YearMonth.from(from)) ? from.getDayOfMonth() : 1;
                int endDay = month.equals(YearMonth.from(to)) ? to.getDayOfMonth() : month.lengthOfMonth();
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

    private static SortedSet<YearMonth> partitions(Connection connection, LocalDate from, LocalDate to) throws SQLException {
        SortedSet<YearMonth> found = new TreeSet<>();
        String schema = null;
        try {
            schema = connection.getSchema();
        } catch (java.sql.SQLFeatureNotSupportedException ignored) {
            // Older JDBC drivers use their default schema for metadata lookups.
        } catch (AbstractMethodError ignored) {
            // Older JDBC drivers use their default schema for metadata lookups.
        }
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), schema, "%", new String[] { "TABLE" })) {
            while (tables.next()) {
                Matcher matcher = PARTITION.matcher(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
                if (!matcher.matches()) continue;
                YearMonth month = YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                if (!month.isBefore(YearMonth.from(from)) && !month.isAfter(YearMonth.from(to))) found.add(month);
            }
        }
        return found;
    }

    static boolean isMissingTable(SQLException exception) {
        return "42S02".equals(exception.getSQLState()) || "42P01".equals(exception.getSQLState())
                || exception.getErrorCode() == 942 || exception.getErrorCode() == 208;
    }

    static String suffix(YearMonth month) {
        return "_" + month.getYear() + "_" + month.getMonthValue();
    }

    private static String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank() || domain.length() > 255) throw new IllegalArgumentException("Invalid heatmap domain");
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
