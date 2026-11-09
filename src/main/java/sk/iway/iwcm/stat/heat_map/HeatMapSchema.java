package sk.iway.iwcm.stat.heat_map;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.stat.StatNewDB;

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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        long to = calendar.getTimeInMillis();
        calendar.set(Calendar.YEAR, 2000);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        long from = calendar.getTimeInMillis();
        String[] suffixes = StatNewDB.getTableSuffix("stat_clicks", from, to);
        int databaseType = Constants.DB_TYPE;
        int updated = 0;

        for (int i = 0; i < suffixes.length; i++) {
            String suffix = suffixes[i];
            String table = "stat_clicks" + suffix;
            Logger.println(HeatMapSchema.class, "Updating stat_clicks columns " + (i + 1) + "/" + suffixes.length + " " + suffix);
            try (Connection connection = DBPool.getConnection(); Statement statement = connection.createStatement()) {
                try {
                    statement.executeQuery("SELECT 1 FROM " + table + " WHERE 1=0").close();
                } catch (SQLException exception) {
                    if (HeatMapStorage.isMissingTable(exception)) continue;
                    throw exception;
                }
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
