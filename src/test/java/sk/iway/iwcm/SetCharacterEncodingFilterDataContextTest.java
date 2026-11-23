package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class SetCharacterEncodingFilterDataContextTest {

    private RequestBean originalBean;
    private ThreadLocal<Map<EntityManagerFactory, EntityManager>> managers;
    private Map<EntityManagerFactory, EntityManager> originalManagers;
    private MockedStatic<DBPool> database;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void isolateThreadContext() {
        originalBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        managers = (ThreadLocal<Map<EntityManagerFactory, EntityManager>>) ReflectionTestUtils.getField(
                SetCharacterEncodingFilter.class, "threadEntityManagers");
        originalManagers = managers.get();
        managers.remove();
        SetCharacterEncodingFilter.setCurrentRequestBean(null);
        database = mockStatic(DBPool.class);
    }

    @AfterEach
    void restoreThreadContext() {
        database.close();
        if (originalManagers == null) managers.remove();
        else managers.set(originalManagers);
        SetCharacterEncodingFilter.setCurrentRequestBean(originalBean);
    }

    @Test
    void nestedContextsRestoreTheirCallerAndCloseOnlyTheirOwnManagers() {
        EntityManager outerManager = openManager();
        EntityManager innerManager = openManager();
        EntityManagerFactory factory = registerManager("iwcm", outerManager);
        when(factory.createEntityManager()).thenReturn(outerManager, innerManager);

        try (var outer = SetCharacterEncodingFilter.openIsolatedDataContext(null)) {
            RequestBean outerBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            assertSame(outerManager, SetCharacterEncodingFilter.getEntityManager());

            try (var inner = SetCharacterEncodingFilter.openIsolatedDataContext(null)) {
                assertNotSame(outerBean, SetCharacterEncodingFilter.getCurrentRequestBean());
                assertSame(innerManager, SetCharacterEncodingFilter.getEntityManager());
            }

            verify(innerManager).close();
            verify(outerManager, never()).close();
            assertSame(outerBean, SetCharacterEncodingFilter.getCurrentRequestBean());
            assertSame(outerManager, SetCharacterEncodingFilter.getEntityManager());
        }

        verify(outerManager).close();
        assertNull(managers.get());
        assertNull(SetCharacterEncodingFilter.getCurrentRequestBean());
    }

    /** Both unchecked failure types restore the caller without closing its entity manager. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void registrationFailureClosesNewManagersAndRestoresCallerContext(boolean failWithError) {
        RequestBean callerBean = new RequestBean();
        SetCharacterEncodingFilter.setCurrentRequestBean(callerBean);
        EntityManager callerManager = openManager();
        EntityManager renderingManager = openManager();
        EntityManagerFactory factory = registerManager("iwcm", callerManager);
        when(factory.createEntityManager()).thenReturn(callerManager, renderingManager);
        assertSame(callerManager, SetCharacterEncodingFilter.getEntityManager());

        HttpServletRequest request = mock(HttpServletRequest.class);
        Throwable failure = failWithError ? new AssertionError("Request registration failed")
                : new IllegalStateException("Request registration failed");
        when(request.getSession()).thenAnswer(invocation -> {
            assertSame(renderingManager, SetCharacterEncodingFilter.getEntityManager());
            throw failure;
        });

        assertSame(failure, assertThrows(failure.getClass(),
                () -> SetCharacterEncodingFilter.openIsolatedDataContext(request)));

        verify(renderingManager).close();
        verify(callerManager, never()).close();
        assertSame(callerBean, SetCharacterEncodingFilter.getCurrentRequestBean());
        assertSame(callerManager, SetCharacterEncodingFilter.getEntityManager());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupFailureStillClosesRemainingManagersAndRestoresCaller(boolean failRollback) {
        RequestBean callerBean = new RequestBean();
        SetCharacterEncodingFilter.setCurrentRequestBean(callerBean);
        EntityManager callerManager = openManager();
        EntityManagerFactory callerFactory = registerManager("caller", callerManager);
        Map<EntityManagerFactory, EntityManager> callerManagers = Map.of(callerFactory, callerManager);
        managers.set(callerManagers);

        EntityManager failingManager = openManager();
        EntityManager remainingManager = openManager();
        EntityManager closedManager = mock(EntityManager.class);
        EntityManagerFactory failingFactory = registerManager("failing", failingManager);
        EntityManagerFactory remainingFactory = registerManager("remaining", remainingManager);
        EntityManagerFactory closedFactory = registerManager("closed", closedManager);
        IllegalStateException failure = new IllegalStateException("Persistence cleanup failed");
        if (failRollback) {
            EntityTransaction transaction = failingManager.getTransaction();
            when(transaction.isActive()).thenReturn(true);
            doThrow(failure).when(transaction).rollback();
        } else {
            doThrow(failure).when(failingManager).close();
        }

        assertSame(failure, assertThrows(IllegalStateException.class, () -> {
            try (var context = SetCharacterEncodingFilter.openIsolatedDataContext(null)) {
                assertSame(failingManager, SetCharacterEncodingFilter.getEntityManager("failing"));
                assertSame(remainingManager, SetCharacterEncodingFilter.getEntityManager("remaining"));
                assertSame(closedManager, SetCharacterEncodingFilter.getEntityManager("closed"));

                // Make cleanup order deterministic: the failure must precede the healthy manager.
                Map<EntityManagerFactory, EntityManager> orderedManagers = new LinkedHashMap<>();
                orderedManagers.put(failingFactory, failingManager);
                orderedManagers.put(remainingFactory, remainingManager);
                orderedManagers.put(closedFactory, closedManager);
                managers.set(orderedManagers);
            }
        }));

        verify(failingManager).close();
        verify(remainingManager).close();
        verify(closedManager, never()).getTransaction();
        verify(closedManager, never()).close();
        verify(callerManager, never()).close();
        assertSame(callerBean, SetCharacterEncodingFilter.getCurrentRequestBean());
        assertSame(callerManagers, managers.get());
    }

    private EntityManager openManager() {
        EntityManager manager = mock(EntityManager.class);
        when(manager.isOpen()).thenReturn(true);
        when(manager.getTransaction()).thenReturn(mock(EntityTransaction.class));
        return manager;
    }

    private EntityManagerFactory registerManager(String dbName, EntityManager manager) {
        EntityManagerFactory factory = mock(EntityManagerFactory.class);
        when(factory.createEntityManager()).thenReturn(manager);
        database.when(() -> DBPool.getEntityManagerFactory(dbName)).thenReturn(factory);
        return factory;
    }
}
