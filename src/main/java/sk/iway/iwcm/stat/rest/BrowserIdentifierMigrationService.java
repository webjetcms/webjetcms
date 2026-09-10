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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * Migrates versioned browser identifiers to canonical browser-family identifiers.
 *
 * <p>The migration runs on a single background worker, commits changes in bounded batches,
 * and exposes a synchronized progress snapshot so an administrator can pause, resume, and
 * monitor it. Finalization is a separate explicit step that merges browser counters, removes
 * obsolete identifiers, creates the uniqueness constraint, and refreshes statistics caches
 * across the cluster.</p>
 */
@Service
public class BrowserIdentifierMigrationService implements DisposableBean {

    public static final String UPDATE_NOTE = "08.09.2026 [jeeff] browser identifier migration including stat_error";

    private static final int ROW_BATCH_SIZE = 1_000;
    private static final int UPDATE_BATCH_SIZE = 500;
    private static final Pattern VERSION_SUFFIX = Pattern.compile("(?i)\\s+v?\\d+(?:[._-]\\d+)*$");
    private static final Pattern VERSION_ONLY = Pattern.compile("(?i)^v?\\d+(?:[._-]\\d+)*$");
    private final Object stateLock = new Object();
    private final ExecutorService executor;
    private State migrationState = new State();
    private MigrationPlan migrationPlan;
    private volatile boolean stopRequested;

    /** Creates the production service with one dedicated daemon worker thread. */
    public BrowserIdentifierMigrationService() {
        this(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "browser-identifier-migration");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * Creates the service with an externally supplied executor, primarily for deterministic tests.
     *
     * @param executor executor that accepts migration and finalization tasks
     */
    BrowserIdentifierMigrationService(ExecutorService executor) {
        this.executor = executor;
    }

    /** Describes one source identifier that must be replaced by its canonical target. */
    @Getter
    @AllArgsConstructor
    public static class Mapping {
        private final long sourceId;
        private final long targetId;
        private final String source;
        private final String target;
    }

    /** Contains the read-only analysis shown before an administrator starts the migration. */
    @Getter
    @AllArgsConstructor
    public static class Preview {
        private final List<Mapping> seoBots;
        private final List<Mapping> browserKeys;
        private final List<String> tables;
    }

    /** Summarizes how many obsolete statistics keys were deleted or retained during finalization. */
    @Getter
    @AllArgsConstructor
    public static class FinalizationResult {
        private final int deletedStatKeys;
        private final int retainedStatKeys;
    }

    /**
     * Mutable migration progress transferred through the REST API as a detached snapshot.
     *
     * <p>Cursor values describe the current table, while the cumulative counters cover the
     * complete migration. Lifecycle flags distinguish active migration, cooperative pause,
     * completion, and the separate finalization phase.</p>
     */
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
        private long tableUpdated;
        private long tableDurationMillis;
        private boolean running;
        private boolean stopRequested;
        private boolean paused;
        private boolean done;
        private boolean finalizing;
        private boolean finalized;
        private int deletedStatKeys;
        private int retainedStatKeys;
        private long updatedStatErrors;
        private String table;
        private String error;
    }

    /**
     * Analyzes the current database without creating identifiers or modifying statistics.
     *
     * @return mappings that would be applied and the statistics tables that would be processed
     * @throws SQLException if identifiers or table metadata cannot be read
     */
    public Preview preview() throws SQLException {
        try (Connection connection = DBPool.getConnection()) {
            List<Mapping> botMappings = buildSeoBotMappings(connection, false);
            StatKeyMappingResult keyMappings = buildStatKeyMappings(connection, false);
            return new Preview(botMappings, keyMappings.mappings(), discoverTables(connection));
        }
    }

    /**
     * Returns the latest detached progress snapshot.
     *
     * <p>A newly created service also restores the completed flag from the persistent update
     * marker before returning the snapshot.</p>
     *
     * @return a copy of the current migration state
     */
    public State getStatus() {
        synchronized (stateLock) {
            restoreCompletedState();
            return copyState(migrationState);
        }
    }

    /**
     * Starts or resumes migration on the background worker.
     *
     * <p>The request is ignored when a task is already running or the persistent migration
     * marker reports completion. A paused migration reuses its prepared plan and cursor.</p>
     *
     * @return the state immediately after the start request
     */
    public State start() {
        synchronized (stateLock) {
            restoreCompletedState();
            if (migrationState.isRunning() || migrationState.isDone()) return copyState(migrationState);

            migrationState.setRunning(true);
            migrationState.setStopRequested(false);
            migrationState.setPaused(false);
            migrationState.setFinalizing(false);
            migrationState.setFinalized(false);
            migrationState.setUpdatedStatErrors(0);
            migrationState.setError(null);
            stopRequested = false;
            executeInBackground(this::runMigration);
            return copyState(migrationState);
        }
    }

    /**
     * Requests a cooperative pause after the currently executing database batch finishes.
     *
     * @return the state containing the stop-request flag when migration is active
     */
    public State stop() {
        synchronized (stateLock) {
            if (migrationState.isRunning()) {
                stopRequested = true;
                migrationState.setStopRequested(true);
            }
            return copyState(migrationState);
        }
    }

    /**
     * Starts asynchronous cleanup after all statistics references have been migrated.
     *
     * @return the state immediately after the finalization request
     * @throws SQLException if the migration has not been marked as completed
     */
    public State finalizeCompletedMigration() throws SQLException {
        synchronized (stateLock) {
            if (migrationState.isRunning()) return copyState(migrationState);
            if (UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE) == false) {
                throw new SQLException("Browser identifier migration must be completed before finalization");
            }
            migrationState.setRunning(true);
            migrationState.setDone(false);
            migrationState.setFinalizing(true);
            migrationState.setFinalized(false);
            migrationState.setTableIndex(0);
            migrationState.setTotalTables(0);
            migrationState.setCursor(0);
            migrationState.setTableMaxId(0);
            migrationState.setScanned(0);
            migrationState.setUpdated(0);
            migrationState.setUpdatedStatErrors(0);
            migrationState.setError(null);
            migrationState.setTable("finalizing");
            executeInBackground(this::runFinalization);
            return copyState(migrationState);
        }
    }

    /**
     * Restores in-memory completion from the persistent update marker.
     *
     * <p>The caller must hold {@link #stateLock} while invoking this method.</p>
     */
    private void restoreCompletedState() {
        if (migrationState.isRunning() == false && migrationState.isDone() == false &&
            UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE)) {
            migrationState.setDone(true);
            migrationState.setTable("done");
        }
    }

    /**
     * Submits a migration phase to the worker and publishes submission failures as state errors.
     *
     * @param task background migration or finalization task to execute
     */
    private void executeInBackground(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            publishFailedState(ex);
        }
    }

    /**
     * Rebuilds and validates canonical mappings, then removes obsolete identifiers transactionally.
     *
     * <p>Successful cleanup is audited and followed by a local and cluster-wide statistics cache
     * refresh. Any database or runtime failure is logged and exposed in the shared state.</p>
     */
    private void runFinalization() {
        try {
            List<Mapping> botMappings;
            StatKeyMappingResult keyMappings;
            try (Connection connection = DBPool.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    botMappings = buildSeoBotMappings(connection, false);
                    keyMappings = buildStatKeyMappings(connection, false);
                    verifyStatKeyTargets(connection, keyMappings.mappings());
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
            refreshStatKeyCacheIfNeeded(keyMappings);

            FinalizationResult result;
            try (Connection connection = DBPool.getConnection()) {
                List<Mapping> unusedKeys = findUnusedStatKeys(connection, keyMappings.mappings());
                connection.setAutoCommit(false);
                try {
                    finalizeSeoBots(connection, botMappings);
                    int deleted = deleteStatKeys(connection, unusedKeys);
                    result = new FinalizationResult(deleted, keyMappings.mappings().size() - deleted);
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
            auditFinalization(botMappings.size(), result);
            refreshStatKeyCache();
            synchronized (stateLock) {
                migrationState.setRunning(false);
                migrationState.setDone(true);
                migrationState.setFinalizing(false);
                migrationState.setFinalized(true);
                migrationState.setDeletedStatKeys(result.deletedStatKeys);
                migrationState.setRetainedStatKeys(result.retainedStatKeys);
                migrationState.setTable("done");
            }
        } catch (SQLException | RuntimeException ex) {
            Logger.error(BrowserIdentifierMigrationService.class, ex);
            publishFailedState(ex);
        }
    }

    public static boolean isAllreadyUpdated() {
        return UpdateDatabase.isAllreadyUpdated(BrowserIdentifierMigrationService.UPDATE_NOTE);
    }

    /**
     * Processes the prepared migration plan until completion, pause, or failure.
     *
     * <p>Each loop iteration commits at most one bounded batch and publishes a new detached
     * progress snapshot. A failed batch leaves the previously published cursor unchanged so the
     * same work can be retried.</p>
     */
    void runMigration() {
        try {
            MigrationPlan plan = prepareMigrationPlan();
            State preparedState = getStateSnapshot();
            preparedState.setTotalTables(plan.tables().size());
            publishRunningState(preparedState);

            while (isStopRequested() == false) {
                State nextState = getStateSnapshot();
                if (nextState.getTableIndex() >= plan.tables().size()) {
                    finishMigration(nextState);
                    return;
                }

                nextState.setTable(plan.tables().get(nextState.getTableIndex()).name());
                publishRunningState(nextState);
                if (isStopRequested()) break;

                int tableIndex = nextState.getTableIndex();
                long updatedBefore = nextState.getUpdated();
                long batchStarted = System.nanoTime();
                processNextBatch(plan, nextState);
                nextState.setTableDurationMillis(nextState.getTableDurationMillis() +
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - batchStarted));
                nextState.setTableUpdated(nextState.getTableUpdated() + nextState.getUpdated() - updatedBefore);
                if (nextState.getTableIndex() > tableIndex) {
                    auditCompletedTable(nextState.getTable(), nextState.getTableUpdated(), nextState.getTableDurationMillis());
                    nextState.setTableUpdated(0);
                    nextState.setTableDurationMillis(0);
                }
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

    /**
     * Creates canonical identifiers and builds the immutable work plan used by background batches.
     *
     * <p>The plan is cached after the first successful preparation so a paused migration resumes
     * with exactly the same mappings and ordered table list.</p>
     *
     * @return the existing resumable plan or a newly prepared plan
     * @throws SQLException if mappings, target identifiers, or table definitions cannot be prepared
     */
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
            List.copyOf(keyMappingResult.mappings()),
            Map.copyOf(toIdMap(botMappings)),
            Map.copyOf(toIdMap(keyMappingResult.mappings())),
            List.copyOf(tables)
        );
        synchronized (stateLock) {
            if (migrationPlan == null) migrationPlan = plan;
            return migrationPlan;
        }
    }

    /**
     * Processes and commits the next batch for the table selected by the supplied state.
     *
     * <p>Regular statistics tables advance by their numeric row identifier. {@code stat_error}
     * tables have no row identifier and therefore advance through chunks of identifier mappings.</p>
     *
     * @param plan immutable identifier mappings and ordered table definitions
     * @param state detached state to update after the batch succeeds
     * @throws SQLException if the batch cannot be read, updated, committed, or rolled back
     */
    private void processNextBatch(MigrationPlan plan, State state) throws SQLException {
        TableDefinition table = plan.tables().get(state.getTableIndex());
        state.setTable(table.name);

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (table.statError) {
                    processStatErrorBatch(connection, table, state, plan.keyMappings());
                    connection.commit();
                    return;
                }
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

    /**
     * Applies one mapping chunk to a {@code stat_error} table using a set-based update.
     *
     * @param connection transactional connection used for the current batch
     * @param table {@code stat_error} table being updated
     * @param state detached state whose mapping cursor and counters are advanced
     * @param mappings ordered browser-key mappings processed by cursor offset
     * @throws SQLException if the set-based update fails
     */
    private void processStatErrorBatch(Connection connection, TableDefinition table, State state,
                                       List<Mapping> mappings) throws SQLException {
        if (state.getTableMaxId() < 1) state.setTableMaxId(mappings.size());
        int offset = Math.toIntExact(state.getCursor());
        int end = Math.min(offset + UPDATE_BATCH_SIZE, mappings.size());
        if (offset < end) {
            long updatedRows = updateStatErrorMappingBatch(connection, table.name, mappings.subList(offset, end));
            state.setUpdated(state.getUpdated() + updatedRows);
            state.setUpdatedStatErrors(state.getUpdatedStatErrors() + updatedRows);
            state.setScanned(state.getScanned() + updatedRows);
            state.setCursor(end);
        }
        if (state.getCursor() >= mappings.size()) {
            state.setTableIndex(state.getTableIndex() + 1);
            state.setCursor(0);
            state.setTableMaxId(0);
        }
    }

    /**
     * Rewrites one group of browser-key identifiers in a {@code stat_error} table.
     *
     * <p>The table name must come from the migration's strict table-name allowlist. A single
     * {@code UPDATE CASE} statement replaces every source ID in the supplied mapping chunk.</p>
     *
     * @param connection transactional connection used for the update
     * @param table validated physical table name
     * @param mappings source-to-target browser-key mappings for this batch
     * @return number of database rows changed by the update
     * @throws SQLException if the update cannot be prepared or executed
     */
    long updateStatErrorMappingBatch(Connection connection, String table, List<Mapping> mappings) throws SQLException {
        if (mappings.isEmpty()) return 0;
        StringBuilder sql = new StringBuilder("UPDATE ").append(table)
            .append(" SET browser_ua_id=CASE browser_ua_id ");
        for (int i = 0; i < mappings.size(); i++) sql.append("WHEN ? THEN ? ");
        sql.append("ELSE browser_ua_id END WHERE browser_ua_id IN (")
            .append(String.join(",", java.util.Collections.nCopies(mappings.size(), "?")))
            .append(')');
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            for (Mapping mapping : mappings) {
                ps.setLong(parameter++, mapping.sourceId);
                ps.setLong(parameter++, mapping.targetId);
            }
            for (Mapping mapping : mappings) ps.setLong(parameter++, mapping.sourceId);
            return ps.executeUpdate();
        }
    }

    /**
     * Persists successful completion and publishes the terminal migration state.
     *
     * <p>A late stop request wins over completion. Otherwise the resumable plan is discarded only
     * after the update marker and terminal state have been stored.</p>
     *
     * @param state detached state positioned after the final table
     */
    private void finishMigration(State state) {
        if (isStopRequested()) {
            publishStoppedState();
            return;
        }

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

    /**
     * Copies the shared state while holding its synchronization lock.
     *
     * @return detached state safe for mutation by the background worker
     */
    private State getStateSnapshot() {
        synchronized (stateLock) {
            return copyState(migrationState);
        }
    }

    /**
     * Checks both the cooperative stop flag and worker-thread interruption.
     *
     * @return {@code true} when the active phase should stop after its current safe point
     */
    private boolean isStopRequested() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    /**
     * Publishes a running-state snapshot while preserving the latest stop request.
     *
     * @param state detached progress state produced by the worker
     */
    private void publishRunningState(State state) {
        state.setRunning(true);
        state.setPaused(false);
        synchronized (stateLock) {
            state.setStopRequested(stopRequested);
            migrationState = copyState(state);
        }
    }

    /** Marks the current phase as paused and clears the consumed stop request. */
    private void publishStoppedState() {
        synchronized (stateLock) {
            migrationState.setRunning(false);
            migrationState.setStopRequested(false);
            migrationState.setPaused(true);
            stopRequested = false;
        }
    }

    /**
     * Publishes a terminal error state and clears active lifecycle flags.
     *
     * @param ex failure whose message is exposed to the administrator
     */
    private void publishFailedState(Exception ex) {
        synchronized (stateLock) {
            migrationState.setRunning(false);
            migrationState.setStopRequested(false);
            migrationState.setPaused(false);
            migrationState.setFinalizing(false);
            migrationState.setError(ex.getMessage());
            stopRequested = false;
        }
    }

    /**
     * Creates a field-for-field copy so REST clients and workers never mutate shared state directly.
     *
     * @param source state to copy
     * @return independent state containing the same progress and lifecycle values
     */
    private State copyState(State source) {
        State copy = new State();
        copy.setTableIndex(source.getTableIndex());
        copy.setTotalTables(source.getTotalTables());
        copy.setCursor(source.getCursor());
        copy.setTableMaxId(source.getTableMaxId());
        copy.setScanned(source.getScanned());
        copy.setUpdated(source.getUpdated());
        copy.setTableUpdated(source.getTableUpdated());
        copy.setTableDurationMillis(source.getTableDurationMillis());
        copy.setRunning(source.isRunning());
        copy.setStopRequested(source.isStopRequested());
        copy.setPaused(source.isPaused());
        copy.setDone(source.isDone());
        copy.setFinalizing(source.isFinalizing());
        copy.setFinalized(source.isFinalized());
        copy.setDeletedStatKeys(source.getDeletedStatKeys());
        copy.setRetainedStatKeys(source.getRetainedStatKeys());
        copy.setUpdatedStatErrors(source.getUpdatedStatErrors());
        copy.setTable(source.getTable());
        copy.setError(source.getError());
        return copy;
    }

    /**
     * Writes an audit entry for a fully processed statistics table.
     *
     * @param table physical table that completed migration
     * @param convertedRecords number of rows changed in that table
     * @param durationMillis cumulative processing time for the table in milliseconds
     */
    void auditCompletedTable(String table, long convertedRecords, long durationMillis) {
        Adminlog.add(
            Adminlog.TYPE_UPDATEDB,
            "Browser identifier migration completed: table=" + table +
                ", convertedRecords=" + convertedRecords +
                ", duration=" + formatDuration(durationMillis),
            -1,
            -1
        );
    }

    /**
     * Writes an audit entry summarizing identifier cleanup.
     *
     * @param deletedSeoBots number of obsolete {@code seo_bots} rows removed
     * @param result statistics-key deletion and retention counts
     */
    void auditFinalization(int deletedSeoBots, FinalizationResult result) {
        Adminlog.add(
            Adminlog.TYPE_UPDATEDB,
            "Browser identifier migration finalized: deletedSeoBots=" + deletedSeoBots +
                ", deletedStatKeys=" + result.deletedStatKeys +
                ", retainedStatKeys=" + result.retainedStatKeys,
            -1,
            -1
        );
    }

    /**
     * Formats an elapsed duration for audit output.
     *
     * @param durationMillis elapsed time in milliseconds
     * @return duration formatted as {@code HH:mm:ss.SSS}
     */
    static String formatDuration(long durationMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        long millis = durationMillis % 1_000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    /** Stops the worker and interrupts any active background migration during bean destruction. */
    @Override
    public void destroy() {
        stopRequested = true;
        executor.shutdownNow();
    }

    /** Persists the migration update marker once, leaving an existing marker unchanged. */
    void markAsCompleted() {
        if (UpdateDatabase.isAllreadyUpdated(UPDATE_NOTE) == false) {
            UpdateDatabase.saveSuccessUpdate(UPDATE_NOTE);
        }
    }

    /**
     * Converts a versioned identifier to its whitespace-normalized browser-family name.
     *
     * <p>Empty values and identifiers containing only a version become {@code Unknown}. A trailing
     * numeric version is removed, while already canonical identifiers remain unchanged.</p>
     *
     * @param value stored browser identifier; may be {@code null} or empty
     * @return canonical browser-family identifier
     */
    static String normalizeBrowserIdentifier(String value) {
        if (Tools.isEmpty(value)) return "Unknown";
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (VERSION_ONLY.matcher(normalized).matches()) return "Unknown";
        normalized = VERSION_SUFFIX.matcher(normalized).replaceFirst("").trim();
        return Tools.isEmpty(normalized) ? "Unknown" : normalized;
    }

    /**
     * Groups {@code seo_bots} rows by normalized name and maps duplicates to one canonical row.
     *
     * <p>Rows are loaded by ascending ID. An existing row whose name already equals the canonical
     * value is preferred as the target; otherwise the oldest row becomes the target and is renamed
     * only in preparation mode.</p>
     *
     * @param connection connection used to load and optionally rename browser rows
     * @param prepare whether missing canonical names may be written to the database
     * @return mappings for every duplicate row except the selected canonical targets
     * @throws SQLException if browser rows cannot be loaded or a target cannot be renamed
     */
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
                .findFirst().orElse(group.get(0));
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

    /**
     * Maps versioned browser entries in the shared {@code stat_keys} table to canonical entries.
     *
     * <p>Values containing only a version are deliberately excluded because {@code stat_keys}
     * stores non-browser dimensions as well. Preparation mode creates missing canonical targets,
     * validates every target ID, and reports whether the statistics cache must be refreshed.</p>
     *
     * @param connection transactional connection used to inspect and optionally extend stat keys
     * @param prepare whether missing canonical target rows may be created
     * @return mappings and an indication that newly inserted targets require a cache refresh
     * @throws SQLException if stat keys cannot be loaded, created, or validated
     */
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

    /**
     * Resolves a canonical stat key or inserts it with a newly allocated application ID.
     *
     * <p>The database is checked again before insertion to account for another cluster node. The
     * allocated ID is also checked explicitly because legacy schemas do not guarantee uniqueness
     * of {@code stat_keys_id}.</p>
     *
     * @param connection transactional connection used for lookup and insertion
     * @param target canonical stat-key value
     * @return existing or newly allocated positive stat-key ID
     * @throws SQLException if lookup, ID allocation validation, or insertion fails
     */
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

        return allocatedId;
    }

    /**
     * Finds the unique stat-key row for a canonical value.
     *
     * @param connection connection used for the lookup
     * @param target canonical value to find
     * @return matching positive ID, or {@code 0} when no row exists
     * @throws SQLException if the query returns duplicate, mismatched, or invalid rows
     */
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

    /**
     * Ensures a newly allocated ID is not already present in a legacy {@code stat_keys} table.
     *
     * @param connection connection used for the lookup
     * @param targetId allocated ID that must be unused
     * @throws SQLException if the lookup fails or the ID already exists
     */
    private void verifyStatKeyIdIsAvailable(Connection connection, long targetId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM stat_keys WHERE stat_keys_id=?")) {
            ps.setLong(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Allocated stat_keys ID already exists: " + targetId);
            }
        }
    }

    /**
     * Verifies that every mapping points to exactly one row with the expected canonical value.
     *
     * <p>The preliminary in-memory check also rejects one target ID associated with multiple
     * canonical values before any statistics table is rewritten.</p>
     *
     * @param connection connection used to validate target rows
     * @param mappings source-to-target mappings to validate
     * @throws SQLException if a target is invalid, ambiguous, missing, or has an unexpected value
     */
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

    /** Reloads the local stat-key cache and requests the same refresh on other cluster nodes. */
    private void refreshStatKeyCache() {
        StatDB.getInstance(true);
        ClusterDB.addRefresh(StatDB.class);
    }

    /**
     * Refreshes caches when targets were inserted or are absent from the current local cache.
     *
     * @param result prepared mappings and their cache-refresh requirement
     */
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

    /**
     * Migrates one cursor-bounded batch from a regular statistics table.
     *
     * <p>At most {@link #ROW_BATCH_SIZE} rows are scanned. Changed browser IDs and optional
     * browser-UA IDs are queued in JDBC batches while SQL {@code NULL} values are preserved.</p>
     *
     * @param connection transactional connection used to scan and update rows
     * @param table validated regular statistics-table definition
     * @param state detached progress state providing the cursor and maximum row ID
     * @param botIds source-to-target mappings for {@code browser_id}
     * @param keyIds source-to-target mappings for {@code browser_ua_id}
     * @return ID of the last scanned row, or {@code 0} when the cursor found no rows
     * @throws SQLException if rows cannot be read or a JDBC update batch fails
     */
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

    /**
     * Merges browser counters into canonical rows and removes their obsolete source rows.
     *
     * <p>For each target, visit counts from the target and all sources are summed and the latest
     * non-null visit timestamp is retained. The unique name index is created only after source
     * rows have been deleted.</p>
     *
     * @param connection transactional connection used for aggregation, deletion, and index DDL
     * @param mappings duplicate browser rows that will be grouped by canonical target ID
     * @throws SQLException if counters cannot be read or written, rows cannot be deleted, or the
     * unique index cannot be verified
     */
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

    /**
     * Finds obsolete stat keys that are no longer referenced by any statistics dimension.
     *
     * <p>Each table is scanned at most once, selecting distinct combinations of its available
     * key columns together. Used candidates are removed immediately, and remaining tables are
     * skipped when no deletion candidates remain. Progress is published before and after each
     * table; a zero table maximum denotes a scan of unknown duration.</p>
     *
     * @param connection connection used for read-only reference checks before the cleanup transaction
     * @param mappings obsolete identifiers considered for deletion
     * @return only mappings whose source IDs have no remaining statistics references
     * @throws SQLException if any reference check fails, preventing identifier deletion
     */
    List<Mapping> findUnusedStatKeys(Connection connection, List<Mapping> mappings) throws SQLException {
        Set<Long> unusedIds = new HashSet<>();
        for (Mapping mapping : mappings) unusedIds.add(mapping.sourceId);
        List<String> tables = unusedIds.isEmpty() ? List.of() : discoverTables(connection);
        State state = getStateSnapshot();
        state.setTableIndex(0);
        state.setTotalTables(tables.size() + 1);

        for (String table : tables) {
            if (unusedIds.isEmpty()) break;
            state.setTable(table);
            state.setCursor(0);
            state.setTableMaxId(0);
            publishRunningState(state);

            Set<String> columns = readTableColumns(connection, table);
            List<String> keyColumns = List.of("browser_ua_id", "platform_id", "subplatform_id").stream()
                .filter(columns::contains).toList();
            if (keyColumns.isEmpty() == false) {
                String sql = "SELECT DISTINCT " + String.join(", ", keyColumns) + " FROM " + table;
                try (PreparedStatement ps = connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        for (int column = 1; column <= keyColumns.size(); column++) {
                            long id = rs.getLong(column);
                            if (rs.wasNull() == false) unusedIds.remove(id);
                        }
                        state.setScanned(state.getScanned() + 1);
                        if (unusedIds.isEmpty()) break;
                    }
                }
            }
            state.setRetainedStatKeys(mappings.size() - unusedIds.size());
            state.setTableIndex(state.getTableIndex() + 1);
            state.setTableMaxId(1);
            state.setCursor(1);
            publishRunningState(state);
        }

        state.setTableIndex(tables.size());
        state.setTable("seo_bots / stat_keys");
        state.setCursor(0);
        state.setTableMaxId(1);
        publishRunningState(state);
        return mappings.stream().filter(mapping -> unusedIds.contains(mapping.sourceId)).toList();
    }

    /**
     * Deletes obsolete source rows from {@code stat_keys} in bounded JDBC batches.
     *
     * @param connection transactional connection used for deletion
     * @param mappings mappings whose source IDs were verified as unused by {@link #findUnusedStatKeys}
     * @return number of rows reported as deleted by the database driver
     * @throws SQLException if a delete batch fails
     */
    int deleteStatKeys(Connection connection, List<Mapping> mappings) throws SQLException {
        int deleted = 0;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM stat_keys WHERE stat_keys_id=?")) {
            int pending = 0;
            for (Mapping mapping : mappings) {
                ps.setLong(1, mapping.sourceId);
                ps.addBatch();
                pending++;
                if (pending == UPDATE_BATCH_SIZE) {
                    deleted += sumBatchResults(ps.executeBatch());
                    pending = 0;
                }
            }
            if (pending > 0) deleted += sumBatchResults(ps.executeBatch());
        }
        return deleted;
    }

    /**
     * Converts JDBC batch result codes into an affected-row count.
     *
     * <p>{@link Statement#SUCCESS_NO_INFO} counts as one successful deletion because each batch
     * entry contains one source ID. {@link Statement#EXECUTE_FAILED} aborts finalization.</p>
     *
     * @param results result codes returned by {@link PreparedStatement#executeBatch()}
     * @return number of successful or positively counted batch entries
     * @throws SQLException if any batch entry reports execution failure
     */
    private int sumBatchResults(int[] results) throws SQLException {
        int count = 0;
        for (int result : results) {
            if (result == Statement.EXECUTE_FAILED) throw new SQLException("A finalization database batch failed");
            if (result == Statement.SUCCESS_NO_INFO) count++;
            else if (result > 0) count += result;
        }
        return count;
    }

    /**
     * Ensures {@code seo_bots(name)} has an exact, unfiltered, single-column unique index.
     *
     * <p>Metadata is checked before and after DDL. The second check accepts an index created
     * concurrently by another node, while still propagating the original DDL failure when the
     * required index remains absent.</p>
     *
     * @param connection connection used for metadata inspection and index creation
     * @throws SQLException if the table is ambiguous or missing, metadata cannot be read, or the
     * required index cannot be created and verified
     */
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

    /**
     * Resolves a table in the current catalog and schema with driver-compatible fallbacks.
     *
     * @param connection connection that supplies the current catalog and schema
     * @param metadata database metadata used for table discovery
     * @param expectedName case-insensitive table name to resolve
     * @return unambiguous physical table reference
     * @throws SQLException if metadata lookup fails or the table is missing or ambiguous
     */
    private TableReference findTable(Connection connection, DatabaseMetaData metadata, String expectedName) throws SQLException {
        String catalog = connection.getCatalog();
        String schema = getCurrentSchema(connection, metadata);

        TableReference table = findTable(metadata, catalog, schema, expectedName);
        if (table == null && schema != null) table = findTable(metadata, catalog, null, expectedName);
        if (table == null && catalog != null) table = findTable(metadata, null, null, expectedName);
        if (table == null) throw new SQLException("Table not found in database metadata: " + expectedName);
        return table;
    }

    /**
     * Searches one metadata scope for a case-insensitive table-name match.
     *
     * @param metadata database metadata used for discovery
     * @param catalog catalog to search, or {@code null} for an unrestricted catalog
     * @param schema schema to search, or {@code null} for an unrestricted schema
     * @param expectedName case-insensitive table name to resolve
     * @return matching table reference, or {@code null} when the scope contains no match
     * @throws SQLException if metadata lookup fails or multiple physical tables match
     */
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

    /**
     * Reads the active schema when supported by the JDBC driver.
     *
     * <p>jTDS and drivers that throw {@link SQLFeatureNotSupportedException} are treated as having
     * no usable schema so metadata discovery can fall back to a broader scope.</p>
     *
     * @param connection connection whose active schema is requested
     * @param metadata metadata used to identify drivers with unsupported schema access
     * @return current schema, or {@code null} when schema lookup is unsupported
     * @throws SQLException if driver metadata or schema lookup fails for another reason
     */
    private String getCurrentSchema(Connection connection, DatabaseMetaData metadata) throws SQLException {
        String driverName = metadata.getDriverName();
        if (driverName != null && driverName.toLowerCase(Locale.ROOT).contains("jtds")) return null;
        try {
            return connection.getSchema();
        } catch (SQLFeatureNotSupportedException ignored) {
            return null;
        }
    }

    /**
     * Checks whether a table has an unfiltered unique index containing only one expected column.
     *
     * <p>Index metadata rows are grouped by qualifier and name, sorted by ordinal position, and
     * statistics or nullable uniqueness metadata are ignored.</p>
     *
     * @param metadata database metadata used to inspect indexes
     * @param table resolved physical table reference
     * @param expectedColumn case-insensitive column name required as the sole index column
     * @return {@code true} only for an exact non-filtered unique index
     * @throws SQLException if index metadata cannot be read
     */
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

    /**
     * Builds migration definitions for all discovered statistics tables with supported columns.
     *
     * <p>Regular tables require {@code browser_id} and use a name-derived row-ID column.
     * {@code stat_error} tables are included only when they contain {@code browser_ua_id} and are
     * marked for mapping-based processing because they have no migration cursor column.</p>
     *
     * @param connection connection used for table and column discovery
     * @return ordered definitions for tables that can be migrated
     * @throws SQLException if table or column metadata cannot be inspected
     */
    List<TableDefinition> tableDefinitions(Connection connection) throws SQLException {
        List<TableDefinition> result = new ArrayList<>();
        for (String table : discoverTables(connection)) {
            Set<String> columns = readTableColumns(connection, table);
            String lower = table.toLowerCase(Locale.ROOT);
            if (lower.startsWith("stat_error")) {
                if (columns.contains("browser_ua_id")) result.add(new TableDefinition(table, null, true, true));
                continue;
            }
            if (columns.contains("browser_id") == false) continue;
            String id = lower.startsWith("stat_views") ? "view_id" : lower.startsWith("stat_from") ? "from_id" : "click_id";
            result.add(new TableDefinition(table, id, columns.contains("browser_ua_id"), false));
        }
        return result;
    }

    /**
     * Discovers allowlisted current and monthly statistics tables in the active metadata scope.
     *
     * @param connection connection used for metadata lookup
     * @return case-insensitively sorted physical table names
     * @throws SQLException if catalog, schema, or table metadata cannot be read
     */
    private List<String> discoverTables(Connection connection) throws SQLException {
        List<String> result = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = getCurrentSchema(connection, metadata);
        try (ResultSet rs = metadata.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                String lower = table.toLowerCase(Locale.ROOT);
                if (lower.matches("stat_views(_\\d{4}_\\d{1,2})?") || lower.matches("stat_from(_\\d{4}_\\d{1,2})?") ||
                    lower.matches("stat_error(_\\d{4}_\\d{1,2})?") || lower.equals("emails_stat_click")) result.add(table);
            }
        }
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    /**
     * Reads normalized column names from a zero-row query against an allowlisted table.
     *
     * @param connection connection used to inspect result-set metadata
     * @param table validated physical table name
     * @return lowercase column names exposed by the table
     * @throws SQLException if the metadata query cannot be executed or inspected
     */
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

    /**
     * Captures the highest row ID that belongs to the current migration run for one table.
     *
     * @param connection connection used for the aggregate query
     * @param table regular statistics-table definition with a cursor column
     * @return maximum existing ID, or {@code 0} when the table has no rows
     * @throws SQLException if the aggregate query fails
     */
    private long maxId(Connection connection, TableDefinition table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT MAX(" + table.idColumn + ") FROM " + table.name)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /**
     * Converts detailed mappings to the lookup form used while scanning statistics rows.
     *
     * @param mappings detailed source-to-target mappings
     * @return map keyed by source ID with canonical target IDs as values
     */
    private Map<Long, Long> toIdMap(List<Mapping> mappings) {
        Map<Long, Long> result = new HashMap<>();
        for (Mapping mapping : mappings) result.put(mapping.sourceId, mapping.targetId);
        return result;
    }

    /** Represents one persisted {@code seo_bots} row during mapping analysis. */
    private record BrowserRow(long id, String name) {}

    /** Represents one persisted {@code stat_keys} row during mapping analysis. */
    private record StatKeyRow(long id, String value) {}

    /** Holds the stable mappings and table order reused while a migration is paused and resumed. */
    private record MigrationPlan(List<Mapping> keyMappings, Map<Long, Long> botIds, Map<Long, Long> keyIds,
                                 List<TableDefinition> tables) {}

    /** Describes how one discovered statistics table must be migrated. */
    record TableDefinition(String name, String idColumn, boolean browserKey, boolean statError) {
        TableDefinition(String name, String idColumn, boolean browserKey) {
            this(name, idColumn, browserKey, false);
        }
    }

    /** Identifies a physical table within JDBC catalog and schema metadata. */
    private record TableReference(String catalog, String schema, String name) {}

    /** Groups the metadata rows that belong to one database index. */
    private record IndexReference(String qualifier, String name) {}

    /** Captures the index-column metadata needed to recognize an exact uniqueness constraint. */
    private record IndexColumn(int position, String name, String filterCondition) {}

    /** Returns prepared stat-key mappings together with their cache-refresh requirement. */
    record StatKeyMappingResult(List<Mapping> mappings, boolean cacheRefreshRequired) {}
}
