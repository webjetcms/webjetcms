package sk.iway.iwcm.stat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Performs the explicit, resumable migration of versioned browser identifiers. */
@Service
public class BrowserIdentifierMigrationService {

    private static final int ROW_BATCH_SIZE = 10_000;
    private static final int UPDATE_BATCH_SIZE = 500;
    private static final Pattern VERSION_SUFFIX = Pattern.compile("(?i)\\s+v?\\d+(?:[._-]\\d+)*$");
    private static final Pattern VERSION_ONLY = Pattern.compile("(?i)^v?\\d+(?:[._-]\\d+)*$");
    private static final Pattern SAFE_TABLE = Pattern.compile("[a-zA-Z0-9_]+");

    @Getter
    @AllArgsConstructor
    public static class Mapping {
        private final long sourceId;
        private final long targetId;
        private final String source;
        private final String target;
    }

    @Getter
    @AllArgsConstructor
    public static class Preview {
        private final List<Mapping> seoBots;
        private final List<Mapping> browserKeys;
        private final List<String> tables;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class State {
        private int tableIndex;
        private long cursor;
        private long tableMaxId;
        private long scanned;
        private long updated;
        private boolean done;
        private String table;
        private String error;
    }

    public Preview preview() throws SQLException {
        try (Connection connection = DBPool.getConnection()) {
            return new Preview(buildSeoBotMappings(connection, false), buildStatKeyMappings(connection, false), discoverTables(connection));
        }
    }

    public State process(State state) throws SQLException {
        if (state == null) state = new State();
        if (state.isDone()) return state;
        if (state.getTableIndex() < 0 || state.getCursor() < 0 || state.getTableMaxId() < 0) {
            throw new SQLException("Invalid migration state");
        }

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Mapping> botMappings = buildSeoBotMappings(connection, true);
                List<Mapping> keyMappings = buildStatKeyMappings(connection, true);
                List<TableDefinition> tables = tableDefinitions(connection);

                if (state.getTableIndex() >= tables.size()) {
                    verifyNoReferences(connection, tables, botMappings, keyMappings);
                    finalizeSeoBots(connection, botMappings);
                    connection.commit();
                    StatDB.getInstance(true);
                    ClusterDB.addRefresh(StatDB.class);
                    state.setDone(true);
                    state.setTable("done");
                    return state;
                }

                TableDefinition table = tables.get(state.getTableIndex());
                state.setTable(table.name);
                if (state.getTableMaxId() < 1) state.setTableMaxId(maxId(connection, table));

                Map<Long, Long> botIds = toIdMap(botMappings);
                Map<Long, Long> keyIds = toIdMap(keyMappings);
                long lastId = migrateRows(connection, table, state, botIds, keyIds);
                connection.commit();

                if (lastId == 0 || lastId >= state.getTableMaxId()) {
                    state.setTableIndex(state.getTableIndex() + 1);
                    state.setCursor(0);
                    state.setTableMaxId(0);
                } else {
                    state.setCursor(lastId);
                }
            } catch (SQLException ex) {
                connection.rollback();
                state.setError(ex.getMessage());
                throw ex;
            }
        }
        return state;
    }

    static String normalizeBrowserIdentifier(String value) {
        if (Tools.isEmpty(value)) return "Unknown";
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (VERSION_ONLY.matcher(normalized).matches()) return "Unknown";
        normalized = VERSION_SUFFIX.matcher(normalized).replaceFirst("").trim();
        return Tools.isEmpty(normalized) ? "Unknown" : normalized;
    }

    private List<Mapping> buildSeoBotMappings(Connection connection, boolean prepare) throws SQLException {
        List<BrowserRow> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT seo_bots_id, name FROM seo_bots ORDER BY seo_bots_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new BrowserRow(rs.getLong(1), rs.getString(2)));
        }

        Map<String, List<BrowserRow>> groups = new LinkedHashMap<>();
        for (BrowserRow row : rows) {
            String canonical = normalizeBrowserIdentifier(row.name);
            groups.computeIfAbsent(canonical.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(row);
        }

        List<Mapping> result = new ArrayList<>();
        for (List<BrowserRow> group : groups.values()) {
            String canonical = normalizeBrowserIdentifier(group.get(0).name);
            BrowserRow target = group.stream()
                .filter(row -> canonical.equalsIgnoreCase(row.name))
                .min(Comparator.comparingLong(row -> row.id)).orElse(group.get(0));
            if (prepare && !canonical.equals(target.name)) {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE seo_bots SET name=? WHERE seo_bots_id=?")) {
                    ps.setString(1, canonical);
                    ps.setLong(2, target.id);
                    ps.executeUpdate();
                }
            }
            for (BrowserRow row : group) {
                if (row.id != target.id) result.add(new Mapping(row.id, target.id, row.name, canonical));
            }
        }
        return result;
    }

    private List<Mapping> buildStatKeyMappings(Connection connection, boolean prepare) throws SQLException {
        List<Mapping> mappings = new ArrayList<>();
        Map<String, Long> existing = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT stat_keys_id, value FROM stat_keys");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) existing.putIfAbsent(rs.getString(2).toLowerCase(Locale.ROOT), rs.getLong(1));
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT stat_keys_id, value FROM stat_keys");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long sourceId = rs.getLong(1);
                String source = rs.getString(2);
                String target = normalizeBrowserIdentifier(source);
                if (!target.equals(source) && !VERSION_ONLY.matcher(source.trim()).matches()) {
                    long targetId = existing.getOrDefault(target.toLowerCase(Locale.ROOT), 0L);
                    if (targetId < 1 && prepare) targetId = StatDB.getStatKeyId(target);
                    if (targetId != sourceId) mappings.add(new Mapping(sourceId, targetId, source, target));
                }
            }
        }
        return mappings;
    }

    private long migrateRows(Connection connection, TableDefinition table, State state,
                             Map<Long, Long> botIds, Map<Long, Long> keyIds) throws SQLException {
        String select = "SELECT " + table.idColumn + ", browser_id" + (table.browserKey ? ", browser_ua_id" : "") +
            " FROM " + table.name + " WHERE " + table.idColumn + ">? AND " + table.idColumn + "<=? ORDER BY " + table.idColumn;
        String update = "UPDATE " + table.name + " SET browser_id=?" + (table.browserKey ? ", browser_ua_id=?" : "") +
            " WHERE " + table.idColumn + "=?";
        long lastId = 0;
        int pending = 0;
        try (PreparedStatement read = connection.prepareStatement(select);
             PreparedStatement write = connection.prepareStatement(update)) {
            read.setLong(1, state.getCursor());
            read.setLong(2, state.getTableMaxId());
            read.setMaxRows(ROW_BATCH_SIZE);
            try (ResultSet rs = read.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    long oldBot = rs.getLong(2);
                    long newBot = botIds.getOrDefault(oldBot, oldBot);
                    long oldKey = table.browserKey ? rs.getLong(3) : 0;
                    long newKey = table.browserKey ? keyIds.getOrDefault(oldKey, oldKey) : 0;
                    lastId = id;
                    state.setScanned(state.getScanned() + 1);
                    if (newBot != oldBot || newKey != oldKey) {
                        int parameter = 1;
                        write.setLong(parameter++, newBot);
                        if (table.browserKey) write.setLong(parameter++, newKey);
                        write.setLong(parameter, id);
                        write.addBatch();
                        pending++;
                        state.setUpdated(state.getUpdated() + 1);
                        if (pending == UPDATE_BATCH_SIZE) {
                            write.executeBatch();
                            pending = 0;
                        }
                    }
                }
            }
            if (pending > 0) write.executeBatch();
        }
        return lastId;
    }

    private void finalizeSeoBots(Connection connection, List<Mapping> mappings) throws SQLException {
        Map<Long, List<Long>> sources = new HashMap<>();
        for (Mapping mapping : mappings) sources.computeIfAbsent(mapping.targetId, key -> new ArrayList<>()).add(mapping.sourceId);
        for (Map.Entry<Long, List<Long>> entry : sources.entrySet()) {
            long count = 0;
            Timestamp latest = null;
            List<Long> ids = new ArrayList<>(entry.getValue());
            ids.add(entry.getKey());
            for (Long id : ids) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT visit_count, last_visit FROM seo_bots WHERE seo_bots_id=?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            count += rs.getLong(1);
                            Timestamp date = rs.getTimestamp(2);
                            if (date != null && (latest == null || date.after(latest))) latest = date;
                        }
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("UPDATE seo_bots SET visit_count=?, last_visit=? WHERE seo_bots_id=?")) {
                ps.setLong(1, count);
                ps.setTimestamp(2, latest);
                ps.setLong(3, entry.getKey());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM seo_bots WHERE seo_bots_id=?")) {
                for (Long id : entry.getValue()) {
                    ps.setLong(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        ensureUniqueNameIndex(connection);
    }

    private void verifyNoReferences(Connection connection, List<TableDefinition> tables,
                                    List<Mapping> botMappings, List<Mapping> keyMappings) throws SQLException {
        for (TableDefinition table : tables) {
            verifyColumnHasNoReferences(connection, table, "browser_id", botMappings);
            if (table.browserKey) verifyColumnHasNoReferences(connection, table, "browser_ua_id", keyMappings);
        }
    }

    private void verifyColumnHasNoReferences(Connection connection, TableDefinition table,
                                             String column, List<Mapping> mappings) throws SQLException {
        if (mappings.isEmpty()) return;
        for (int offset = 0; offset < mappings.size(); offset += UPDATE_BATCH_SIZE) {
            int end = Math.min(offset + UPDATE_BATCH_SIZE, mappings.size());
            String placeholders = String.join(",", java.util.Collections.nCopies(end - offset, "?"));
            String sql = "SELECT " + table.idColumn + " FROM " + table.name + " WHERE " + column + " IN (" + placeholders + ")";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setMaxRows(1);
                for (int i = offset; i < end; i++) ps.setLong(i - offset + 1, mappings.get(i).sourceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new SQLException("Migration is incomplete; browser references remain in " + table.name);
                }
            }
        }
    }

    private void ensureUniqueNameIndex(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rs = metadata.getIndexInfo(null, null, "seo_bots", true, false)) {
            while (rs.next()) if ("name".equalsIgnoreCase(rs.getString("COLUMN_NAME"))) return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE UNIQUE INDEX ix_seo_bots_name ON seo_bots (name)");
        }
    }

    private List<TableDefinition> tableDefinitions(Connection connection) throws SQLException {
        List<TableDefinition> result = new ArrayList<>();
        for (String table : discoverTables(connection)) {
            if (!hasColumn(connection, table, "browser_id")) continue;
            String lower = table.toLowerCase(Locale.ROOT);
            String id = lower.startsWith("stat_views") ? "view_id" : lower.startsWith("stat_from") ? "from_id" : "click_id";
            result.add(new TableDefinition(table, id, hasColumn(connection, table, "browser_ua_id")));
        }
        return result;
    }

    private List<String> discoverTables(Connection connection) throws SQLException {
        List<String> result = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                String lower = table.toLowerCase(Locale.ROOT);
                if (SAFE_TABLE.matcher(table).matches() && (lower.matches("stat_views(_\\d{4}_\\d{1,2})?") ||
                    lower.matches("stat_from(_\\d{4}_\\d{1,2})?") || lower.equals("emails_stat_click"))) result.add(table);
            }
        }
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) return true;
        }
        return false;
    }

    private long maxId(Connection connection, TableDefinition table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT MAX(" + table.idColumn + ") FROM " + table.name)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private Map<Long, Long> toIdMap(List<Mapping> mappings) {
        Map<Long, Long> result = new HashMap<>();
        for (Mapping mapping : mappings) result.put(mapping.sourceId, mapping.targetId);
        return result;
    }

    private record BrowserRow(long id, String name) {}
    private record TableDefinition(String name, String idColumn, boolean browserKey) {}
}
