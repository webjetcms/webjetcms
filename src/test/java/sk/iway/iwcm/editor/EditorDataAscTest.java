package sk.iway.iwcm.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.spring.webjet_component.WebjetComponentParser;

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
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void restoresExistingContextAfterSuccessOrFailure(boolean hasRequest, boolean fail) {
        HttpServletRequest request = null;
        if (hasRequest) request = new MockHttpServletRequest();
        HttpServletRequest expectedRequest = request;
        RequestBean bean = new RequestBean();
        bean.setRequest(request);
        bean.setDomain("tenant-a.example");
        bean.setUserId(42);
        SetCharacterEncodingFilter.setCurrentRequestBean(bean);
        EditorForm form = new EditorForm();
        form.setGroupId(7);
        form.setVirtualPath("/indexed-page.html");
        GroupDetails group = new GroupDetails();
        group.setDomainName("tenant-b.example");
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(7)).thenReturn(group);

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
                        HttpServletRequest actualRequest = invocation.getArgument(3);
                        if (hasRequest) {
                            assertSame(expectedRequest, actualRequest);
                            assertSame(bean, SetCharacterEncodingFilter.getCurrentRequestBean());
                        }
                        else {
                            assertNotNull(actualRequest);
                            assertNotNull(actualRequest.getSession());
                            assertEquals("tenant-b.example", actualRequest.getServerName());
                            assertNotSame(bean, SetCharacterEncodingFilter.getCurrentRequestBean());
                            assertEquals("tenant-b.example", CloudToolsForCore.getDomainName());
                        }
                        assertEquals("tenant-a.example", bean.getDomain());
                        assertEquals(42, bean.getUserId());
                        if (fail) throw new IllegalStateException("Preparation failed");
                        return "prepared";
                    });

            if (fail) {
                assertThrows(IllegalStateException.class, () -> EditorDB.getDataAsc("source", form, true));
            } else {
                assertEquals("prepared", EditorDB.getDataAsc("source", form, true));
            }
            assertSame(bean, SetCharacterEncodingFilter.getCurrentRequestBean());
            assertEquals("tenant-a.example", bean.getDomain());
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

    @Test
    void backgroundMockUsesServletContextDispatcherWithoutLoopback() throws Exception {
        Thread caller = Thread.currentThread();
        ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getRequestDispatcher("/components/_common/fulltext_preview.jsp")).thenReturn(dispatcher);
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            assertSame(caller, Thread.currentThread());
            assertFalse(request.isAsyncSupported());
            assertFalse(request.isAsyncStarted());
            assertSame(servletContext, ((HttpServletRequest) request).getSession().getServletContext());
            assertNull(new WebjetComponentParser().getRedirectLocation(response));
            response.getWriter().write("<p>Application rendered on the calling thread</p>");
            return null;
        }).when(dispatcher).include(any(), any());
        EditorForm form = new EditorForm();
        form.setVirtualPath("/background.html");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class, CALLS_REAL_METHODS);
                MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            constants.when(Constants::getServletContext).thenReturn(servletContext);
            documents.when(() -> DocDB.getDomain(any(HttpServletRequest.class))).thenReturn("");
            editor.when(() -> EditorToolsForCore.getDataAsc(eq("source"), eq(form), eq(true), any()))
                    .thenAnswer(invocation -> EditorToolsForCore.renderIncludes("source", false, invocation.getArgument(3)));
            tools.when(() -> Tools.downloadUrl(anyString())).thenThrow(new AssertionError("Loopback must not be used"));
            tools.clearInvocations();

            assertEquals("<p>Application rendered on the calling thread</p>", EditorDB.getDataAsc("source", form, true));
            tools.verify(() -> Tools.downloadUrl(anyString()), never());
            verify(dispatcher).include(any(), any());
            assertNull(SetCharacterEncodingFilter.getCurrentRequestBean());
        }
    }
}
