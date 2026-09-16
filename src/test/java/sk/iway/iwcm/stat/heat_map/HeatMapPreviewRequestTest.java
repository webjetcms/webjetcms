package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.users.UsersDB;

/** Verifies normalized preview parameters and separate requests initiated by preview JavaScript. */
class HeatMapPreviewRequestTest {

    /** Untrusted rendering and action parameters cannot replace the server-selected page. */
    @Test
    void exposesOnlyTheSelectedDocumentParameter() {
        MockHttpServletRequest original = new MockHttpServletRequest();
        original.addParameter("docid", "999");
        original.addParameter("historyid", "888");
        original.addParameter("doShowdocAction", "/editor.do");
        original.addParameter("NO_WJTOOLBAR", "false");
        HeatMapPreviewRequest request = new HeatMapPreviewRequest(original, 123);

        assertEquals("123", request.getParameter("docid"));
        assertArrayEquals(new String[] { "123" }, request.getParameterValues("docid"));
        assertEquals(List.of("docid"), Collections.list(request.getParameterNames()));
        assertEquals(1, request.getParameterMap().size());
        assertNull(request.getParameter("historyid"));
        assertNull(request.getParameterValues("doShowdocAction"));
        assertNull(request.getParameter("NO_WJTOOLBAR"));
        assertEquals("docid=123", request.getQueryString());
        assertEquals("999", original.getParameter("docid"));
    }

    /** A preview's trusted domain overrides another tab's editor domain without changing its session. */
    @Test
    void resolvesDomainFromThePreviewRequestWithoutChangingEditorSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("preview.editorDomainName", "editor.example");
        request.setSession(session);
        request.setAttribute("heatMapPreview", Boolean.TRUE);
        request.setAttribute("heatMapPreviewDomain", "public.example");

        assertEquals("public.example", DocDB.getDomain("admin.example", request));
        assertEquals("editor.example", session.getAttribute("preview.editorDomainName"));
    }

    /** An asynchronous statistics request is suppressed only for an authenticated statistics preview. */
    @Test
    void recognizesAuthenticatedPreviewReferrerIncludingContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/cms");
        request.addHeader("Referer", "https://admin.example/cms" + HeatMapPreviewRequest.PREVIEW_PATH + "?docId=123");
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_stat")).thenReturn(true);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class)) {
            tools.when(() -> Tools.getServerName(request, false)).thenReturn("admin.example");
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);

            assertTrue(HeatMapPreviewRequest.isPreviewReferrer(request));
            when(user.isEnabledItem("cmp_stat")).thenReturn(false);
            assertFalse(HeatMapPreviewRequest.isPreviewReferrer(request));
            when(user.isEnabledItem("cmp_stat")).thenReturn(true);
            when(user.isAdmin()).thenReturn(false);
            assertFalse(HeatMapPreviewRequest.isPreviewReferrer(request));
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(null);
            assertFalse(HeatMapPreviewRequest.isPreviewReferrer(request));
        }
    }

    /** Missing, malformed, foreign-host, and non-preview referrers remain ordinary statistics traffic. */
    @Test
    void ignoresOtherReferrers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertFalse(HeatMapPreviewRequest.isPreviewReferrer(request));
        try (MockedStatic<Tools> tools = mockStatic(Tools.class)) {
            tools.when(() -> Tools.getServerName(request, false)).thenReturn("admin.example");
            for (String referer : List.of("not a URI", "/admin/rest/stat/heat-map/preview",
                    "https://other.example" + HeatMapPreviewRequest.PREVIEW_PATH,
                    "https://admin.example/public-page.html")) {
                request.removeHeader("Referer");
                request.addHeader("Referer", referer);
                assertFalse(HeatMapPreviewRequest.isPreviewReferrer(request), referer);
            }
        }
    }
}
