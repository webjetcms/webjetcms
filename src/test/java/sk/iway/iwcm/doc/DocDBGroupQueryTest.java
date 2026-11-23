package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;

/** Verifies folder-query predicates for all supported databases without opening a database connection. */
@Execution(ExecutionMode.SAME_THREAD)
class DocDBGroupQueryTest {
    /** Including hidden pages must preserve the Oracle join and the folder restriction in every database. */
    @ParameterizedTest(name = "database={0}, onlyAvailable={1}")
    @CsvSource({
        Constants.DB_ORACLE + ", false, d.author_id=u.user_id(+) AND group_id=42",
        Constants.DB_ORACLE + ", true, d.author_id=u.user_id(+) AND available=1 AND group_id=42",
        Constants.DB_MYSQL + ", false, group_id=42",
        Constants.DB_MYSQL + ", true, available=1 AND group_id=42",
        Constants.DB_MSSQL + ", false, group_id=42",
        Constants.DB_MSSQL + ", true, available=1 AND group_id=42",
        Constants.DB_PGSQL + ", false, group_id=42",
        Constants.DB_PGSQL + ", true, available=true AND group_id=42"
    })
    void preservesJoinAndAvailabilityFilter(int databaseType, boolean onlyAvailable, String expectedWhere) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        DocDB documents = mock(DocDB.class, CALLS_REAL_METHODS);
        int originalDatabaseType = Constants.DB_TYPE;

        try (var database = mockStatic(DBPool.class)) {
            Constants.DB_TYPE = databaseType;
            database.when(DBPool::getConnectionReadUncommited).thenReturn(connection);

            documents.getDocByGroup(42, DocDB.ORDER_TITLE, true, -1, -1, true, onlyAvailable);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sql.capture());
            verify(statement).executeQuery();
            String query = sql.getValue();
            String where = query.substring(query.indexOf(" WHERE ") + 7, query.indexOf(" ORDER BY "));
            assertEquals(expectedWhere, where.trim().replaceAll("\\s+", " "));
        } finally {
            Constants.DB_TYPE = originalDatabaseType;
        }
    }
}
