package sk.iway.iwcm.stat.heat_map;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;

/** Upgrades existing monthly click tables while preserving their legacy identifiers and inserts. */
public final class HeatMapSchema {
    public static final int DEFAULT_VIEWPORT_WIDTH = 1920;
    private static final Pattern TABLE = Pattern.compile("stat_clicks_[0-9]{4}_([1-9]|1[0-2])", Pattern.CASE_INSENSITIVE);

    private HeatMapSchema() { }

    /**
     * Adds responsive tracking columns and indexes to every existing monthly table in the active schema.
     * Partial upgrades are resumed using JDBC metadata; failures must prevent the caller's success marker.
     *
     * @throws SQLException if a table cannot be upgraded
     */
    public static void upgradeExistingTables() throws SQLException {
        try (Connection connection = DBPool.getConnection()) {
            upgradeExistingTables(connection, Constants.DB_TYPE);
        }
    }

    static void upgradeExistingTables(Connection connection, int databaseType) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = currentSchema(connection, databaseType);
        List<String> tables = new ArrayList<>();
        try (ResultSet result = metadata.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
            while (result.next()) {
                if (schema != null && !schema.equals(result.getString("TABLE_SCHEM"))) continue;
                String table = result.getString("TABLE_NAME");
                if (table != null && TABLE.matcher(table).matches()) tables.add(table);
            }
        }
        for (String table : tables) {
            String target = schema == null ? table : quote(metadata, schema) + "." + table;
            Set<String> columns = columns(metadata, catalog, schema, table);
            addColumn(connection, catalog, schema, table, target, columns, "event_id", "CHAR(32) NULL");
            addColumn(connection, catalog, schema, table, target, columns, "domain_name", "VARCHAR(255) NULL");
            String widthDefinition = databaseType == Constants.DB_ORACLE
                    ? "INT DEFAULT " + DEFAULT_VIEWPORT_WIDTH + " NOT NULL"
                    : "INT NOT NULL DEFAULT " + DEFAULT_VIEWPORT_WIDTH;
            addColumn(connection, catalog, schema, table, target, columns, "viewport_width", widthDefinition);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE " + target + " SET domain_name = (SELECT LOWER(LTRIM(RTRIM(g.domain_name)))"
                        + " FROM " + quoteTable(metadata, "documents") + " d JOIN " + quoteTable(metadata, "groups") + " g ON g.group_id = d.group_id"
                        + " WHERE d.doc_id = " + table + ".document_id) WHERE domain_name IS NULL");
            }

            String suffix = table.substring("stat_clicks".length()).toLowerCase(Locale.ROOT);
            Set<String> indexes = indexes(metadata, catalog, schema, table);
            addIndex(connection, catalog, schema, table, indexes, "hm_event" + suffix,
                    uniqueIndexSql(target, suffix, databaseType));
            addIndex(connection, catalog, schema, table, indexes, "hm_page" + suffix,
                    pageIndexSql(target, suffix, databaseType));
        }
    }

    /** Returns a unique event index that permits any number of legacy rows without event identifiers. */
    public static String uniqueIndexSql(String table, String suffix, int databaseType) {
        return "CREATE UNIQUE INDEX hm_event" + suffix + " ON " + table + "(event_id)"
                + (databaseType == Constants.DB_MSSQL ? " WHERE event_id IS NOT NULL" : "");
    }

    /** Returns the lookup index, including support for older MySQL table engines and key-size limits. */
    public static String pageIndexSql(String table, String suffix, int databaseType) {
        String domain = databaseType == Constants.DB_MYSQL ? "domain_name(128)" : "domain_name";
        return "CREATE INDEX hm_page" + suffix + " ON " + table
                + "(" + domain + ", document_id, viewport_width, day_of_month)";
    }

    private static void addColumn(Connection connection, String catalog, String schema, String table,
            String target, Set<String> columns, String name, String definition) throws SQLException {
        if (columns.contains(name)) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + target + " ADD " + name + " " + definition);
        } catch (SQLException exception) {
            if (!columns(connection.getMetaData(), catalog, schema, table).contains(name)) throw exception;
        }
        columns.add(name);
    }

    private static void addIndex(Connection connection, String catalog, String schema, String table,
            Set<String> indexes, String name, String sql) throws SQLException {
        if (indexes.contains(name)) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            if (!indexes(connection.getMetaData(), catalog, schema, table).contains(name)) throw exception;
        }
        indexes.add(name);
    }

    private static Set<String> columns(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet result = metadata.getColumns(catalog, schema, table, "%")) {
            while (result.next()) {
                if (schema != null && !schema.equals(result.getString("TABLE_SCHEM"))) continue;
                if (!table.equalsIgnoreCase(result.getString("TABLE_NAME"))) continue;
                names.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static Set<String> indexes(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet result = metadata.getIndexInfo(catalog, schema, table, false, false)) {
            while (result.next()) {
                String name = result.getString("INDEX_NAME");
                if (name != null) names.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static String currentSchema(Connection connection, int databaseType) throws SQLException {
        if (databaseType == Constants.DB_MYSQL) return null;
        try {
            String schema = connection.getSchema();
            if (schema != null && !schema.isBlank()) return schema;
        } catch (SQLFeatureNotSupportedException ignored) {
            // Older JDBC drivers need a query for their active schema.
        } catch (AbstractMethodError ignored) {
            // jTDS predates the JDBC getSchema method.
        }
        String query = switch (databaseType) {
            case Constants.DB_MSSQL -> "SELECT SCHEMA_NAME()";
            case Constants.DB_ORACLE -> "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM dual";
            default -> "SELECT current_schema()";
        };
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            if (result.next()) {
                String schema = result.getString(1);
                if (schema != null && !schema.isBlank()) return schema;
            }
        }
        throw new SQLException("Cannot determine the active schema for click-table migration");
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
