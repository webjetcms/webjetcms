package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;

/**
 * Verifies that deferred delivery reports the actual queue insertion result.
 */
class SendMailQueueTest {

    /**
     * Verifies that a failed queue insertion is not reported as successful delivery.
     *
     * @throws Exception if the mocked database interaction cannot be configured or closed
     */
    @Test
    void reportsQueueInsertionFailure() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.execute()).thenThrow(new SQLException("Queue insertion failed"));

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
             MockedStatic<DB> db = mockStatic(DB.class);
             MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
             MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            assertFalse(SendMail.sendLater(
                "Sender", "sender@example.com", "recipient@example.com", null, null, null,
                "Subject", "Message", "https://example.com", null, null, null));
        }
    }
}
