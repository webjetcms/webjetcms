package sk.iway.iwcm.stat.rest;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Performs the explicit, resumable migration of versioned browser identifiers. */
@Service
public class BrowserIdentifierMigrationService implements DisposableBean {

    public static final String UPDATE_NOTE = "07.09.2026 [jeeff] browser identifier migration";

    private static final int ROW_BATCH_SIZE = 1_000;
    private static final int UPDATE_BATCH_SIZE = 500;
    private static final Pattern VERSION_SUFFIX = Pattern.compile("(?i)\\s+v?\\d+(?:[._-]\\d+)*$");
    private static final Pattern VERSION_ONLY = Pattern.compile("(?i)^v?\\d+(?:[._-]\\d+)*$");
    private static final Pattern SAFE_TABLE = Pattern.compile("[a-zA-Z0-9_]+");

    private final Object stateLock = new Object();
    private final ExecutorService executor;
    private State migrationState = new State();
    private MigrationPlan migrationPlan;
    private volatile boolean stopRequested;

    public BrowserIdentifierMigrationService() {
        this(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "browser-identifier-migration");
            thread.setDaemon(true);
            return thread;
        }));
    }

    BrowserIdentifierMigrationService(ExecutorService executor) {
        this.executor = executor;
    }

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
        private int totalTables;
        private long cursor;
        private long tableMaxId;
        private long scanned;
        private long updated;
        private boolean running;
        private boolean stopRequested;
        private boolean paused;
        private boolean done;
        private String table;
        private String error;
    }

    public Preview preview() throws SQLException {
        try (Connection connection = DBPool.getConnection()) {
            List<Mapping> botMappings = buildSeoBotMappings(connection, false);
            StatKeyMappingResult keyMappings = buildStatKeyMappings(connection, false);
            return new Preview(botMappings, keyMappings.mappings(), discoverTables(connection));
        }
    }

    public State getStatus() {
        synchronized (stateLock) {
            if (migrationState.isRunning() == false && migrationState.isDone() == false &&
                UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE)) {
                migrationState.setDone(true);
                migrationState.setTable("done");
            }
            return copyState(migrationState);
        }
    }

    public State start() {
        synchronized (stateLock) {
            if (migrationState.isRunning() == false && migrationState.isDone() == false &&
                UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE)) {
                migrationState.setDone(true);
                migrationState.setTable("done");
            }
            if (migrationState.isRunning() || migrationState.isDone()) return copyState(migrationState);

            migrationState.setRunning(true);
            migrationState.setStopRequested(false);
            migrationState.setPaused(false);
            migrationState.setError(null);
            stopRequested = false;
            try {
                executor.execute(this::runMigration);
            } catch (RejectedExecutionException ex) {
                migrationState.setRunning(false);
                migrationState.setError(ex.getMessage());
            }
            return copyState(migrationState);
        }
    }

    public State stop() {
        synchronized (stateLock) {
            if (migrationState.isRunning()) {
                stopRequested = true;
                migrationState.setStopRequested(true);
            }
            return copyState(migrationState);
        }
    }

    void runMigration() {
        try {
            MigrationPlan plan = prepareMigrationPlan();
            State preparedState = getStateSnapshot();
            preparedState.setTotalTables(plan.tables().size());
            publishRunningState(preparedState);

            while (isStopRequested() == false) {
                State nextState = getStateSnapshot();
                if (nextState.getTableIndex() >= plan.tables().size()) {
                    finishMigration(plan, nextState);
                    return;
                }

                nextState.setTable(plan.tables().get(nextState.getTableIndex()).name());
                publishRunningState(nextState);
                if (isStopRequested()) break;

                processNextBatch(plan, nextState);
                publishRunningState(nextState);
            }
            publishStoppedState();
        } catch (SQLException | RuntimeException ex) {
            if (isStopRequested()) {
                publishStoppedState();
            } else {
                Logger.error(BrowserIdentifierMigrationService.class, ex);
                publishFailedState(ex);
            }
        }
    }

    private MigrationPlan prepareMigrationPlan() throws SQLException {
        synchronized (stateLock) {
            if (migrationPlan != null) return migrationPlan;
        }

        List<Mapping> botMappings;
        StatKeyMappingResult keyMappingResult;
        List<TableDefinition> tables;
        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                botMappings = buildSeoBotMappings(connection, true);
                keyMappingResult = buildStatKeyMappings(connection, true);
                tables = tableDefinitions(connection);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
        refreshStatKeyCacheIfNeeded(keyMappingResult);

        MigrationPlan plan = new MigrationPlan(
            List.copyOf(botMappings),
            Map.copyOf(toIdMap(botMappings)),
            Map.copyOf(toIdMap(keyMappingResult.mappings())),
            List.copyOf(tables)
        );
        synchronized (stateLock) {
            if (migrationPlan == null) migrationPlan = plan;
            return migrationPlan;
        }
    }

    private void processNextBatch(MigrationPlan plan, State state) throws SQLException {
        TableDefinition table = plan.tables().get(state.getTableIndex());
        state.setTable(table.name);

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (state.getTableMaxId() < 1) state.setTableMaxId(maxId(connection, table));
                long lastId = migrateRows(connection, table, state, plan.botIds(), plan.keyIds());
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
                throw ex;
            }
        }
    }

    private void finishMigration(MigrationPlan plan, State state) throws SQLException {
        if (isStopRequested()) {
            publishStoppedState();
            return;
        }

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Every table was processed up to its captured maximum ID; new records already use canonical IDs.
                finalizeSeoBots(connection, plan.botMappings());
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
        refreshStatKeyCache();
        markAsCompleted();

        state.setRunning(false);
        state.setStopRequested(false);
        state.setPaused(false);
        state.setDone(true);
        state.setTable("done");
        synchronized (stateLock) {
            migrationState = copyState(state);
            migrationPlan = null;
            stopRequested = false;
        }
    }

    private State getStateSnapshot() {
        synchronized (stateLock) {
            return copyState(migrationState);
        }
    }

    private boolean isStopRequested() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    private void publishRunningState(State state) {
        state.setRunning(true);
        state.setPaused(false);
        synchronized (stateLock) {
            state.setStopRequested(stopRequested);
            migrationState = copyState(state);
        }
    }

    private void publishStoppedState() {
        synchronized (stateLock) {
            migrationState.setRunning(false);
            migrationState.setStopRequested(false);
            migrationState.setPaused(true);
            stopRequested = false;
        }
    }

    private void publishFailedState(Exception ex) {
        synchronized (stateLock) {
            migrationState.setRunning(false);
            migrationState.setStopRequested(false);
            migrationState.setPaused(false);
            migrationState.setError(ex.getMessage());
            stopRequested = false;
        }
    }

    private State copyState(State source) {
        State copy = new State();
        copy.setTableIndex(source.getTableIndex());
        copy.setTotalTables(source.getTotalTables());
        copy.setCursor(source.getCursor());
        copy.setTableMaxId(source.getTableMaxId());
        copy.setScanned(source.getScanned());
        copy.setUpdated(source.getUpdated());
        copy.setRunning(source.isRunning());
        copy.setStopRequested(source.isStopRequested());
        copy.setPaused(source.isPaused());
        copy.setDone(source.isDone());
        copy.setTable(source.getTable());
        copy.setError(source.getError());
        return copy;
    }

    @Override
    public void destroy() {
        stopRequested = true;
        executor.shutdownNow();
    }

    void markAsCompleted() {
        if (UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE) == false) {
            UpdateDatabase.saveSuccessUpdate(UPDATE_NOTE);
        }
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

    StatKeyMappingResult buildStatKeyMappings(Connection connection, boolean prepare) throws SQLException {
        List<StatKeyRow> rows = new ArrayList<>();
        List<Mapping> mappings = new ArrayList<>();
        Map<String, Long> existing = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT stat_keys_id, value FROM stat_keys");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StatKeyRow row = new StatKeyRow(rs.getLong(1), rs.getString(2));
                rows.add(row);
                existing.putIfAbsent(row.value.toLowerCase(Locale.ROOT), row.id);
            }
        }

        boolean cacheRefreshRequired = false;
        for (StatKeyRow row : rows) {
            String target = normalizeBrowserIdentifier(row.value);
            if (!target.equals(row.value) && !VERSION_ONLY.matcher(row.value.trim()).matches()) {
                String targetKey = target.toLowerCase(Locale.ROOT);
                long targetId = existing.getOrDefault(targetKey, 0L);
                if (targetId < 1 && prepare) {
                    targetId = getOrCreateStatKey(connection, target);
                    existing.put(targetKey, targetId);
                    cacheRefreshRequired = true;
                }
                if (targetId != row.id) mappings.add(new Mapping(row.id, targetId, row.value, target));
            }
        }
        if (prepare) verifyStatKeyTargets(connection, mappings);
        return new StatKeyMappingResult(mappings, cacheRefreshRequired);
    }

    private long getOrCreateStatKey(Connection connection, String target) throws SQLException {
        long targetId = findStatKeyId(connection, target);
        if (targetId > 0) return targetId;

        long allocatedId = PkeyGenerator.getNextValue("stat_keys");
        if (allocatedId < 1) throw new SQLException("Failed to allocate stat_keys ID for: " + target);
        verifyStatKeyIdIsAvailable(connection, allocatedId);

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO stat_keys (stat_keys_id, value) VALUES (?, ?)")) {
            ps.setLong(1, allocatedId);
            ps.setString(2, target);
            if (ps.executeUpdate() != 1) throw new SQLException("Failed to insert stat_keys value: " + target);
        }

        targetId = findStatKeyId(connection, target);
        if (targetId < 1) throw new SQLException("Inserted stat_keys value cannot be found: " + target);
        return targetId;
    }

    private long findStatKeyId(Connection connection, String target) throws SQLException {
        long targetId = 0;
        boolean found = false;
        try (PreparedStatement ps = connection.prepareStatement("SELECT stat_keys_id, value FROM stat_keys WHERE value=?")) {
            ps.setString(1, target);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (found) throw new SQLException("Multiple stat_keys rows found for value: " + target);
                    targetId = rs.getLong(1);
                    String value = rs.getString(2);
                    if (value == null || !target.equalsIgnoreCase(value)) {
                        throw new SQLException("Unexpected stat_keys value found for: " + target);
                    }
                    found = true;
                }
            }
        }
        if (found && targetId < 1) throw new SQLException("Invalid stat_keys ID for value: " + target);
        return targetId;
    }

    private void verifyStatKeyIdIsAvailable(Connection connection, long targetId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM stat_keys WHERE stat_keys_id=?")) {
            ps.setLong(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Allocated stat_keys ID already exists: " + targetId);
            }
        }
    }

    void verifyStatKeyTargets(Connection connection, List<Mapping> mappings) throws SQLException {
        Map<Long, String> targets = new LinkedHashMap<>();
        for (Mapping mapping : mappings) {
            if (mapping.targetId < 1) throw new SQLException("Invalid target stat_keys ID for: " + mapping.target);
            String previous = targets.putIfAbsent(mapping.targetId, mapping.target);
            if (previous != null && !previous.equalsIgnoreCase(mapping.target)) {
                throw new SQLException("Target stat_keys ID maps to multiple values: " + mapping.targetId);
            }
        }

        for (Map.Entry<Long, String> target : targets.entrySet()) {
            int rows = 0;
            try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM stat_keys WHERE stat_keys_id=?")) {
                ps.setLong(1, target.getKey());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows++;
                        String value = rs.getString(1);
                        if (value == null || !target.getValue().equalsIgnoreCase(value)) {
                            throw new SQLException("Target stat_keys ID does not match value: " + target.getKey());
                        }
                    }
                }
            }
            if (rows != 1) throw new SQLException("Target stat_keys ID must reference exactly one row: " + target.getKey());
        }
    }

    private void refreshStatKeyCache() {
        StatDB.getInstance(true);
        ClusterDB.addRefresh(StatDB.class);
    }

    private void refreshStatKeyCacheIfNeeded(StatKeyMappingResult result) {
        if (result.cacheRefreshRequired()) {
            refreshStatKeyCache();
            return;
        }
        if (result.mappings().isEmpty()) return;

        StatDB statDB = StatDB.getInstance();
        for (Mapping mapping : result.mappings()) {
            if (mapping.targetId > Integer.MAX_VALUE || !mapping.target.equalsIgnoreCase(statDB.getValue((int) mapping.targetId))) {
                refreshStatKeyCache();
                return;
            }
        }
    }

    long migrateRows(Connection connection, TableDefinition table, State state,
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
                    boolean oldBotNull = rs.wasNull();
                    long newBot = oldBotNull ? oldBot : botIds.getOrDefault(oldBot, oldBot);
                    long oldKey = 0;
                    boolean oldKeyNull = false;
                    if (table.browserKey) {
                        oldKey = rs.getLong(3);
                        oldKeyNull = rs.wasNull();
                    }
                    long newKey = !table.browserKey || oldKeyNull ? oldKey : keyIds.getOrDefault(oldKey, oldKey);
                    lastId = id;
                    state.setScanned(state.getScanned() + 1);
                    if ((!oldBotNull && newBot != oldBot) || (table.browserKey && !oldKeyNull && newKey != oldKey)) {
                        int parameter = 1;
                        if (oldBotNull) write.setNull(parameter++, Types.BIGINT);
                        else write.setLong(parameter++, newBot);
                        if (table.browserKey) {
                            if (oldKeyNull) write.setNull(parameter++, Types.INTEGER);
                            else write.setLong(parameter++, newKey);
                        }
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

    void finalizeSeoBots(Connection connection, List<Mapping> mappings) throws SQLException {
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

    void ensureUniqueNameIndex(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        TableReference table = findTable(connection, metadata, "seo_bots");
        if (hasUniqueSingleColumnIndex(metadata, table, "name")) return;

        SQLException createFailure = null;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE UNIQUE INDEX ix_seo_bots_name ON seo_bots (name)");
        } catch (SQLException ex) {
            createFailure = ex;
        }

        if (hasUniqueSingleColumnIndex(connection.getMetaData(), table, "name")) return;
        if (createFailure != null) throw createFailure;
        throw new SQLException("Unique index on seo_bots(name) was not created");
    }

    private TableReference findTable(Connection connection, DatabaseMetaData metadata, String expectedName) throws SQLException {
        String catalog = connection.getCatalog();
        String schema = getCurrentSchema(connection, metadata);

        TableReference table = findTable(metadata, catalog, schema, expectedName);
        if (table == null && schema != null) table = findTable(metadata, catalog, null, expectedName);
        if (table == null && catalog != null) table = findTable(metadata, null, null, expectedName);
        if (table == null) throw new SQLException("Table not found in database metadata: " + expectedName);
        return table;
    }

    private TableReference findTable(DatabaseMetaData metadata, String catalog, String schema, String expectedName) throws SQLException {
        TableReference match = null;
        try (ResultSet rs = metadata.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(tableName)) {
                    TableReference candidate = new TableReference(rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), tableName);
                    if (match != null && !match.equals(candidate)) {
                        throw new SQLException("Multiple tables found in database metadata: " + expectedName);
                    }
                    match = candidate;
                }
            }
        }
        return match;
    }

    private String getCurrentSchema(Connection connection, DatabaseMetaData metadata) throws SQLException {
        String driverName = metadata.getDriverName();
        if (driverName != null && driverName.toLowerCase(Locale.ROOT).contains("jtds")) return null;
        try {
            return connection.getSchema();
        } catch (SQLFeatureNotSupportedException ignored) {
            return null;
        }
    }

    private boolean hasUniqueSingleColumnIndex(DatabaseMetaData metadata, TableReference table, String expectedColumn) throws SQLException {
        Map<IndexReference, List<IndexColumn>> indexes = new LinkedHashMap<>();
        try (ResultSet rs = metadata.getIndexInfo(table.catalog, table.schema, table.name, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                if (indexName == null || nonUnique || rs.wasNull() || rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }
                IndexReference index = new IndexReference(rs.getString("INDEX_QUALIFIER"), indexName);
                indexes.computeIfAbsent(index, key -> new ArrayList<>())
                    .add(new IndexColumn(
                        rs.getShort("ORDINAL_POSITION"),
                        rs.getString("COLUMN_NAME"),
                        rs.getString("FILTER_CONDITION")
                    ));
            }
        }

        for (List<IndexColumn> columns : indexes.values()) {
            columns.sort(Comparator.comparingInt(IndexColumn::position));
            if (columns.size() == 1) {
                IndexColumn column = columns.get(0);
                if (column.position == 1 && expectedColumn.equalsIgnoreCase(column.name) && Tools.isEmpty(column.filterCondition)) {
                    return true;
                }
            }
        }
        return false;
    }

    List<TableDefinition> tableDefinitions(Connection connection) throws SQLException {
        List<TableDefinition> result = new ArrayList<>();
        for (String table : discoverTables(connection)) {
            Set<String> columns = readTableColumns(connection, table);
            if (columns.contains("browser_id") == false) continue;
            String lower = table.toLowerCase(Locale.ROOT);
            String id = lower.startsWith("stat_views") ? "view_id" : lower.startsWith("stat_from") ? "from_id" : "click_id";
            result.add(new TableDefinition(table, id, columns.contains("browser_ua_id")));
        }
        return result;
    }

    private List<String> discoverTables(Connection connection) throws SQLException {
        List<String> result = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = getCurrentSchema(connection, metadata);
        try (ResultSet rs = metadata.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
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

    private Set<String> readTableColumns(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
            ResultSetMetaData metadata = rs.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columns.add(metadata.getColumnName(i).toLowerCase(Locale.ROOT));
            }
        }
        return columns;
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
    private record StatKeyRow(long id, String value) {}
    private record MigrationPlan(List<Mapping> botMappings, Map<Long, Long> botIds,
                                 Map<Long, Long> keyIds, List<TableDefinition> tables) {}
    record TableDefinition(String name, String idColumn, boolean browserKey) {}
    private record TableReference(String catalog, String schema, String name) {}
    private record IndexReference(String qualifier, String name) {}
    private record IndexColumn(int position, String name, String filterCondition) {}
    record StatKeyMappingResult(List<Mapping> mappings, boolean cacheRefreshRequired) {}
}
