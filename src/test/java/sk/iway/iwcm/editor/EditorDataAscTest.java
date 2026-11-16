package sk.iway.iwcm.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

class EditorDataAscTest {

    private RequestBean originalBean;

    @BeforeEach
    void saveContext() {
        originalBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        SetCharacterEncodingFilter.setCurrentRequestBean(null);
    }

    @AfterEach
    void restoreContext() {
        SetCharacterEncodingFilter.setCurrentRequestBean(originalBean);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesExistingContextWhetherOrNotItStoresTheRequest(boolean hasRequest) {
        HttpServletRequest request = null;
        if (hasRequest) request = new MockHttpServletRequest();
        HttpServletRequest expectedRequest = request;
        RequestBean bean = new RequestBean();
        bean.setRequest(request);
        bean.setDomain("example.com");
        bean.setUserId(42);
        SetCharacterEncodingFilter.setCurrentRequestBean(bean);
        EditorForm form = new EditorForm();

        try (MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class)) {
            editor.when(() -> EditorToolsForCore.getDataAsc(eq("source"), eq(form), eq(true), any()))
                    .thenAnswer(invocation -> {
                        HttpServletRequest actualRequest = invocation.getArgument(3);
                        if (hasRequest) assertSame(expectedRequest, actualRequest);
                        else {
                            assertNotNull(actualRequest);
                            assertNotNull(actualRequest.getSession());
                        }
                        assertSame(bean, SetCharacterEncodingFilter.getCurrentRequestBean());
                        return "prepared";
                    });

            assertEquals("prepared", EditorDB.getDataAsc("source", form, true));
            assertSame(bean, SetCharacterEncodingFilter.getCurrentRequestBean());
            assertEquals("example.com", bean.getDomain());
            assertEquals(42, bean.getUserId());
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
        "/products/detail.html,example.com,true,/products/detail.html,example.com",
        "'',example.com,true,/LuceneFulltextSearch,example.com",
        "NULL,example.com,true,/LuceneFulltextSearch,example.com",
        "'   ',NULL,true,/LuceneFulltextSearch,''",
        "/products/,NULL,false,/products/,''"
    }, nullValues = "NULL")
    void mockUsesPagePathAndGroupDomainWithFallbacks(String virtualPath, String domain, boolean groupExists,
            String expectedPath, String expectedDomain) {
        EditorForm form = new EditorForm();
        form.setVirtualPath(virtualPath);
        form.setGroupId(7);
        GroupDetails group = new GroupDetails();
        group.setDomainName(domain);
        GroupsDB groups = mock(GroupsDB.class);
        if (groupExists) when(groups.getGroup(7)).thenReturn(group);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<GroupsDB> groupInstances = mockStatic(GroupsDB.class);
                MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class)) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
            groupInstances.when(GroupsDB::getInstance).thenReturn(groups);
            documents.when(() -> DocDB.getDomain(any(HttpServletRequest.class)))
                    .thenAnswer(invocation -> ((HttpServletRequest) invocation.getArgument(0)).getServerName());
            editor.when(() -> EditorToolsForCore.getDataAsc(eq("source"), eq(form), eq(true), any()))
                    .thenAnswer(invocation -> {
                        HttpServletRequest request = invocation.getArgument(3);
                        assertEquals(expectedPath, request.getRequestURI());
                        assertEquals(expectedDomain, request.getServerName());
                        assertNotNull(request.getSession());
                        RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
                        assertEquals(expectedPath, bean.getUrl());
                        assertEquals(expectedDomain, bean.getDomain());
                        return "prepared";
                    });

            assertEquals("prepared", EditorDB.getDataAsc("source", form, true));
            verify(groups).getGroup(7);
            assertNull(SetCharacterEncodingFilter.getCurrentRequestBean());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void scopesMockContextWithoutClosingCallerEntityManagers(boolean fail) {
        EditorForm form = new EditorForm();
        EntityManager manager = mock(EntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);
        when(manager.isOpen()).thenReturn(true);
        when(manager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<EntityManagerFactory, EntityManager>> managers =
                (ThreadLocal<Map<EntityManagerFactory, EntityManager>>) ReflectionTestUtils.getField(
                        SetCharacterEncodingFilter.class, "threadEntityManagers");
        Map<EntityManagerFactory, EntityManager> originalManagers = managers.get();
        Map<EntityManagerFactory, EntityManager> callerManagers = Map.of(mock(EntityManagerFactory.class), manager);
        managers.set(callerManagers);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class)) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
            documents.when(() -> DocDB.getDomain(any(HttpServletRequest.class))).thenReturn("");
            editor.when(() -> EditorToolsForCore.getDataAsc(eq("source"), eq(form), eq(true), any()))
                    .thenAnswer(invocation -> {
                        HttpServletRequest request = invocation.getArgument(3);
                        assertNotNull(request);
                        assertNotNull(request.getSession());
                        assertEquals("", request.getServerName());
                        assertEquals("/LuceneFulltextSearch", request.getServletPath());
                        assertNotNull(SetCharacterEncodingFilter.getCurrentRequestBean());
                        if (fail) throw new IllegalStateException("Preparation failed");
                        return "prepared";
                    });

            if (fail) {
                assertThrows(IllegalStateException.class, () -> EditorDB.getDataAsc("source", form, true));
            } else {
                assertEquals("prepared", EditorDB.getDataAsc("source", form, true));
            }
            assertNull(SetCharacterEncodingFilter.getCurrentRequestBean());
            assertSame(callerManagers, managers.get());
            verify(manager, never()).close();
            verify(transaction, never()).rollback();
        } finally {
            if (originalManagers == null) managers.remove();
            else managers.set(originalManagers);
        }
    }
}
