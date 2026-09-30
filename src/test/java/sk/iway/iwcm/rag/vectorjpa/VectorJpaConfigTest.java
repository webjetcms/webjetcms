package sk.iway.iwcm.rag.vectorjpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import sk.iway.iwcm.DBPool;

class VectorJpaConfigTest {

    @Test
    void entityManagerFactoryUsesDedicatedPersistenceUnitWithRagDataSource() throws SQLException {
        DBPool dbPool = mock(DBPool.class);
        DataSource ragDataSource = postgresqlDataSource();

        try (MockedStatic<DBPool> dbPoolMock = mockStatic(DBPool.class)) {
            dbPoolMock.when(DBPool::getInstance).thenReturn(dbPool);
            when(dbPool.getDataSource("rag_jpa")).thenReturn(ragDataSource);

            LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new VectorJpaConfig().entityManagerFactory();

            assertEquals("webjet-rag", entityManagerFactory.getPersistenceUnitName());
            assertSame(ragDataSource, entityManagerFactory.getDataSource());
        }
    }

    @Test
    void entityManagerFactoryKeepsDedicatedPersistenceUnitWithIwcmFallback() throws SQLException {
        DBPool dbPool = mock(DBPool.class);
        DataSource iwcmDataSource = postgresqlDataSource();

        try (MockedStatic<DBPool> dbPoolMock = mockStatic(DBPool.class)) {
            dbPoolMock.when(DBPool::getInstance).thenReturn(dbPool);
            when(dbPool.getDataSource("rag_jpa")).thenReturn(null);
            when(dbPool.getDataSource("iwcm")).thenReturn(iwcmDataSource);

            LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new VectorJpaConfig().entityManagerFactory();

            assertEquals("webjet-rag", entityManagerFactory.getPersistenceUnitName());
            assertSame(iwcmDataSource, entityManagerFactory.getDataSource());
        }
    }

    private DataSource postgresqlDataSource() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metadata.getDatabaseProductVersion()).thenReturn("17.2");
        return dataSource;
    }
}
