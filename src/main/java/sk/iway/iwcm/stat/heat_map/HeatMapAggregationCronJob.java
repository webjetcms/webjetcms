package sk.iway.iwcm.stat.heat_map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/** Compacts completed monthly click tables into the same cells used to render heatmap tiles. */
public final class HeatMapAggregationCronJob {
    private static final Pattern MONTH = Pattern.compile("([2-9][0-9]{3})_(0?[1-9]|1[0-2])");
    private static final int BATCH_SIZE = 256;

    private HeatMapAggregationCronJob() { }

    /**
     * Compacts the selected monthly click tables and logs argument or database errors.
     *
     * @param args empty for all eligible months, or a month supplied as {@code YYYY_M}, as separate year and month
     *             arguments, or as {@code year=YYYY} and {@code month=M}
     */
    public static void main(String[] args) {
        try {
            YearMonth target = parseTarget(args);
            try (Connection connection = DBPool.getConnection()) {
                aggregate(connection, target, Clock.systemDefaultZone());
            }
        } catch (IllegalArgumentException | SQLException exception) {
            Logger.error(HeatMapAggregationCronJob.class, exception);
        }
    }

    /**
     * Resolves positional or named cron arguments to a calendar month.
     *
     * @param args arguments passed to the job
     * @return the selected month, or {@code null} for all months
     * @throws IllegalArgumentException if the arguments do not specify a valid target
     */
    static YearMonth parseTarget(String[] args) {
        if (args.length == 0) return null;
        String target = String.join("_", args);
        if (args.length == 2) {
            String year = null;
            String month = null;
            for (String arg : args) {
                String[] pair = arg.split("=", 2);
                if (pair.length != 2) continue;
                if ("year".equalsIgnoreCase(pair[0])) year = pair[1];
                else if ("month".equalsIgnoreCase(pair[0])) month = pair[1];
            }
            if (year != null && month != null) target = year + "_" + month;
        }
        Matcher match = MONTH.matcher(target);
        if (match.matches()) return YearMonth.of(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)));
        throw new IllegalArgumentException("Expected year=2025|month=10, 2025_10 or 2025|10, or no arguments for all months");
    }

    /**
     * Checks whether the delivery window for late clicks has elapsed since the end of the month.
     *
     * @param month partition month
     * @param clock source of the current instant and the time zone for the month boundary
     * @return true once the full delivery window has elapsed
     */
    static boolean eligible(YearMonth month, Clock clock) {
        var closed = month.plusMonths(1).atDay(1).atStartOfDay(clock.getZone()).toInstant();
        return !clock.instant().isBefore(closed.plusSeconds(HeatMapTrackingService.MAX_AGE_SECONDS));
    }

    /**
     * Compacts eligible tables, committing each batch independently.
     *
     * @param db dedicated connection on which the job sets isolation and disables auto-commit
     * @param target month to compact, or {@code null} to process all eligible months
     * @param clock clock used to determine which months are closed to late clicks
     * @throws SQLException if discovery or aggregation fails
     */
    static void aggregate(Connection db, YearMonth target, Clock clock) throws SQLException {
        if (target != null && !eligible(target, clock)) {
            Logger.warn(HeatMapAggregationCronJob.class, "Click table for " + target + " is still open for delivery");
            return;
        }
        List<YearMonth> months = HeatMapSchema.existingMonths(db);
        if (target != null) {
            if (!months.contains(target)) {
                Logger.warn(HeatMapAggregationCronJob.class, "No monthly click table for " + target);
                return;
            }
            months = List.of(target);
        }
        int cellSize = HeatMapStorage.cellSize();
        db.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        db.setAutoCommit(false);
        try {
            for (YearMonth month : months) {
                if (!eligible(month, clock)) continue;
                String table = HeatMapStorage.TABLE_NAME + HeatMapStorage.suffix(month);
                long removed = aggregateTable(db, table, cellSize);
                Logger.println(HeatMapAggregationCronJob.class, table + ": merged " + removed + " redundant click rows");
                db.commit();
            }
        } catch (SQLException | RuntimeException exception) {
            db.rollback();
            throw exception;
        }
    }

    /**
     * Compacts one monthly table in ranges of document IDs.
     *
     * @param db connection used by the aggregation job
     * @param table monthly click table discovered in the active database
     * @param cellSize cell size captured at the start of the job
     * @return number of redundant rows removed
     * @throws SQLException if reading or compacting a range fails
     */
    private static long aggregateTable(Connection db, String table, int cellSize) throws SQLException {
        // Page through the document index so a busy month never materializes all its clicks or groups in Java.
        long removed = 0;
        int lastDoc = 0;
        String sql = "SELECT DISTINCT document_id FROM " + table + " WHERE document_id > ? ORDER BY document_id";
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setMaxRows(BATCH_SIZE);
            int pageCount;
            do {
                int previousDoc = lastDoc;
                pageCount = 0;
                statement.setInt(1, previousDoc);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        lastDoc = rows.getInt(1);
                        pageCount++;
                    }
                }
                if (pageCount > 0) removed += aggregatePages(db, table, previousDoc, lastDoc, cellSize);
            } while (pageCount == BATCH_SIZE);
        }
        return removed;
    }

    /**
     * Describes a group of rows that share a reporting key and a heatmap cell.
     *
     * @param keeper lowest row ID, which receives the merged count
     * @param lastId highest row ID seen during discovery
     * @param total sum of click counts at discovery time
     */
    private record Cell(int keeper, int lastId, long total) { }

    /**
     * Merges duplicate cells for a range of documents, skipping cells whose totals exceed the count column.
     *
     * @param db connection used by the aggregation job
     * @param table monthly click table discovered in the active database
     * @param previousDoc exclusive lower document ID bound
     * @param lastDoc inclusive upper document ID bound
     * @param cellSize cell size captured at the start of the job
     * @return number of redundant rows removed
     * @throws SQLException if querying cells or committing a batch fails
     */
    private static long aggregatePages(Connection db, String table, int previousDoc, int lastDoc, int cellSize) throws SQLException {
        String sql = "SELECT MIN(stat_click_id), MAX(stat_click_id), " + HeatMapStorage.SUM_CLICKS + " FROM " + table
                + " WHERE document_id > ? AND document_id <= ? AND x IS NOT NULL AND y IS NOT NULL AND day_of_month IS NOT NULL"
                + " GROUP BY document_id, domain_name, day_of_month, coordinate_mode, viewport_width, source_viewport_width, "
                + HeatMapStorage.cellSql("x", cellSize) + ", " + HeatMapStorage.cellSql("y", cellSize)
                + " HAVING MIN(stat_click_id) > ? AND MIN(stat_click_id) <> MAX(stat_click_id) ORDER BY MIN(stat_click_id)";
        long removed = 0;
        int lastKeeper = 0;
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setMaxRows(BATCH_SIZE);
            statement.setInt(1, previousDoc);
            statement.setInt(2, lastDoc);
            List<Cell> cells = new ArrayList<>(BATCH_SIZE);
            do {
                cells.clear();
                statement.setInt(3, lastKeeper);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) cells.add(new Cell(rows.getInt(1), rows.getInt(2), rows.getLong(3)));
                }
                for (Cell cell : cells) {
                    lastKeeper = cell.keeper();
                    if (cell.total() > Integer.MAX_VALUE) {
                        Logger.warn(HeatMapAggregationCronJob.class, "Skipping overflowing click count in " + table + " at row " + lastKeeper);
                        continue;
                    }
                    int absorbed;
                    do {
                        absorbed = mergeBatch(db, table, cell, cellSize);
                        db.commit();
                        removed += absorbed;
                    } while (absorbed == BATCH_SIZE);
                }
            } while (cells.size() == BATCH_SIZE);
        }
        return removed;
    }

    /**
     * Locks the retained row, adds the weights of up to one batch of matching rows and deletes them.
     *
     * @param db connection whose transaction the caller must commit or roll back
     * @param table monthly click table discovered in the active database
     * @param cell retained row and upper ID bound from cell discovery
     * @param cellSize cell size captured at the start of the job
     * @return number of rows absorbed, or zero if the retained row is missing, no siblings remain or the total overflows
     * @throws SQLException if a database operation fails or the affected row counts differ from those expected
     */
    private static int mergeBatch(Connection db, String table, Cell cell, int cellSize) throws SQLException {
        // Competing jobs lock the same lowest ID before reading the remaining siblings.
        String lockSql = Constants.DB_TYPE == Constants.DB_MSSQL
                ? "SELECT count FROM " + table + " WITH (UPDLOCK, ROWLOCK) WHERE stat_click_id=?"
                : "SELECT count FROM " + table + " WHERE stat_click_id=? FOR UPDATE";
        long weight;
        try (PreparedStatement lock = db.prepareStatement(lockSql)) {
            lock.setInt(1, cell.keeper());
            try (ResultSet row = lock.executeQuery()) {
                if (!row.next()) return 0;
                weight = row.getLong(1);
            }
        }

        String sql = "SELECT c.stat_click_id, c.count FROM " + table + " c JOIN " + table + " k ON c.document_id=k.document_id"
                + " AND (c.domain_name=k.domain_name OR (c.domain_name IS NULL AND k.domain_name IS NULL))"
                + " AND c.day_of_month=k.day_of_month AND c.coordinate_mode=k.coordinate_mode"
                + " AND c.viewport_width=k.viewport_width AND c.source_viewport_width=k.source_viewport_width"
                + " AND " + HeatMapStorage.cellSql("c.x", cellSize) + "=" + HeatMapStorage.cellSql("k.x", cellSize)
                + " AND " + HeatMapStorage.cellSql("c.y", cellSize) + "=" + HeatMapStorage.cellSql("k.y", cellSize)
                + " WHERE k.stat_click_id=? AND c.stat_click_id>k.stat_click_id AND c.stat_click_id<=? ORDER BY c.stat_click_id";
        List<Integer> ids = new ArrayList<>(BATCH_SIZE);
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setInt(1, cell.keeper());
            statement.setInt(2, cell.lastId());
            statement.setMaxRows(BATCH_SIZE);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    ids.add(rows.getInt(1));
                    weight += rows.getInt(2);
                }
            }
        }
        if (ids.isEmpty()) return 0;
        if (weight > Integer.MAX_VALUE) {
            Logger.warn(HeatMapAggregationCronJob.class, "Skipping overflowing click count in " + table + " at row " + cell.keeper());
            return 0;
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement update = db.prepareStatement("UPDATE " + table + " SET count=? WHERE stat_click_id=?");
                PreparedStatement delete = db.prepareStatement("DELETE FROM " + table + " WHERE stat_click_id IN (" + placeholders + ")")) {
            update.setInt(1, (int) weight);
            update.setInt(2, cell.keeper());
            for (int i = 0; i < ids.size(); i++) delete.setInt(i + 1, ids.get(i));
            if (update.executeUpdate() != 1 || delete.executeUpdate() != ids.size()) {
                throw new SQLException("Click rows changed during aggregation of " + table + " at row " + cell.keeper());
            }
        }
        return ids.size();
    }
}
