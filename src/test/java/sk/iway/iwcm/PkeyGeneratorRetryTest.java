package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.database.ComplexQuery;

class PkeyGeneratorRetryTest {

    @Test
    void failedKeyCreationDoesNotRetryIndefinitely() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
        when(statement.execute()).thenThrow(new SQLException("Key insertion failed"));
        AtomicInteger insertAttempts = new AtomicInteger();
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("INSERT") && insertAttempts.incrementAndGet() > 1) {
                throw new AssertionError("Repeated creation of the same missing key");
            }
            return statement;
        });

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
            database.when(DBPool::getConnection).thenReturn(connection);
            database.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);

            createGenerator();
            assertThrows(IllegalStateException.class, () -> PkeyGenerator.getNextValueAsLong("missing_key"));
            assertEquals(1, insertAttempts.get());
        }
    }

    @Test
    void newKeyIsLoadedThenUsesItsAllocatedRange() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        PkeyBean bean = new PkeyBean();
        bean.setName("new_key");
        bean.setValue(10);
        bean.setMaxValue(100);
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class);
                MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class, (query, context) -> {
                    when(query.setSql(anyString())).thenReturn(query);
                    when(query.list(any())).thenReturn(context.getCount() == 1 ? List.of() : List.of(bean));
                })) {
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
            constants.when(() -> Constants.getInt("pkeyGenIncrement")).thenReturn(2);
            constants.when(() -> Constants.getInt("pkeyGenOffset")).thenReturn(1);
            database.when(DBPool::getConnection).thenReturn(connection);
            createGenerator();

            assertEquals(13, PkeyGenerator.getNextValueAsLong(bean.getName()));
            assertEquals(15, PkeyGenerator.getNextValueAsLong(bean.getName()));
            assertEquals(2, queries.constructed().size());
            verify(statement).execute();
        }
    }

    private PkeyGenerator createGenerator() throws Exception {
        Constructor<PkeyGenerator> constructor = PkeyGenerator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
