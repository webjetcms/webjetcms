package sk.iway.iwcm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

class IwcmInputStreamRetryTest {

    private static final String PATH = "/files/retry-test.bin";

    @Test
    void unchangedStaleFileTableDoesNotCauseEndlessReloads() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
        AtomicInteger refreshes = new AtomicInteger();

        try (MockedStatic<IwcmFsDB> files = mockStatic(IwcmFsDB.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            configureFileTable(files, new HashMap<>(Map.of(PATH, 1)));
            database.when(DBPool::getConnection).thenReturn(connection);
            files.when(() -> IwcmFsDB.loadHashTables(true)).thenAnswer(invocation -> {
                if (refreshes.incrementAndGet() > 1) throw new AssertionError("Repeated stale file table refresh");
                return null;
            });

            try (IwcmInputStream stream = new IwcmInputStream(new File(PATH))) {
                assertThrows(FileNotFoundException.class, stream::read);
            }
            assertEquals(1, refreshes.get());
        }
    }

    @Test
    void successfulFileTableRefreshStillLoadsTheReplacementFile() throws Exception {
        Map<String, Integer> table = new HashMap<>(Map.of(PATH, 1));
        Connection connection = mock(Connection.class);
        PreparedStatement nodeQuery = mock(PreparedStatement.class);
        PreparedStatement existenceQuery = mock(PreparedStatement.class);
        PreparedStatement dataQuery = mock(PreparedStatement.class);
        ResultSet nodes = mock(ResultSet.class);
        when(nodes.next()).thenReturn(true, false);
        when(nodes.getInt("node_id")).thenReturn(10);
        when(nodeQuery.executeQuery()).thenReturn(mock(ResultSet.class), nodes);
        when(existenceQuery.executeQuery()).thenReturn(mock(ResultSet.class));
        ResultSet data = mock(ResultSet.class);
        when(data.next()).thenReturn(true);
        when(data.getBinaryStream("data")).thenReturn(new ByteArrayInputStream(new byte[] {65}));
        when(dataQuery.executeQuery()).thenReturn(data);
        when(connection.prepareStatement("select node_id from file_data where file_fat_id = ? order by node_id asc"))
                .thenReturn(nodeQuery);
        when(connection.prepareStatement("select file_fat_id from file_fat where file_fat_id = ?"))
                .thenReturn(existenceQuery);
        when(connection.prepareStatement("select data from file_data where node_id = ? ")).thenReturn(dataQuery);

        try (MockedStatic<IwcmFsDB> files = mockStatic(IwcmFsDB.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            configureFileTable(files, table);
            database.when(DBPool::getConnection).thenReturn(connection);
            files.when(() -> IwcmFsDB.loadHashTables(true)).thenAnswer(invocation -> {
                table.put(PATH, 2);
                return null;
            });

            try (IwcmInputStream stream = new IwcmInputStream(new File(PATH))) {
                assertEquals(65, stream.read());
            }
            files.verify(() -> IwcmFsDB.loadHashTables(true), times(1));
        }
    }

    private void configureFileTable(MockedStatic<IwcmFsDB> files, Map<String, Integer> table) {
        files.when(() -> IwcmFsDB.getVirtualPath(PATH)).thenReturn(PATH);
        files.when(() -> IwcmFsDB.useDBStorage(PATH)).thenReturn(true);
        files.when(IwcmFsDB::getBlockSize).thenReturn(1024);
        files.when(IwcmFsDB::getFatIdTable).thenReturn(table);
    }
}
