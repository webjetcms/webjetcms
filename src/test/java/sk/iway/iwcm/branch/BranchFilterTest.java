package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.GroupsDB;

/** Request-boundary checks; content readers/renderers have separate real DB/JSP coverage. */
class BranchFilterTest {
    private static final String DOMAIN="a.example";
    private final BranchFilter filter=new BranchFilter();
    private final MockHttpServletRequest request=new MockHttpServletRequest();
    private MockHttpServletResponse response=new MockHttpServletResponse();
    private final Identity admin=new Identity();
    private final RequestBean bean=new RequestBean();
    private final BranchContext branch=new BranchContext(42,DOMAIN,1,BranchContext.Mode.ADMIN);
    private MockedStatic<Constants> constants;
    private MockedStatic<GroupsDB> groups;
    private MockedStatic<BranchSelection> selection;

    @BeforeEach void requestBoundary() {
        constants=mockStatic(Constants.class);constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
        constants.when(()->Constants.getString("xsrfReferers")).thenReturn("");
        constants.when(()->Constants.getString("allowAdminUrls")).thenReturn("");
        constants.when(()->Constants.getString("xsrfParamNameExceptionSystem")).thenReturn("");
        constants.when(Constants::getInstallName).thenReturn("test");
        groups=mockStatic(GroupsDB.class);GroupsDB permissions=mock(GroupsDB.class);groups.when(GroupsDB::getInstance).thenReturn(permissions);
        admin.setAdmin(true);admin.setUserId(1);when(permissions.getUserRootDomainNames(admin)).thenReturn(List.of(DOMAIN,"b.example"));
        request.setMethod("GET");request.setServerName("admin.example");request.getSession().setAttribute(Constants.USER_KEY,admin);request.getSession().setAttribute("preview.editorDomainName",DOMAIN);
        selection=mockStatic(BranchSelection.class);selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN,1));
        bean.setRequest(request);bean.setDomain("old.example");bean.setUrl("/old.html");SetCharacterEncodingFilter.setCurrentRequestBean(bean);
    }
    @AfterEach void cleanBoundary(){selection.close();groups.close();constants.close();SetCharacterEncodingFilter.setCurrentRequestBean(null);}

    @Test void schedulingActionsUseTechnicalLiveContextEvenWhenEditorHasASelection() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setMethod("POST");
        for(String action:List.of("schedule","unschedule","reopen")) {
            request.setRequestURI("/admin/rest/branches/42/"+action);response=new MockHttpServletResponse();
            filter.doFilter(request,response,(in,out)->assertFalse(BranchContexts.current().isBranch()));
            assertEquals(200,response.getStatus());
        }
    }

    @Test void permitsOnlyOrdinaryEditorPostsAndRejectsLegacyImportActions() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setMethod("POST"); request.addHeader(BranchSelection.ID_HEADER,"42"); request.addHeader(BranchSelection.DOMAIN_HEADER,DOMAIN);
        for(String path:List.of("/admin/rest/web-pages/editor","/admin/rest/groups/editor","/admin/rest/news/news-list/editor","/admin/rest/media/editor","/admin/upload/chunk","/admin/upload/overwrite","/admin/web-pages/upload/","/admin/elfinder-connector/")) {
            request.setRequestURI(path); response=new MockHttpServletResponse();
            filter.doFilter(request,response,(in,out)->assertEquals(branch,BranchContexts.current())); assertEquals(200,response.getStatus());
        }
        for(String path:List.of("/admin/rest/web-pages/add","/admin/rest/web-pages/action/saveAsAbTest","/admin/rest/groups/tree/save","/admin/rest/media/action/import","/admin/rest/groups/row-reorder")) {
            request.setRequestURI(path); response=new MockHttpServletResponse();
            filter.doFilter(request,response,(in,out)->fail("Unsupported write dispatched: "+path)); assertEquals(409,response.getStatus());
        }
    }

    @Test void nestedIframeBootstrapChecksBothFrozenContextValues() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setRequestURI("/apps/news/admin/"); request.setParameter("branchId","42"); request.setParameter("branchDomain",DOMAIN);
        filter.doFilter(request,response,(in,out)->assertEquals(branch,BranchContexts.current())); assertEquals(200,response.getStatus());
        response=new MockHttpServletResponse(); request.setParameter("branchDomain","b.example");
        filter.doFilter(request,response,(in,out)->fail("Foreign iframe domain dispatched")); assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse(); request.setParameter("branchDomain",DOMAIN);
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN,1));
        filter.doFilter(request,response,(in,out)->fail("Stale branch iframe fell back to LIVE")); assertEquals(409,response.getStatus());
    }

    @Test void fileManagerLoadsOnlyStaticDependenciesAndChecksItsFrozenScreenContext() throws Exception {
        selected();
        for(String path:List.of("/admin/elFinder/js/elfinder.full.js","/admin/elFinder/css/elfinder.full.css","/admin/codemirror/lib/codemirror.js")) {
            request.setRequestURI(path);response=new MockHttpServletResponse();
            filter.doFilter(request,response,(in,out)->assertFalse(BranchContexts.current().isBranch()));assertEquals(200,response.getStatus());
        }
        headers(42,DOMAIN);request.setRequestURI("/admin/elFinder/gethash.jsp");request.setMethod("POST");response=new MockHttpServletResponse();
        filter.doFilter(request,response,(in,out)->assertEquals(branch,BranchContexts.current()));assertEquals(200,response.getStatus());
        headers(43,DOMAIN);request.setRequestURI("/admin/upload/chunk");response=new MockHttpServletResponse();
        filter.doFilter(request,response,(in,out)->fail("Stale upload reached the writer"));assertEquals(409,response.getStatus());
        headers(42,DOMAIN);request.setRequestURI("/admin/elFinder/unsafe.jsp");response=new MockHttpServletResponse();
        filter.doFilter(request,response,(in,out)->fail("Dynamic dependency bypassed the supported routes"));assertEquals(409,response.getStatus());
    }

    @Test void inlineEditorFrameReadsWorkAndNeverFallsThroughAfterSelectionChanges() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setRequestURI("/p04-frame.html"); request.setParameter("inlineEditorAdmin","true"); request.setParameter("branchId","42"); request.setParameter("branchDomain",DOMAIN);
        var view=mock(BranchView.class); var doc=new sk.iway.iwcm.doc.DocDetails(); doc.setDocId(123);
        try(var views=mockStatic(BranchView.class);var editor=mockStatic(sk.iway.iwcm.editor.service.EditorService.class)) {
            views.when(BranchView::current).thenReturn(view); when(view.docIdFromUrl("/p04-frame.html")).thenReturn(123); when(view.document(123,false)).thenReturn(doc);
            editor.when(()->sk.iway.iwcm.editor.service.EditorService.hasPagePermission(admin,doc)).thenReturn(true);
            filter.doFilter(request,response,(in,out)->{assertEquals(branch,BranchContexts.current());assertEquals(Boolean.TRUE,request.getAttribute("isPreview"));});
            assertEquals(200,response.getStatus()); assertTrue(response.getHeader("Cache-Control").contains("no-store")); assertEquals("old.example",bean.getDomain());
            response=new MockHttpServletResponse();selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN,1));
            filter.doFilter(request,response,(in,out)->fail("Old editor frame fell back to production")); assertEquals(409,response.getStatus());
        }
    }

    @Test void flagOffAndGuestPreviewArePrivateAndNeverDispatch()throws Exception {
        request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(false);
        filter.doFilter(request,response,(in,out)->fail("Disabled preview dispatched"));assertEquals(404,response.getStatus());assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        response=new MockHttpServletResponse();request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(42,DOMAIN));request.setRequestURI("/components/news/admin_save.jsp");
        filter.doFilter(request,response,(in,out)->fail("Disabled stale branch executed a legacy write"));assertEquals(409,response.getStatus());request.getSession().removeAttribute(BranchSelection.SESSION_KEY);request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");
        response=new MockHttpServletResponse();constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);request.getSession().removeAttribute(Constants.USER_KEY);
        filter.doFilter(request,response,(in,out)->fail("Guest preview dispatched"));assertEquals(403,response.getStatus());assertTrue(response.getHeader("Cache-Control").contains("no-store"));
    }

    @Test void adminPreviewChecksOriginalRequestBeforeResolvingTheBranch()throws Exception {
        request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");
        constants.when(()->Constants.getString("clusterMyNodeType")).thenReturn("public");
        filter.doFilter(request,response,(in,out)->fail("Public node served preview"));
        assertEquals(404,response.getStatus());
        selection.verifyNoInteractions();

        response=new MockHttpServletResponse();
        constants.when(()->Constants.getString("clusterMyNodeType")).thenReturn("");
        constants.when(()->Constants.getString("adminEnableIPs")).thenReturn("192.0.2.");
        filter.doFilter(request,response,(in,out)->fail("Disallowed IP served preview"));
        assertEquals(404,response.getStatus());
        selection.verifyNoInteractions();
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
    }

    @Test void adminPreviewRedirectsToHttpsBeforeResolvingTheBranch()throws Exception {
        request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");
        request.setQueryString("test=1");
        constants.when(()->Constants.getBoolean("adminRequireSSL")).thenReturn(true);
        filter.doFilter(request,response,(in,out)->fail("Insecure preview dispatched"));
        assertEquals(302,response.getStatus());
        assertEquals("https://admin.example/admin/webpages/branch-preview/42/images/a.png?test=1",response.getRedirectedUrl());
        selection.verifyNoInteractions();
    }

    @Test void adminPreviewRejectsForeignRefererBeforeResolvingTheBranch()throws Exception {
        request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");
        request.addHeader("referer","https://foreign.example/");
        constants.when(()->Constants.getString("xsrfReferers")).thenReturn("admin.example");
        try(var audit=mockStatic(sk.iway.iwcm.Adminlog.class)) {
            filter.doFilter(request,response,(in,out)->fail("Foreign referer served preview"));
            assertEquals(403,response.getStatus());
            selection.verifyNoInteractions();
        }
    }

    @Test void rejectsStaleLiveDomainAndMissingContextForWrites()throws Exception {
        request.setRequestURI("/admin/rest/web-pages/all");request.addHeader(BranchSelection.ID_HEADER,"0");request.addHeader(BranchSelection.DOMAIN_HEADER,"b.example");
        filter.doFilter(request,response,(in,out)->fail("Stale LIVE domain dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.removeHeader(BranchSelection.ID_HEADER);request.removeHeader(BranchSelection.DOMAIN_HEADER);request.setMethod("POST");
        filter.doFilter(request,response,(in,out)->fail("Unscoped LIVE write dispatched"));assertEquals(409,response.getStatus());
    }

    @Test void activeBranchRequiresMatchingHeadersAndBlocksMutations()throws Exception {
        selected();request.setRequestURI("/admin/rest/web-pages/all");
        filter.doFilter(request,response,(in,out)->fail("Unscoped branch list dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();headers(41,DOMAIN);filter.doFilter(request,response,(in,out)->fail("Old branch screen dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();headers(42,DOMAIN);request.setMethod("POST");filter.doFilter(request,response,(in,out)->fail("P03 content write dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.setMethod("GET");request.setRequestURI("/admin/restart.jsp");filter.doFilter(request,response,(in,out)->fail("Unsupported admin GET dispatched"));assertEquals(409,response.getStatus());
    }

    @Test void legacyAdminSaveAndUploadCannotBypassTheAdminPathGuard()throws Exception {
        selected();
        for(String path:List.of("/components/gallery/admin_save_image_ajax_utf-8.jsp","/components/news/admin_save.jsp","/components/elfinder/metadata/admin_metadata_save_ajax.jsp","/XhrFileUpload","/Editor.do")) {
            for(String method:List.of("GET","POST")) {
                response=new MockHttpServletResponse();request.setRequestURI(path);request.setMethod(method);filter.doFilter(request,response,(in,out)->fail("Legacy mutation dispatched: "+method+" "+path));assertEquals(409,response.getStatus(),method+" "+path);
            }
        }
        response=new MockHttpServletResponse();request.setMethod("POST");request.setRequestURI("/public-submit");filter.doFilter(request,response,(in,out)->fail("Public mutation dispatched in a selected branch"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.setMethod("GET");request.setRequestURI("/public.html");AtomicBoolean dispatched=new AtomicBoolean();filter.doFilter(request,response,(in,out)->dispatched.set(true));assertTrue(dispatched.get());assertFalse(BranchContexts.current().isBranch());
    }

    @Test void permitsReadOnlyTreePostAndRestoresScopeAfterDownstreamFailure()throws Exception {
        selected();headers(42,DOMAIN);request.setMethod("POST");request.setRequestURI("/admin/rest/groups/tree/tree");AtomicBoolean dispatched=new AtomicBoolean();
        filter.doFilter(request,response,(in,out)->{dispatched.set(true);assertEquals(branch,BranchContexts.current());});assertTrue(dispatched.get());assertFalse(BranchContexts.current().isBranch());assertEquals("old.example",bean.getDomain());
        request.setMethod("GET");request.setRequestURI("/admin/rest/web-pages/all");
        assertThrows(ServletException.class,()->filter.doFilter(request,response,(in,out)->{assertEquals(branch,BranchContexts.current());throw new ServletException("fixture downstream error");}));
        assertFalse(BranchContexts.current().isBranch());assertNull(request.getAttribute(BranchContexts.ATTRIBUTE));assertEquals("old.example",bean.getDomain());assertEquals("/old.html",bean.getUrl());
    }
    @Test void disablingTheFeatureDoesNotTurnASelectedBranchIntoLiveWrites()throws Exception {
        selected();constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(false);
        request.setMethod("POST");request.setRequestURI("/admin/rest/web-pages/all");filter.doFilter(request,response,(in,out)->fail("Disabled stale branch write dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.getSession().removeAttribute(BranchSelection.SESSION_KEY);headers(42,DOMAIN);request.setRequestURI("/XhrFileUpload");filter.doFilter(request,response,(in,out)->fail("Disabled explicit branch upload dispatched"));assertEquals(409,response.getStatus());
    }
    @Test void anAssetSuffixOnARestRouteDoesNotBypassWriteProtection()throws Exception {
        selected();headers(42,DOMAIN);request.setMethod("POST");request.setRequestURI("/admin/rest/elfinder/connector.js");filter.doFilter(request,response,(in,out)->fail("REST asset suffix bypass dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.setMethod("GET");request.setRequestURI("/admin/v9/dist/scripts/app.js");AtomicBoolean dispatched=new AtomicBoolean();filter.doFilter(request,response,(in,out)->dispatched.set(true));assertTrue(dispatched.get());
    }

    private void selected(){request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(42,DOMAIN));selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);}
    private void headers(long id,String domain){request.removeHeader(BranchSelection.ID_HEADER);request.removeHeader(BranchSelection.DOMAIN_HEADER);request.addHeader(BranchSelection.ID_HEADER,Long.toString(id));request.addHeader(BranchSelection.DOMAIN_HEADER,domain);}
}
