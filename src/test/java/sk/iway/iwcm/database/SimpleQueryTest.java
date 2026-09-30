package sk.iway.iwcm.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;

class SimpleQueryTest {

    @Test
    void executePropagatesDatabaseFailure() throws Exception {
        String sql = "UPDATE users SET first_name=? WHERE user_id=?";
        SQLException databaseFailure = new SQLException("database write failed");
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(sql)).thenThrow(databaseFailure);

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class)) {
            dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);

            IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> new SimpleQuery().execute(sql, "Fail", 123)
            );

            assertTrue(thrown.getCause() == databaseFailure);
            verify(connection).close();
        }
    }
}
