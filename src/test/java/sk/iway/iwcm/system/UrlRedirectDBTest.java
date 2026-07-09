package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityTransaction;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.system.jpa.JpaTools;

class UrlRedirectDBTest {

    @Test
    void saveShouldNotRollbackWhenTransactionIsNotActive() {
        UrlRedirectBean redirect = new UrlRedirectBean();
        JpaEntityManager entityManager = mock(JpaEntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);

        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);
        doThrow(new RuntimeException("Simulated persist failure")).when(entityManager).persist(any(UrlRedirectBean.class));

        try (MockedStatic<JpaTools> jpaToolsMock = mockStatic(JpaTools.class)) {
            jpaToolsMock.when(JpaTools::getEclipseLinkEntityManager).thenReturn(entityManager);

            assertDoesNotThrow(() -> UrlRedirectDB.save(redirect));
        }

        verify(transaction).begin();
        verify(transaction).isActive();
        verify(transaction, never()).rollback();
        verify(entityManager).close();
    }
}
