package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.stat.StatNewDB;

class UsersDBSettingsRetryTest {

    @Test
    void stopsRetryingAfterSuccessfulRecovery() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
        AtomicInteger attempts = new AtomicInteger();

        try (MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<StatNewDB> statistics = mockStatic(StatNewDB.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            database.when(DBPool::getConnection).thenAnswer(invocation -> {
                int attempt = attempts.incrementAndGet();
                if (attempt == 1) throw new SQLException("Temporary database failure");
                if (attempt > 2) throw new AssertionError("Retried after a successful query");
                return connection;
            });
            statistics.when(() -> StatNewDB.prepareStatement(connection,
                    "SELECT * FROM user_settings WHERE user_id=?")).thenReturn(statement);

            assertTrue(UsersDB.getSettings(123, 3).isEmpty());
            database.verify(DBPool::getConnection, times(2));
        }
    }

    @Test
    void persistentFailureStopsAtAttemptLimit() {
        AtomicInteger attempts = new AtomicInteger();
        try (MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            database.when(DBPool::getConnection).thenAnswer(invocation -> {
                if (attempts.incrementAndGet() > 3) throw new AssertionError("Exceeded the retry limit");
                throw new SQLException("Persistent database failure");
            });

            assertTrue(UsersDB.getSettings(123, 3).isEmpty());
            database.verify(DBPool::getConnection, times(3));
        }
    }
}
