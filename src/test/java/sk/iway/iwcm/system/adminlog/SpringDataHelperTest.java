package sk.iway.iwcm.system.adminlog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifies audit reads without re-entering the EntityManager that is committing changes. */
class SpringDataHelperTest {

    @Test
    void readsOriginalStateThroughFreshEntityManager() {
        EntityManager committingManager = mock(EntityManager.class);
        EntityManagerFactory factory = mock(EntityManagerFactory.class);
        EntityManager reader = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        EntityType<?> entityType = mock(EntityType.class);
        Object current = new Object();
        Object original = new Object();
        when(committingManager.getEntityManagerFactory()).thenReturn(factory);
        when(factory.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of(entityType));
        when(entityType.getJavaType()).thenAnswer(invocation -> Object.class);
        when(factory.createEntityManager()).thenReturn(reader);
        when(reader.find(Object.class, 42L)).thenReturn(original);
        when(reader.isOpen()).thenReturn(true);
        SpringDataHelper helper = new SpringDataHelper();
        ReflectionTestUtils.setField(helper, "entityManagers", Map.of("cms", committingManager));

        assertSame(original, helper.getSpringDataEntity(current, 42L));

        verify(committingManager, never()).contains(current);
        verify(reader).close();
    }

    @Test
    void skipsPersistenceUnitsThatDoNotManageTheClass() {
        EntityManager committingManager = mock(EntityManager.class);
        EntityManagerFactory factory = mock(EntityManagerFactory.class);
        Metamodel metamodel = mock(Metamodel.class);
        when(committingManager.getEntityManagerFactory()).thenReturn(factory);
        when(factory.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of());
        SpringDataHelper helper = new SpringDataHelper();
        ReflectionTestUtils.setField(helper, "entityManagers", Map.of("other", committingManager));

        assertNull(helper.getSpringDataEntity(new Object(), 42L));

        verify(factory, never()).createEntityManager();
    }
}
