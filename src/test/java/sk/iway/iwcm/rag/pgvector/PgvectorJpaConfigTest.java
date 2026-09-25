package sk.iway.iwcm.rag.pgvector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import sk.iway.iwcm.DBPool;

class PgvectorJpaConfigTest {

    @Test
    void entityManagerFactoryUsesDedicatedPersistenceUnitWithRagDataSource() {
        DBPool dbPool = mock(DBPool.class);
        DataSource ragDataSource = mock(DataSource.class);

        try (MockedStatic<DBPool> dbPoolMock = mockStatic(DBPool.class)) {
            dbPoolMock.when(DBPool::getInstance).thenReturn(dbPool);
            when(dbPool.getDataSource("rag_jpa")).thenReturn(ragDataSource);

            LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new PgvectorJpaConfig().entityManagerFactory();

            assertEquals("webjet-rag", entityManagerFactory.getPersistenceUnitName());
            assertSame(ragDataSource, entityManagerFactory.getDataSource());
        }
    }

    @Test
    void entityManagerFactoryKeepsDedicatedPersistenceUnitWithIwcmFallback() {
        DBPool dbPool = mock(DBPool.class);
        DataSource iwcmDataSource = mock(DataSource.class);

        try (MockedStatic<DBPool> dbPoolMock = mockStatic(DBPool.class)) {
            dbPoolMock.when(DBPool::getInstance).thenReturn(dbPool);
            when(dbPool.getDataSource("rag_jpa")).thenReturn(null);
            when(dbPool.getDataSource("iwcm")).thenReturn(iwcmDataSource);

            LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new PgvectorJpaConfig().entityManagerFactory();

            assertEquals("webjet-rag", entityManagerFactory.getPersistenceUnitName());
            assertSame(iwcmDataSource, entityManagerFactory.getDataSource());
        }
    }
}
