package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.PreviewSelection;

/** Verifies preview isolation from the page editor and untrusted history parameters. */
class HeatMapPreviewControllerTest {

    /** Preview content and domain come from validated services while editor session content survives. */
    @Test
    void preparesOnlyRequestScopedPreviewState() throws Exception {
        HeatMapHistoryService history = mock(HeatMapHistoryService.class);
        HeatMapAccess access = mock(HeatMapAccess.class);
        HeatMapPreviewController controller = new HeatMapPreviewController(history, access);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                assertEquals("/showdoc.do", path);
                return dispatcher;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        Object editorDraft = new Object();
        session.setAttribute("ShowdocAction.showDocData", editorDraft);
        session.setAttribute("previewDomain", "editor.example");
        request.setSession(session);
        request.addParameter("historyid", "999");
        request.addParameter("domain", "untrusted.example");
        request.addParameter("docid", "456");
        request.addParameter("inlineEditorAdmin", "true");
        request.addParameter("NO_WJTOOLBAR", "false");

        DocDetails document = mock(DocDetails.class);
        when(document.getVirtualPath()).thenReturn("/historical-page.html");
        when(document.getGroupId()).thenReturn(5);
        when(history.resolve(request, 123, "period")).thenReturn(
                new PreviewSelection(7, 100L, "history", false, false, document));
        when(access.currentDomain(request)).thenReturn("public.example");
        RequestBean requestBean = new RequestBean();

        try (MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
                MockedStatic<SetCharacterEncodingFilter> context = mockStatic(SetCharacterEncodingFilter.class)) {
            templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
            context.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);

            controller.preview(123, "period", request, response);
        }

        ArgumentCaptor<ServletRequest> forwarded = ArgumentCaptor.forClass(ServletRequest.class);
        verify(dispatcher).forward(forwarded.capture(), org.mockito.ArgumentMatchers.same(response));
        HttpServletRequest preview = (HttpServletRequest) forwarded.getValue();
        assertEquals("123", preview.getParameter("docid"));
        assertNull(preview.getParameter("historyid"));
        assertNull(preview.getParameter("inlineEditorAdmin"));
        assertNull(preview.getParameter("NO_WJTOOLBAR"));

        assertSame(document, request.getAttribute("ShowdocAction.showDocData"));
        assertEquals(Boolean.TRUE, request.getAttribute("heatMapPreview"));
        assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
        assertEquals(Boolean.TRUE, request.getAttribute("NO_WJTOOLBAR"));
        assertEquals("public.example", request.getAttribute("heatMapPreviewDomain"));
        assertEquals("/historical-page.html", request.getAttribute("path_filter_orig_path"));
        assertEquals("/historical-page.html", request.getAttribute("heatMapPreviewBasePath"));
        assertEquals("123", request.getAttribute("docid"));
        assertEquals(123, requestBean.getDocId());
        assertEquals(5, requestBean.getGroupId());
        assertEquals("public.example", requestBean.getDomain());
        assertEquals("docid=123", requestBean.getQueryString());
        assertSame(preview, requestBean.getRequest());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertSame(editorDraft, session.getAttribute("ShowdocAction.showDocData"));
        assertEquals("editor.example", session.getAttribute("previewDomain"));
        assertNull(session.getAttribute("NO_WJTOOLBAR"));
        verify(history).resolve(request, 123, "period");
    }

    /** A known unavailable revision returns a non-cacheable 404 without activating a preview. */
    @Test
    void refusesUnavailablePreviewWithoutCreatingSession() {
        HeatMapHistoryService history = mock(HeatMapHistoryService.class);
        HeatMapAccess access = mock(HeatMapAccess.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(history.resolve(request, 123, "period")).thenReturn(
                new PreviewSelection(7, 100L, "unavailable", false, false, null));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new HeatMapPreviewController(history, access).preview(123, "period", request, response));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertNull(request.getSession(false));
        assertFalse(Boolean.TRUE.equals(request.getAttribute("heatMapPreview")));
    }
}
