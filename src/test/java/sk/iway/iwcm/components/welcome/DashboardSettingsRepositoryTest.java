package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies atomic writes, owner scoping, and preservation of other-domain preferences. */
class DashboardSettingsRepositoryTest {
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement read = mock(PreparedStatement.class);
    private final PreparedStatement lock = mock(PreparedStatement.class);
    private final PreparedStatement delete = mock(PreparedStatement.class);
    private final PreparedStatement insert = mock(PreparedStatement.class);
    private final ResultSet records = mock(ResultSet.class);
    private final DashboardSettingsRepository repository = new DashboardSettingsRepository(() -> connection);

    @BeforeEach
    void prepareConnection() throws SQLException {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getAutoCommit()).thenReturn(true, false);
        when(connection.prepareStatement(startsWith("SELECT skey"))).thenReturn(read);
        when(connection.prepareStatement(startsWith("SELECT user_id"))).thenReturn(lock);
        when(connection.prepareStatement(startsWith("DELETE"))).thenReturn(delete);
        when(connection.prepareStatement(startsWith("INSERT"))).thenReturn(insert);
        when(read.executeQuery()).thenReturn(records);
        ResultSet owner = mock(ResultSet.class);
        when(owner.next()).thenReturn(true);
        when(lock.executeQuery()).thenReturn(owner);
    }

    @Test
    void commitsOnlyAfterAllRecordsAndPreservesRetainedInstancesOnOtherDomains() throws SQLException {
        when(records.next()).thenReturn(true, true, true, true, false);
        when(records.getString(1)).thenReturn("overview.layout.v1", "overview.widget.kept", "overview.domain.84.kept", "overview.domain.84.deleted");
        when(records.getString(2)).thenReturn("{}", "{}", "{}", "{}");

        repository.replace(7, "42", Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}"), Set.of("kept"));

        verify(lock).setInt(1, 7);
        verify(read).setInt(1, 7);
        verify(delete, times(3)).setInt(1, 7);
        verify(delete, never()).setString(2, "overview.domain.84.kept");
        verify(delete).setString(2, "overview.domain.84.deleted");
        verify(insert, times(2)).setInt(1, 7);
        var order = inOrder(connection, insert);
        order.verify(connection).setAutoCommit(false);
        order.verify(insert, times(2)).executeUpdate();
        order.verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void rollsBackWhenARecordCannotBeWritten() throws SQLException {
        when(insert.executeUpdate()).thenReturn(1).thenThrow(new SQLException("Write failed"));

        assertThrows(IllegalStateException.class, () -> repository.replace(7, "42",
            Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}"), Set.of("kept")));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void readsOnlyTheAuthenticatedOwnersDashboardNamespace() throws SQLException {
        repository.read(17);

        verify(read).setInt(1, 17);
        verify(read).setString(2, "overview.%");
        verifyNoInteractions(insert, delete);
        verify(connection, never()).setAutoCommit(false);
    }

    /** Prevents a locking SQL Server read from mixing records from concurrent saves. */
    @Test
    void sqlServerReadsUseTheSameOwnerLockAsWriters() throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn("Microsoft SQL Server");

        repository.read(17);

        var order = inOrder(connection, lock, read);
        order.verify(connection).setAutoCommit(false);
        order.verify(connection).prepareStatement("SELECT user_id FROM users WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=?");
        order.verify(lock).setInt(1, 17);
        order.verify(lock).executeQuery();
        order.verify(read).executeQuery();
        order.verify(connection).commit();
        order.verify(connection).setAutoCommit(true);
        verifyNoInteractions(insert, delete);
    }

    /** Releases the SQL Server read lock and connection state when a query fails. */
    @Test
    void sqlServerReadFailureRollsBackAndRestoresConnection() throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn("Microsoft SQL Server");
        when(read.executeQuery()).thenThrow(new SQLException("Read failed"));

        assertThrows(IllegalStateException.class, () -> repository.read(17));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
    }

    /** Keeps another domain's filter for an immediate undo, then prunes unclaimed orphan records. */
    @Test
    void preservesRemovedInstanceDomainOptionsForExactlyOneSuccessfulWrite() throws SQLException {
        Map<String, String> original = Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}",
            "overview.widget.removed", "{}", "overview.domain.84.removed", "{\"formName\":\"Contact\"}");
        Map<String, String> removed = Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}",
            "overview.domain.84.removed", "{\"formName\":\"Contact\"}");
        stubRecords(original);
        repository.replace(7, "42", Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}"), Set.of("kept"));
        verify(delete, never()).setString(2, "overview.domain.84.removed");

        clearInvocations(delete);
        stubRecords(removed);
        repository.replace(7, "42", Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}", "overview.widget.removed", "{}"), Set.of("kept", "removed"));
        verify(delete, never()).setString(2, "overview.domain.84.removed");

        clearInvocations(delete);
        stubRecords(removed);
        repository.replace(7, "42", Map.of("overview.layout.v1", "{}", "overview.widget.kept", "{}"), Set.of("kept"));
        verify(delete).setString(2, "overview.domain.84.removed");
    }

    private void stubRecords(Map<String, String> values) throws SQLException {
        var iterator = values.entrySet().iterator();
        AtomicReference<Map.Entry<String, String>> current = new AtomicReference<>();
        when(records.next()).thenAnswer(invocation -> {
            if (!iterator.hasNext()) return false;
            current.set(iterator.next());
            return true;
        });
        when(records.getString(1)).thenAnswer(invocation -> current.get().getKey());
        when(records.getString(2)).thenAnswer(invocation -> current.get().getValue());
    }

    @Test
    void rejectsNonTransactionalMysqlBeforeAnyWrite() throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn("MySQL");
        PreparedStatement engineQuery = mock(PreparedStatement.class);
        ResultSet engine = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT ENGINE"))).thenReturn(engineQuery);
        when(engineQuery.executeQuery()).thenReturn(engine);
        when(engine.next()).thenReturn(true);
        when(engine.getString(1)).thenReturn("MyISAM");

        assertThrows(IllegalStateException.class, () -> repository.replace(7, "42", Map.of("overview.layout.v1", "{}"), Set.of()));

        verifyNoInteractions(insert, delete, lock, read);
        verify(connection, never()).commit();
    }

    /** Clears every dashboard domain while preserving unrelated and legacy account settings. */
    @ParameterizedTest
    @ValueSource(strings = { "PostgreSQL", "Oracle", "Microsoft SQL Server" })
    void resetDeletesOnlyOwnedDashboardRecordsUnderTheSaveLock(String product) throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn(product);
        Set<String> removed = Set.of("overview.layout.v1", "overview.news", "overview.widget.form-1",
            "overview.domain.42.form-1", "overview.domain.84.form-1", "overview.domain.84.orphan");
        Map<String, String> original = new java.util.LinkedHashMap<>();
        removed.forEach(key -> original.put(key, "{corrupt"));
        original.put("overview.bookmarks", "[]");
        original.put("overview.newsletter", "{}");
        original.put("overview.layout.v2", "{}");
        original.put("overview.widgetSettings", "{}");
        original.put("datatable-state", "{}");
        stubRecords(original);

        repository.reset(7);

        verify(read).setInt(1, 7);
        verify(read).setString(2, "overview.%");
        verify(delete, times(removed.size())).setInt(1, 7);
        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delete, times(removed.size())).setString(eq(2), keys.capture());
        assertEquals(removed, Set.copyOf(keys.getAllValues()));
        String expectedLock = product.contains("SQL Server")
            ? "SELECT user_id FROM users WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=?"
            : "SELECT user_id FROM users WHERE user_id=? FOR UPDATE";
        var order = inOrder(connection, lock, read, delete);
        order.verify(connection).setAutoCommit(false);
        order.verify(connection).prepareStatement(expectedLock);
        order.verify(lock).setInt(1, 7);
        order.verify(lock).executeQuery();
        order.verify(read).executeQuery();
        order.verify(delete, times(removed.size())).executeUpdate();
        order.verify(connection).commit();
        order.verify(connection).setAutoCommit(true);
        verify(connection, never()).rollback();
        verifyNoInteractions(insert);
    }

    /** A partial delete failure must preserve the complete previous profile. */
    @Test
    void resetRollsBackWhenARecordCannotBeDeleted() throws SQLException {
        stubRecords(Map.of("overview.layout.v1", "{}", "overview.news", "{}"));
        when(delete.executeUpdate()).thenReturn(1).thenThrow(new SQLException("Delete failed"));

        assertThrows(IllegalStateException.class, () -> repository.reset(7));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
        verifyNoInteractions(insert);
    }

    /** Reset uses the same MySQL advisory lock and engine guard as a normal save. */
    @Test
    void mysqlResetLocksUntilCommitAndReleasesTheLock() throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn("MariaDB");
        PreparedStatement engineQuery = mock(PreparedStatement.class);
        PreparedStatement acquire = mock(PreparedStatement.class);
        PreparedStatement release = mock(PreparedStatement.class);
        ResultSet engine = mock(ResultSet.class);
        ResultSet acquired = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT ENGINE"))).thenReturn(engineQuery);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 10)")).thenReturn(acquire);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(release);
        when(engineQuery.executeQuery()).thenReturn(engine);
        when(engine.next()).thenReturn(true);
        when(engine.getString(1)).thenReturn("InnoDB");
        when(acquire.executeQuery()).thenReturn(acquired);
        when(acquired.next()).thenReturn(true);
        when(acquired.getInt(1)).thenReturn(1);
        when(release.executeQuery()).thenReturn(mock(ResultSet.class));
        stubRecords(Map.of("overview.layout.v1", "{}"));

        repository.reset(7);

        var order = inOrder(engineQuery, acquire, connection, delete, release);
        order.verify(engineQuery).executeQuery();
        order.verify(acquire).setString(1, "webjet-dashboard-7");
        order.verify(acquire).executeQuery();
        order.verify(connection).setAutoCommit(false);
        order.verify(delete).executeUpdate();
        order.verify(connection).commit();
        order.verify(release).setString(1, "webjet-dashboard-7");
        order.verify(release).executeQuery();
        order.verify(connection).setAutoCommit(true);
        verifyNoInteractions(lock, insert);

        clearInvocations(connection, acquire, delete, read, release);
        when(engine.getString(1)).thenReturn("MyISAM");
        assertThrows(IllegalStateException.class, () -> repository.reset(7));
        verifyNoInteractions(acquire, delete, read, release);
        verify(connection, never()).commit();
    }
}
