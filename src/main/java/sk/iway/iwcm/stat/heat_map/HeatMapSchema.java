package sk.iway.iwcm.stat.heat_map;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/** Upgrades existing monthly click tables while preserving their legacy identifiers and inserts. */
public final class HeatMapSchema {
    public static final int DEFAULT_VIEWPORT_WIDTH = 1920;

    private static final Pattern MONTHLY_TABLE = Pattern.compile("stat_clicks_([2-9][0-9]{3})_([1-9]|1[0-2])", Pattern.CASE_INSENSITIVE);

    private HeatMapSchema() { }

    /**
     * Adds responsive tracking columns and indexes over the standard statistics migration date range.
     * Missing monthly tables and duplicate DDL are skipped; other failures prevent the caller's success marker.
     *
     * @throws SQLException if a table cannot be upgraded
     */
    public static void upgradeExistingTables() throws SQLException {
        Logger.println(HeatMapSchema.class, "Updating stat_clicks columns");
        YearMonth lastMonth = YearMonth.now().plusYears(1);
        int databaseType = Constants.DB_TYPE;
        int updated = 0;

        try (Connection connection = DBPool.getConnection(); Statement statement = connection.createStatement()) {
            for (YearMonth month : existingMonths(connection)) {
                if (month.isAfter(lastMonth)) continue;
                String suffix = HeatMapStorage.suffix(month);
                String table = HeatMapStorage.TABLE_NAME + suffix;
                Logger.println(HeatMapSchema.class, "Updating stat_clicks columns " + suffix);
                executeDdl(statement, "ALTER TABLE " + table + " ADD event_id CHAR(32) NULL", databaseType);
                executeDdl(statement, "ALTER TABLE " + table + " ADD domain_name VARCHAR(255) NULL", databaseType);
                String widthDefinition = databaseType == Constants.DB_ORACLE
                        ? "INT DEFAULT " + DEFAULT_VIEWPORT_WIDTH + " NOT NULL"
                        : "INT NOT NULL DEFAULT " + DEFAULT_VIEWPORT_WIDTH;
                executeDdl(statement, "ALTER TABLE " + table + " ADD viewport_width " + widthDefinition, databaseType);
                String coordinateDefinition = databaseType == Constants.DB_ORACLE ? "INT DEFAULT 0 NOT NULL" : "INT NOT NULL DEFAULT 0";
                executeDdl(statement, "ALTER TABLE " + table + " ADD coordinate_mode " + coordinateDefinition, databaseType);
                executeDdl(statement, "ALTER TABLE " + table + " ADD source_viewport_width " + coordinateDefinition, databaseType);

                DatabaseMetaData metadata = connection.getMetaData();
                statement.executeUpdate("UPDATE " + table + " SET domain_name = (SELECT LOWER(LTRIM(RTRIM(g.domain_name)))"
                        + " FROM " + quoteTable(metadata, "documents") + " d JOIN " + quoteTable(metadata, "groups") + " g ON g.group_id = d.group_id"
                        + " WHERE d.doc_id = " + table + ".document_id) WHERE domain_name IS NULL");
                statement.executeUpdate("UPDATE " + table + " SET source_viewport_width = viewport_width WHERE source_viewport_width = 0");

                executeDdl(statement, uniqueIndexSql(table, suffix, databaseType), databaseType);
                executeDdl(statement, pageIndexSql(table, suffix, databaseType), databaseType);
                updated++;
            }
        }
        Logger.println(HeatMapSchema.class, "Updated stat_clicks columns in " + updated + " tables");
    }

    /**
     * Adds a click count defaulting to one and converts existing MyISAM monthly tables to InnoDB.
     *
     * @throws SQLException if discovery, adding a missing column or converting a storage engine fails
     */
    public static void upgradeClickCounts() throws SQLException {
        try (Connection connection = DBPool.getConnection(); Statement statement = connection.createStatement()) {
            for (YearMonth month : existingMonths(connection)) {
                String table = HeatMapStorage.TABLE_NAME + HeatMapStorage.suffix(month);
                executeDdl(statement, "ALTER TABLE " + table + " ADD " + countColumnSql(Constants.DB_TYPE), Constants.DB_TYPE);
                if (Constants.DB_TYPE == Constants.DB_MYSQL) {
                    boolean myIsam;
                    try (ResultSet rows = statement.executeQuery("SELECT engine FROM information_schema.tables"
                            + " WHERE table_schema=DATABASE() AND table_name='" + table + "'")) {
                        myIsam = rows.next() && "MyISAM".equalsIgnoreCase(rows.getString(1));
                    }
                    if (myIsam) {
                        Logger.println(HeatMapSchema.class, "Converting " + table + " to InnoDB");
                        statement.executeUpdate("ALTER TABLE " + table + " ENGINE=InnoDB");
                    }
                }
            }
        }
    }

    /**
     * Adds covering report indexes after the count and InnoDB migrations have completed.
     * Converts legacy InnoDB row formats to support full utf8mb4 domain indexes.
     * Existing indexes are retained for short date ranges and legacy queries.
     *
     * @throws SQLException if an index cannot be created on a visible monthly table
     */
    public static void upgradeReportIndexes() throws SQLException {
        try (Connection connection = DBPool.getConnection(); Statement statement = connection.createStatement()) {
            for (YearMonth month : existingMonths(connection)) {
                String suffix = HeatMapStorage.suffix(month);
                String table = HeatMapStorage.TABLE_NAME + suffix;
                if (Constants.DB_TYPE == Constants.DB_MYSQL) {
                    boolean legacyRowFormat;
                    try (ResultSet rows = statement.executeQuery("SELECT row_format FROM information_schema.tables"
                            + " WHERE table_schema=DATABASE() AND table_name='" + table + "' AND engine='InnoDB'")) {
                        legacyRowFormat = rows.next() && ("Compact".equalsIgnoreCase(rows.getString(1))
                                || "Redundant".equalsIgnoreCase(rows.getString(1)));
                    }
                    if (legacyRowFormat) {
                        Logger.println(HeatMapSchema.class, "Converting " + table + " to DYNAMIC row format");
                        statement.executeUpdate("ALTER TABLE " + table + " ROW_FORMAT=DYNAMIC");
                    }
                }
                Logger.println(HeatMapSchema.class, "Adding heatmap report indexes to " + table);
                executeDdl(statement, widthIndexSql(table, suffix), Constants.DB_TYPE);
                executeDdl(statement, tileIndexSql(table, suffix), Constants.DB_TYPE);
            }
        }
    }

    /**
     * Finds monthly click tables that heatmap queries can reference without a schema qualifier.
     *
     * @param connection connection whose metadata identifies the tables
     * @return months with canonical partition names, earliest first
     * @throws SQLException if schema or table metadata cannot be read
     */
    static List<YearMonth> existingMonths(Connection connection) throws SQLException {
        String sql = switch (Constants.DB_TYPE) {
            case Constants.DB_PGSQL -> "SELECT c.relname AS table_name FROM pg_catalog.pg_class c"
                    + " WHERE c.relname LIKE 'stat_clicks_%' AND c.relkind IN ('r', 'p')"
                    + " AND pg_catalog.pg_table_is_visible(c.oid)";
            case Constants.DB_MSSQL -> "SELECT t.name AS table_name FROM sys.tables t"
                    + " WHERE t.name LIKE 'stat_clicks_%' AND t.object_id = OBJECT_ID(QUOTENAME(t.name), N'U')";
            default -> null;
        };
        if (sql != null) {
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
                return readMonths(rows);
            }
        }
        DatabaseMetaData metadata = connection.getMetaData();
        String schema = Constants.DB_TYPE == Constants.DB_MYSQL ? null : connection.getSchema();
        String pattern = metadata.storesUpperCaseIdentifiers() ? "STAT_CLICKS_%" : "stat_clicks_%";
        try (ResultSet rows = metadata.getTables(connection.getCatalog(), schema, pattern, new String[] {"TABLE"})) {
            return readMonths(rows);
        }
    }

    private static List<YearMonth> readMonths(ResultSet rows) throws SQLException {
        List<YearMonth> months = new ArrayList<>();
        while (rows.next()) {
            Matcher name = MONTHLY_TABLE.matcher(rows.getString("TABLE_NAME"));
            if (name.matches()) months.add(YearMonth.of(Integer.parseInt(name.group(1)), Integer.parseInt(name.group(2))));
        }
        months.sort(null);
        return months;
    }

    /**
     * Defines the weight column for existing clicks and inserts that omit the count.
     *
     * @param databaseType WebJET database type used to select the column syntax
     * @return column definition with a default count of one
     */
    public static String countColumnSql(int databaseType) {
        return databaseType == Constants.DB_ORACLE ? "count INT DEFAULT 1 NOT NULL" : "count INT NOT NULL DEFAULT 1";
    }

    /** Supplies the same coordinate columns for all newly created monthly partitions. */
    public static String coordinateColumnsSql(int databaseType) {
        String definition = databaseType == Constants.DB_ORACLE ? "INT DEFAULT 0 NOT NULL" : "INT NOT NULL DEFAULT 0";
        return "coordinate_mode " + definition + ",source_viewport_width " + definition + ",";
    }

    /** Returns a unique event index that permits any number of legacy rows without event identifiers. */
    public static String uniqueIndexSql(String table, String suffix, int databaseType) {
        return "CREATE UNIQUE INDEX hm_event" + suffix + " ON " + table + "(event_id)"
                + (databaseType == Constants.DB_MSSQL ? " WHERE event_id IS NOT NULL" : "");
    }

    /** Returns the lookup index, including support for older MySQL table engines and key-size limits. */
    public static String pageIndexSql(String table, String suffix, int databaseType) {
        String domain = databaseType == Constants.DB_MYSQL ? "domain_name(128)" : "domain_name";
        return "CREATE INDEX hm_page_v2" + suffix + " ON " + table
                + "(" + domain + ", document_id, coordinate_mode, viewport_width, day_of_month)";
    }

    /**
     * Covers width totals in grouping order without loading each click row or sorting the groups.
     * Full domain values are required for covering scans; MySQL tables must already use InnoDB.
     *
     * @param table trusted monthly click table name
     * @param suffix trusted monthly suffix used to make the index name unique
     * @return portable index DDL
     */
    public static String widthIndexSql(String table, String suffix) {
        return "CREATE INDEX hm_width" + suffix + " ON " + table
                + "(domain_name, document_id, viewport_width, coordinate_mode, source_viewport_width, day_of_month, count)";
    }

    /**
     * Restricts tile reads to their vertical band and covers all remaining filters and cell weights.
     * The Y range follows the equality columns so clicks elsewhere on a long page can be skipped.
     *
     * @param table trusted monthly click table name
     * @param suffix trusted monthly suffix used to make the index name unique
     * @return portable index DDL
     */
    public static String tileIndexSql(String table, String suffix) {
        return "CREATE INDEX hm_tile" + suffix + " ON " + table
                + "(domain_name, document_id, coordinate_mode, viewport_width, y, day_of_month, x, count)";
    }

    private static void executeDdl(Statement statement, String sql, int databaseType) throws SQLException {
        try {
            statement.execute(sql);
        } catch (SQLException exception) {
            boolean exists = switch (databaseType) {
                case Constants.DB_MYSQL -> exception.getErrorCode() == 1060 || exception.getErrorCode() == 1061;
                case Constants.DB_PGSQL -> "42701".equals(exception.getSQLState()) || "42P07".equals(exception.getSQLState());
                case Constants.DB_MSSQL -> exception.getErrorCode() == 2705 || exception.getErrorCode() == 1913;
                case Constants.DB_ORACLE -> exception.getErrorCode() == 1430 || exception.getErrorCode() == 955;
                default -> false;
            };
            if (!exists) throw exception;
        }
    }

    private static String quote(DatabaseMetaData metadata, String identifier) throws SQLException {
        String quote = metadata.getIdentifierQuoteString();
        if (quote == null || quote.isBlank()) {
            if (!identifier.matches("[a-zA-Z0-9_]+")) throw new SQLException("Unsupported schema identifier");
            return identifier;
        }
        String closing = "[".equals(quote) ? "]" : quote;
        return quote + identifier.replace(closing, closing + closing) + closing;
    }

    private static String quoteTable(DatabaseMetaData metadata, String table) throws SQLException {
        if (metadata.storesUpperCaseIdentifiers()) table = table.toUpperCase(Locale.ROOT);
        return quote(metadata, table);
    }
}
