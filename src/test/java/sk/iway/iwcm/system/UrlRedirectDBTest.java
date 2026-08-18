package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.persistence.EntityTransaction;
import javax.servlet.ServletContext;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.jpa.JpaTools;

class UrlRedirectDBTest {

    @Test
    void getRedirectBeanShouldRespectVirtualPathLastSlash() {
        String oldUrl = "/unpublished-page";
        String domainName = "example.com";
        DocDB docDB = mock(DocDB.class);
        Cache cache = mock(Cache.class);
        ServletContext servletContext = mock(ServletContext.class);
        Map<String, Map<String, UrlRedirectBean>> cachedRedirects = Map.of("", Map.of());

        when(cache.getObject(anyString(), eq(Long.class))).thenReturn(Long.MAX_VALUE);
        when(servletContext.getAttribute(anyString())).thenReturn(cachedRedirects);
        when(docDB.getDocIdFromURLImpl(oldUrl + "/", domainName)).thenReturn(123);

        try (MockedStatic<Constants> constantsMock = mockStatic(Constants.class);
                MockedStatic<Cache> cacheMock = mockStatic(Cache.class);
                MockedStatic<DocDB> docDBMock = mockStatic(DocDB.class)) {
            constantsMock.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(false);
            constantsMock.when(() -> Constants.getBoolean("cacheUrlRedirects")).thenReturn(true);
            constantsMock.when(Constants::getServletContext).thenReturn(servletContext);
            cacheMock.when(Cache::getInstance).thenReturn(cache);
            docDBMock.when(DocDB::getInstance).thenReturn(docDB);

            constantsMock.when(() -> Constants.getBoolean("virtualPathLastSlash")).thenReturn(false);

            assertNull(UrlRedirectDB.getRedirectBean(oldUrl, domainName));
            verify(docDB, never()).getDocIdFromURLImpl(oldUrl + "/", domainName);

            constantsMock.when(() -> Constants.getBoolean("virtualPathLastSlash")).thenReturn(true);

            UrlRedirectBean redirect = UrlRedirectDB.getRedirectBean(oldUrl, domainName);

            assertNotNull(redirect);
            assertEquals(oldUrl, redirect.getOldUrl());
            assertEquals(oldUrl + "/", redirect.getNewUrl());
            assertEquals(302, redirect.getRedirectCode());
            assertEquals(domainName, redirect.getDomainName());
            verify(docDB).getDocIdFromURLImpl(oldUrl + "/", domainName);
        }
    }

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
