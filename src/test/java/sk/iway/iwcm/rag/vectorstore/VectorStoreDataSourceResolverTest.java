package sk.iway.iwcm.rag.vectorstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;

class VectorStoreDataSourceResolverTest {

    @Test
    void explicitRagDataSourceTakesPrecedence() throws Exception {
        DataSource ragDataSource = dataSource("PostgreSQL", "16.3", 16, 3);
        DataSource primaryDataSource = dataSource("MariaDB", "11.8.9-MariaDB", 11, 8);

        Resolution resolution = VectorStoreDataSourceResolver.resolve(ragDataSource, primaryDataSource);

        assertEquals(VectorStoreDataSourceResolver.RAG_DATASOURCE_NAME, resolution.dataSourceName());
        assertEquals(VectorStoreType.POSTGRESQL, resolution.backend());
        assertTrue(resolution.explicit());
        verify(primaryDataSource, never()).getConnection();
    }

    @Test
    void detectsPostgreSqlOnPrimaryDataSource() throws Exception {
        Resolution resolution = VectorStoreDataSourceResolver.resolve(
            null,
            dataSource("PostgreSQL", "17.2", 17, 2)
        );

        assertEquals(VectorStoreDataSourceResolver.PRIMARY_DATASOURCE_NAME, resolution.dataSourceName());
        assertEquals(VectorStoreType.POSTGRESQL, resolution.backend());
        assertTrue(resolution.isSupported());
        assertFalse(resolution.explicit());
    }

    @Test
    void detectsMariaDbElevenEight() throws Exception {
        Resolution resolution = VectorStoreDataSourceResolver.resolve(
            null,
            dataSource("MariaDB", "11.8.9-MariaDB", 11, 8)
        );

        assertEquals(VectorStoreType.MARIADB, resolution.backend());
        assertTrue(resolution.isSupported());
        assertEquals("11.8.9-MariaDB", resolution.databaseProductVersion());
    }

    @Test
    void rejectsOlderMariaDb() throws Exception {
        Resolution resolution = VectorStoreDataSourceResolver.resolve(
            null,
            dataSource("MariaDB", "11.7.2-MariaDB", 11, 7)
        );

        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertFalse(resolution.isSupported());
        assertTrue(resolution.reason().contains("11.8 or newer"));
    }

    @Test
    void rejectsPlainMySql() throws Exception {
        Resolution resolution = VectorStoreDataSourceResolver.resolve(
            null,
            dataSource("MySQL", "8.4.0", 8, 4)
        );

        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertTrue(resolution.reason().contains("MySQL"));
    }

    @Test
    void inaccessibleExplicitDataSourceDoesNotFallBack() throws Exception {
        DataSource ragDataSource = mock(DataSource.class);
        when(ragDataSource.getConnection()).thenThrow(new SQLException("offline"));
        DataSource primaryDataSource = dataSource("PostgreSQL", "16.3", 16, 3);

        Resolution resolution = VectorStoreDataSourceResolver.resolve(ragDataSource, primaryDataSource);

        assertEquals(VectorStoreDataSourceResolver.RAG_DATASOURCE_NAME, resolution.dataSourceName());
        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertTrue(resolution.reason().contains("offline"));
        assertTrue(resolution.explicit());
        verify(primaryDataSource, never()).getConnection();
    }

    @Test
    void configuredDataSourceWhosePoolFailedDoesNotFallBack() throws Exception {
        DataSource primaryDataSource = dataSource("MariaDB", "11.8.9-MariaDB", 11, 8);

        Resolution resolution = VectorStoreDataSourceResolver.resolve(null, true, primaryDataSource);

        assertEquals(VectorStoreDataSourceResolver.RAG_DATASOURCE_NAME, resolution.dataSourceName());
        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertTrue(resolution.reason().contains("unavailable"));
        assertTrue(resolution.explicit());
        verify(primaryDataSource, never()).getConnection();
    }

    @Test
    void unsupportedExplicitDataSourceDoesNotFallBack() throws Exception {
        DataSource ragDataSource = dataSource("MySQL", "8.4.0", 8, 4);
        DataSource primaryDataSource = dataSource("PostgreSQL", "16.3", 16, 3);

        Resolution resolution = VectorStoreDataSourceResolver.resolve(ragDataSource, primaryDataSource);

        assertEquals(VectorStoreDataSourceResolver.RAG_DATASOURCE_NAME, resolution.dataSourceName());
        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertTrue(resolution.explicit());
        verify(primaryDataSource, never()).getConnection();
    }

    @Test
    void reportsMissingDataSources() {
        Resolution resolution = VectorStoreDataSourceResolver.resolve(null, null);

        assertNull(resolution.dataSourceName());
        assertEquals(VectorStoreType.UNSUPPORTED, resolution.backend());
        assertTrue(resolution.reason().contains("Neither"));
    }

    private static DataSource dataSource(String productName, String productVersion, int majorVersion, int minorVersion)
        throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn(productName);
        when(metadata.getDatabaseProductVersion()).thenReturn(productVersion);
        when(metadata.getDatabaseMajorVersion()).thenReturn(majorVersion);
        when(metadata.getDatabaseMinorVersion()).thenReturn(minorVersion);
        return dataSource;
    }
}
