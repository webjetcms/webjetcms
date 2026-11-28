package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.tags.BuffTag;

/** ShowDoc rendering uses a fresh raw version and restores nested request context. */
class BranchPageRendererTest {
    @BeforeEach void requestBean() { SetCharacterEncodingFilter.setCurrentRequestBean(new RequestBean()); }
    @AfterEach void clean() { SetCharacterEncodingFilter.setCurrentRequestBean(null); }

    @Test void renderedTextDiffHandlesChangedLinksAndFormattingOnJakarta() throws Exception {
        String before = "<html><head></head><body><a href='/old'>same link</a><p>old text</p><p>same paragraph</p></body></html>";
        String after = "<html><head></head><body><a href='/new'>same link</a><p>new text</p><p><strong>same paragraph</strong></p></body></html>";
        String html = sk.iway.iwcm.common.DocTools.getHtmlDiff(after, before, false);
        assertTrue(html.contains("diff-html-added"));assertTrue(html.contains("diff-html-removed"));
        assertTrue(html.contains("same link"));assertTrue(html.contains("same paragraph"));
        assertFalse(html.contains("diff-html-changed"));
    }

    @ParameterizedTest
    @EnumSource(BranchDecision.Source.class)
    void rendersTheSelectedVersionWithItsTemplateAndRestoresRequest(BranchDecision.Source version) throws Exception {
        var dispatcher = mock(RequestDispatcher.class);
        var request = new MockHttpServletRequest() {
            @Override public RequestDispatcher getRequestDispatcher(String path) {
                assertEquals("/showdoc.do?docid=12&historyid=-1&NO_WJTOOLBAR=true", path);
                return dispatcher;
            }
        };
        var response = new MockHttpServletResponse();
        var live = new DocDetails();live.setDocId(12);live.setVirtualPath("/main/page.html");
        var work = new DocDetails();work.setDocId(12);work.setVirtualPath("/work/page.html");
        request.setAttribute("ShowdocAction.showDocData", "original");
        request.setAttribute(BuffTag.IS_BUFF_TAG, "original-buffer");
        request.setAttribute(BranchPreviewUrls.LOGICAL_PATH, "/original");
        var selected = new BranchContext(99, "example.sk", 7, BranchContext.Mode.ADMIN);
        try (var db = mockStatic(DocDB.class); var views = mockStatic(BranchView.class); var urls = mockStatic(BranchPreviewUrls.class);
                var ignored = BranchContexts.open(request, selected)) {
            var documents = mock(DocDB.class);db.when(DocDB::getInstance).thenReturn(documents);
            when(documents.getDoc(12, -1, false)).thenReturn(live);
            var view = mock(BranchView.class);views.when(BranchView::current).thenReturn(view);
            when(view.document(12, false)).thenReturn(work);
            urls.when(() -> BranchPreviewUrls.rewriteHtml(anyString(), same(request))).thenAnswer(call -> {
                assertEquals(BranchContext.Mode.PREVIEW, BranchContexts.current().mode());
                return call.getArgument(0);
            });
            doAnswer(call -> {
                assertSame(version == BranchDecision.Source.LIVE ? live : work, request.getAttribute("ShowdocAction.showDocData"));
                assertEquals(version == BranchDecision.Source.LIVE ? BranchContext.Mode.LIVE : BranchContext.Mode.ADMIN, BranchContexts.current().mode());
                assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
                ((HttpServletResponse)call.getArgument(1)).getWriter().write("<html><head><link id=branch-preview-styles rel=stylesheet href='/admin/v9/dist/css/branch-preview.css'><style>.page{color:red}</style><meta http-equiv=refresh content=0></head><body><aside id=branch-preview-status>banner</aside><main data-aos=fade-down>rendered include</main><noscript>enable scripts</noscript><form><button>submit</button></form></body></html>");
                return null;
            }).when(dispatcher).include(same(request), any());
            String html = new BranchPageContent().render(42, "example.sk", 7, 12, version, request, response);
            if (version == BranchDecision.Source.WORK) html = new BranchPageContent().prepareWork(html, 42, "example.sk", 7, request);
            assertTrue(html.contains("rendered include"));assertTrue(html.contains(".page{color:red}"));
            assertTrue(html.contains(version == BranchDecision.Source.LIVE ? "/main/page.html" : "/work/page.html"));
            assertFalse(html.contains("http-equiv=\"refresh\""));assertFalse(html.contains("branch-preview-status"));
            assertFalse(html.contains("branch-preview-styles"));
            assertFalse(html.contains("data-aos"));assertFalse(html.contains("enable scripts"));
            assertTrue(html.contains("<button disabled>"));
            assertEquals(selected, BranchContexts.current(request));
            assertEquals("original", request.getAttribute("ShowdocAction.showDocData"));
            assertEquals("original-buffer", request.getAttribute(BuffTag.IS_BUFF_TAG));
            assertEquals("/original", request.getAttribute(BranchPreviewUrls.LOGICAL_PATH));
            assertNull(request.getAttribute("isPreview"));
        }
    }
}
