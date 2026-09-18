package sk.iway.iwcm.rag.pgvector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.data.jpa.repository.config.JpaMetamodelMappingContextFactoryBean;
import org.springframework.data.jpa.repository.support.DefaultJpaContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.jpa.JpaTools;

/** Verifies that optional RAG persistence does not participate in the CMS startup scans. */
class PgvectorSpringConfigTest {

    @Test
    void offlineRagDatabaseDoesNotPreventMainJpaStartup() throws Exception {
        try (MockedStatic<DBPool> pools = mockStatic(DBPool.class);
             MockedStatic<Constants> constants = mockStatic(Constants.class);
             AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            constants.when(() -> Constants.getBoolean("ragSemanticSearchEnabled")).thenReturn(true);
            DataSource offline = mock(DataSource.class);
            when(offline.getConnection()).thenThrow(new SQLException("RAG database is offline"));
            DBPool pool = mock(DBPool.class);
            pools.when(DBPool::getInstance).thenReturn(pool);
            pools.when(() -> DBPool.isDataSourceConfigured("rag_jpa")).thenReturn(true);
            when(pool.getDataSource("rag_jpa")).thenReturn(offline);

            Metamodel metamodel = mock(Metamodel.class);
            ManagedType<?> managedType = mock(ManagedType.class);
            when(managedType.getJavaType()).thenAnswer(invocation -> String.class);
            when(metamodel.getManagedTypes()).thenReturn(Set.of(managedType));
            JpaEntityManager primary = mock(JpaEntityManager.class);
            when(primary.getMetamodel()).thenReturn(metamodel);
            EntityManagerFactory primaryFactory = mock(EntityManagerFactory.class);
            when(primaryFactory.getMetamodel()).thenReturn(metamodel);
            context.registerBean("primaryEntityManager", JpaEntityManager.class, () -> primary);
            context.registerBean("primaryEntityManagerFactory", EntityManagerFactory.class, () -> primaryFactory);
            context.registerBean("jpaContext", DefaultJpaContext.class);
            context.registerBean("jpaMappingContext", JpaMetamodelMappingContextFactoryBean.class);
            context.scan("sk.iway.iwcm.rag.pgvector");
            context.register(JpaTools.class);

            assertDoesNotThrow(context::refresh);
            assertSame(primary, context.getBean(JpaContext.class).getEntityManagerByManagedType(String.class));
            EmbeddingChunkRepository repository = context.getBean(EmbeddingChunkRepository.class);
            verifyNoInteractions(offline);

            RuntimeException failure = assertThrows(RuntimeException.class, repository::count);
            assertInstanceOf(DataAccessResourceFailureException.class, NestedExceptionUtils.getMostSpecificCause(failure));
            assertThrows(RuntimeException.class, repository::count);
            verify(offline, times(2)).getConnection();
            assertTrue(context.isActive());
            assertSame(primary, context.getBean(JpaContext.class).getEntityManagerByManagedType(String.class));
        }
    }
}
