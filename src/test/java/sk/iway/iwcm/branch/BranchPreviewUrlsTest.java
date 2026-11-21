package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import sk.iway.iwcm.i18n.Prop;
import org.springframework.mock.web.*;
import sk.iway.iwcm.*;

/** URL transport and private response boundaries independent of the database. */
class BranchPreviewUrlsTest {
    @Test void relativeAbsoluteQueryUnicodeAndExternalUrls()throws Exception {
        RequestBean bean=new RequestBean();MockHttpServletRequest request=new MockHttpServletRequest();
        request.setAttribute(BranchPreviewUrls.LOGICAL_PATH,"/section/news.html");
        try(var lifecycle=mockStatic(SetCharacterEncodingFilter.class)) {
            lifecycle.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);
            try(var scope=BranchContexts.open(request,new BranchContext(42,"public.example",1,BranchContext.Mode.PREVIEW))) {
                assertEquals("/admin/webpages/branch-preview/42/section/news.html?page=2&tag=a#more",BranchPreviewUrls.toPreviewUrl("?page=2&tag=a#more",request));
                assertEquals("/admin/webpages/branch-preview/42/detail.html",BranchPreviewUrls.toPreviewUrl("../detail.html",request));
                assertEquals("/admin/webpages/branch-preview/42/images/a%20b.png",BranchPreviewUrls.toPreviewUrl("https://public.example/images/a%20b.png",request));
                assertEquals("https://external.example/elsewhere",BranchPreviewUrls.toPreviewUrl("https://external.example/elsewhere",request));
                assertEquals("#anchor",BranchPreviewUrls.toPreviewUrl("#anchor",request));assertEquals("mailto:a@example.sk",BranchPreviewUrls.toPreviewUrl("mailto:a@example.sk",request));
                assertEquals("/admin/webpages/branch-preview/42/a.html",BranchPreviewUrls.toPreviewUrl("/admin/webpages/branch-preview/42/a.html",request));
                assertFalse(BranchPreviewUrls.allowed("/admin/index.jsp"));assertFalse(BranchPreviewUrls.allowed("/templates/x.jsp"));assertFalse(BranchPreviewUrls.allowed("/images/../WEB-INF/config"));
                MockHttpServletResponse target=new MockHttpServletResponse();BranchPreviewResponseWrapper response=new BranchPreviewResponseWrapper(target,request);
                response.setHeader("Cache-Control","public,max-age=900");response.setDateHeader("Last-Modified",123);response.setHeader("ETag","bad");response.setContentType("text/css;charset=UTF-8");response.getWriter().write("a{background:url('/images/a.png')}");response.finish();
                assertTrue(target.getHeader("Cache-Control").contains("no-store"));assertNull(target.getHeader("ETag"));assertTrue(target.getContentAsString().contains("/admin/webpages/branch-preview/42/images/a.png"));
                MockHttpServletResponse redirect=new MockHttpServletResponse();var wrapper=new BranchPreviewResponseWrapper(redirect,request);wrapper.sendRedirect("/next.html?x=1");wrapper.finish();assertEquals("/admin/webpages/branch-preview/42/next.html?x=1",redirect.getHeader("Location"));
            }
        }
    }

    /** Draft previews omit the changing clock; scheduled previews retain the publication date and safe labels. */
    @Test void previewIndicatorShowsOnlyAMeaningfulPublicationDate() {
        var request=new MockHttpServletRequest();
        var view=mock(BranchView.class);
        var prop=mock(Prop.class);
        String name="Launch <script>alert(1)</script>";
        when(view.name()).thenReturn(name);
        when(view.color()).thenReturn("lavender");
        when(prop.getText("components.branches.preview.label")).thenReturn("Preview");
        when(prop.getText("components.branches.preview.conflicts")).thenReturn("Conflicts require review");
        long at=2_096_177_400_000L;
        String date="04.06.2036 09:30:00";
        try(var views=mockStatic(BranchView.class);var translations=mockStatic(Prop.class);var tools=mockStatic(Tools.class)) {
            tools.when(()->Tools.formatDateTimeSeconds(at)).thenReturn(date);
            views.when(BranchView::current).thenReturn(view);
            translations.when(()->Prop.getInstance(request)).thenReturn(prop);
            var draft=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><head><style>#branch-preview-status { background: white; }</style></head><body><main>Page</main></body></html>",request));
            var indicator=draft.selectFirst("#branch-preview-status");
            assertEquals("Preview: "+name,indicator.text());
            assertEquals("lavender",indicator.attr("data-branch-color"));
            assertTrue(indicator.select("[style]").isEmpty());
            assertEquals("branch-preview-styles",draft.head().child(0).id());
            assertEquals("/admin/v9/dist/css/branch-preview.css",draft.head().child(0).attr("href"));
            assertEquals("style",draft.head().child(1).tagName());
            assertEquals(1,draft.select("#branch-preview-status").size());
            assertTrue(indicator.select("script").isEmpty());
            assertEquals("Page",draft.selectFirst("main").text());
            verify(view,never()).previewAt();

            when(view.scheduledPreview()).thenReturn(true);
            when(view.previewAt()).thenReturn(at);
            when(prop.getText("branches.merge.scheduledAt.js",date)).thenReturn("Scheduled for "+date);
            var scheduled=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><body></body></html>",request)).selectFirst("#branch-preview-status");
            assertTrue(scheduled.text().contains("Scheduled for "+date),scheduled.outerHtml()+"; expected date="+date);

            when(view.scheduledPreview()).thenReturn(false);
            when(view.hasConflicts()).thenReturn(true);
            var conflict=Jsoup.parse(BranchPreviewUrls.rewriteHtml("<html><body></body></html>",request)).selectFirst("#branch-preview-status");
            assertFalse(conflict.text().contains(date));
            assertTrue(conflict.text().contains("Conflicts require review"));
        }
    }

}
