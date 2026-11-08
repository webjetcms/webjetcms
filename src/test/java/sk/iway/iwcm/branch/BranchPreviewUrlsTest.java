package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import org.junit.jupiter.api.Test;
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
                assertEquals("/__branch-preview/42/section/news.html?page=2&tag=a#more",BranchPreviewUrls.toPreviewUrl("?page=2&tag=a#more",request));
                assertEquals("/__branch-preview/42/detail.html",BranchPreviewUrls.toPreviewUrl("../detail.html",request));
                assertEquals("/__branch-preview/42/images/a%20b.png",BranchPreviewUrls.toPreviewUrl("https://public.example/images/a%20b.png",request));
                assertEquals("https://external.example/elsewhere",BranchPreviewUrls.toPreviewUrl("https://external.example/elsewhere",request));
                assertEquals("#anchor",BranchPreviewUrls.toPreviewUrl("#anchor",request));assertEquals("mailto:a@example.sk",BranchPreviewUrls.toPreviewUrl("mailto:a@example.sk",request));
                assertEquals("/__branch-preview/42/a.html",BranchPreviewUrls.toPreviewUrl("/__branch-preview/42/a.html",request));
                assertFalse(BranchPreviewUrls.allowed("/admin/index.jsp"));assertFalse(BranchPreviewUrls.allowed("/templates/x.jsp"));assertFalse(BranchPreviewUrls.allowed("/images/../WEB-INF/config"));
                MockHttpServletResponse target=new MockHttpServletResponse();BranchPreviewResponseWrapper response=new BranchPreviewResponseWrapper(target,request);
                response.setHeader("Cache-Control","public,max-age=900");response.setDateHeader("Last-Modified",123);response.setHeader("ETag","bad");response.setContentType("text/css;charset=UTF-8");response.getWriter().write("a{background:url('/images/a.png')}");response.finish();
                assertTrue(target.getHeader("Cache-Control").contains("no-store"));assertNull(target.getHeader("ETag"));assertTrue(target.getContentAsString().contains("/__branch-preview/42/images/a.png"));
                MockHttpServletResponse redirect=new MockHttpServletResponse();var wrapper=new BranchPreviewResponseWrapper(redirect,request);wrapper.sendRedirect("/next.html?x=1");wrapper.finish();assertEquals("/__branch-preview/42/next.html?x=1",redirect.getHeader("Location"));
            }
        }
    }
}
