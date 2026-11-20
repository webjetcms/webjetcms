package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
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
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;

/** Request-boundary checks; content readers/renderers have separate real DB/JSP coverage. */
class BranchFilterTest {
    private static final String DOMAIN="a.example";
    private final BranchFilter filter=new BranchFilter();
    private final MockHttpServletRequest request=new MockHttpServletRequest();
    private MockHttpServletResponse response=new MockHttpServletResponse();
    private final Identity admin=spy(new Identity());
    private final RequestBean bean=new RequestBean();
    private final BranchContext branch=new BranchContext(42,DOMAIN,1,BranchContext.Mode.ADMIN);
    private MockedStatic<Constants> constants;
    private MockedStatic<GroupsDB> groups;
    private MockedStatic<BranchSelection> selection;
    private MockedStatic<Modules> modules;

    @BeforeEach void requestBoundary() {
        constants=mockStatic(Constants.class);constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
        constants.when(()->Constants.getString("xsrfReferers")).thenReturn("");
        constants.when(()->Constants.getString("allowAdminUrls")).thenReturn("");
        constants.when(()->Constants.getString("xsrfParamNameExceptionSystem")).thenReturn("");
        constants.when(Constants::getInstallName).thenReturn("test");
        groups=mockStatic(GroupsDB.class);GroupsDB permissions=mock(GroupsDB.class);groups.when(GroupsDB::getInstance).thenReturn(permissions);groups.when(()->GroupsDB.getDomainId(DOMAIN)).thenReturn(1);groups.when(()->GroupsDB.getDomainId("b.example")).thenReturn(2);
        admin.setAdmin(true);admin.setUserId(1);when(permissions.getUserRootDomainNames(admin)).thenReturn(List.of(DOMAIN,"b.example"));
        doReturn(true).when(admin).isEnabledItem(BranchAccess.PERMISSION);
        request.setMethod("GET");request.setServerName("admin.example");request.getSession().setAttribute(Constants.USER_KEY,admin);request.getSession().setAttribute("preview.editorDomainName",DOMAIN);
        selection=mockStatic(BranchSelection.class);selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN,1));
        selection.when(() -> BranchSelection.selected(eq(request), eq(admin), eq(DOMAIN), anyBoolean()))
            .thenAnswer(call -> BranchSelection.selected(request, admin, DOMAIN));
        modules = mockStatic(Modules.class);
        Modules menu = mock(Modules.class);modules.when(Modules::getInstance).thenReturn(menu);
        when(menu.getModules()).thenReturn(List.of(
            menuItem("/admin/temps_list.jsp"), menuItem("/admin/restart.jsp"),
            menuItem("/admin/v9/apps/image-editor/"), menuItem("/apps/contact/admin/"),
            menuItem("/components/catalog/custom_admin.jsp"), menuItem("/apps/catalog/administration/")));
        bean.setRequest(request);bean.setDomain("old.example");bean.setUrl("/old.html");SetCharacterEncodingFilter.setCurrentRequestBean(bean);
    }
    @AfterEach void cleanBoundary(){modules.close();selection.closeOnDemand();groups.close();constants.close();SetCharacterEncodingFilter.setCurrentRequestBean(null);}

    /** The preview bootstrap and its form POST must render WORK even without Fetch Metadata headers. */
    @Test void componentPreviewKeepsTheWorkingVersionForDocumentAndFrameRequests() throws Exception {
        selected();
        var pkg = new BranchEntity();pkg.setName("Autumn");
        selection.when(() -> BranchSelection.requireBranch(42, DOMAIN)).thenReturn(pkg);
        String preview = "/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp";
        request.setRequestURI(preview);
        request.addHeader("Accept", "text/html");
        for (String destination : List.of("", "iframe", "document")) {
            request.removeHeader("Sec-Fetch-Dest");
            if (!destination.isEmpty()) request.addHeader("Sec-Fetch-Dest", destination);
            for (String method : List.of("GET", "POST")) {
                request.setMethod(method);
                if ("POST".equals(method)) {
                    request.setParameter("branchId", "42");request.setParameter("branchDomain", "1");
                    request.addHeader("Referer", "http://admin.example" + preview);
                } else {
                    request.removeParameter("branchId");request.removeParameter("branchDomain");request.removeHeader("Referer");
                }
                response = new MockHttpServletResponse();
                AtomicBoolean dispatched = new AtomicBoolean();
                filter.doFilter(request, response, (in, out) -> {
                    dispatched.set(true);assertEquals(branch, BranchContexts.current());
                });
                assertTrue(dispatched.get(), method + " / " + destination + " must render the component instead of the production dialog");
                assertNull(response.getForwardedUrl());assertNull(response.getHeader("Location"));
                assertFalse(BranchContexts.current().isBranch());
            }
        }
        assertFalse(BranchProductionSection.needsConfirmation(preview));
        request.setMethod("POST");request.setParameter("branchId", "41");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("A stale component preview rendered another version"));
        assertEquals(409, response.getStatus());
        request.setParameter("branchId", "42");request.setParameter("branchDomain", "2");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("A component preview crossed the domain boundary"));
        assertEquals(409, response.getStatus());
    }

    @Test void mappedProductionScreensRequireConsentBeforeDispatch() throws Exception {
        selected();
        var pkg = new BranchEntity();
        pkg.setName("Autumn");
        selection.when(() -> BranchSelection.requireBranch(42, DOMAIN)).thenReturn(pkg);
        request.addHeader("Sec-Fetch-Dest", "document");
        for (String path : List.of("/admin/v9/templates/temps-list/", "/admin/v9/files/folder_prop/",
                "/admin/v9/apps/image-editor/", "/apps/contact/admin/",
                "/components/catalog/custom_admin.jsp", "/apps/catalog/administration/")) {
            request.setRequestURI(path);
            request.setQueryString("id=7&view=edit");
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, (in, out) -> fail("Dispatched before confirmation"));
            assertEquals("/WEB-INF/branch-production.jsp", response.getForwardedUrl());
            assertEquals("Autumn", request.getAttribute("productionBranchName"));
            String continuation = (String) request.getAttribute("productionContinuation");
            assertTrue(continuation.startsWith("?id=7&view=edit&"));
            request.setQueryString(continuation.substring(1));
            AtomicBoolean opened = new AtomicBoolean();
            filter.doFilter(request, new MockHttpServletResponse(), (in, out) -> {
                opened.set(true);
                assertFalse(BranchContexts.current().isBranch());
                assertEquals(branch, BranchProductionSection.currentSelection());
            });
            assertTrue(opened.get());
            assertNull(BranchProductionSection.currentSelection());
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, (in, out) -> fail("Reload skipped the dialog"));
            assertEquals("/WEB-INF/branch-production.jsp", response.getForwardedUrl());
        }
        request.setQueryString(null);
        for (String path : List.of("/admin/v9/webpages/web-pages-list/", "/admin/v9/webpages/branches/", "/admin/v9/files/index/", "/apps/news/admin/")) {
            request.setRequestURI(path);
            AtomicBoolean dispatched = new AtomicBoolean();
            filter.doFilter(request, new MockHttpServletResponse(), (in, out) -> {
                dispatched.set(true);
                assertEquals(branch, BranchContexts.current());
            });
            assertTrue(dispatched.get());
        }
    }

    @Test void confirmedScreenUsesProductionForRestFormsAndFramesOnlyInItsOriginalSelection() throws Exception {
        selected();
        request.setRequestURI("/admin/v9/templates/temps-list/");
        String continuation = BranchProductionSection.continuation(request, branch);
        request.setQueryString(continuation.substring(1));
        request.addHeader("Sec-Fetch-Dest", "document");
        filter.doFilter(request, response, (in, out) -> assertFalse(BranchContexts.current().isBranch()));
        request.removeHeader("Sec-Fetch-Dest");
        request.setQueryString(null);
        String referrer = request.getRequestURL() + continuation;
        request.addHeader("Referer", referrer);
        request.addHeader("Sec-Fetch-Dest", "empty");
        request.addHeader(BranchSelection.ID_HEADER, "42");
        request.addHeader(BranchSelection.DOMAIN_HEADER, "1");
        request.setMethod("POST");
        request.setRequestURI("/admin/rest/templates/editor");
        filter.doFilter(request, response, (in, out) -> fail("Missing CSRF"));
        assertEquals(403, response.getStatus());
        request.getSession().setAttribute("webjet-csrf-tokens-list", List.of("test-token"));
        request.addHeader("X-CSRF-Token", "test-token");
        for (String path : List.of("/admin/rest/templates/editor", "/admin/conf_import.jsp", "/XhrFileUpload")) {
            request.setRequestURI(path);
            response = new MockHttpServletResponse();
            AtomicBoolean dispatched = new AtomicBoolean();
            filter.doFilter(request, response, (in, out) -> {
                dispatched.set(true);
                assertFalse(BranchContexts.current().isBranch());
            });
            assertTrue(dispatched.get());
        }
        request.setRequestURI("/admin/v9/files/dialog/");
        request.setMethod("GET");
        request.removeHeader("Sec-Fetch-Dest");
        request.addHeader("Sec-Fetch-Dest", "iframe");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("Frame must first inherit the production context"));
        assertEquals(307, response.getStatus());
        request.setQueryString(response.getHeader("Location").substring(1));
        filter.doFilter(request, new MockHttpServletResponse(), (in, out) -> assertFalse(BranchContexts.current().isBranch()));

        request.setQueryString(null);
        request.setRequestURI("/admin/rest/templates/editor");
        request.setMethod("POST");
        selection.when(() -> BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN, 1));
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("Stale production page wrote after a selection change"));
        assertEquals(409, response.getStatus());

        selected();
        request.removeHeader("Referer");
        request.addHeader("Referer", "http://outside.example/admin/v9/templates/temps-list/" + continuation);
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("External referrer authorised a write"));
        assertEquals(409, response.getStatus());
    }

    @Test void schedulingActionsUseTechnicalLiveContextEvenWhenEditorHasASelection() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setMethod("POST");
        for(String action:List.of("schedule","unschedule","reopen")) {
            request.setRequestURI("/admin/rest/branches/42/"+action);response=new MockHttpServletResponse();
            filter.doFilter(request,response,(in,out)->assertFalse(BranchContexts.current().isBranch()));
            assertEquals(200,response.getStatus());
        }
    }

    @Test void metadataTableUsesLiveStorageWithoutChangingTheSelectedVersion() throws Exception {
        selected();
        request.setMethod("POST");
        for (String path : List.of("/admin/rest/branches/all", "/admin/rest/branches/-1",
                "/admin/rest/branches/search/findByColumns", "/admin/rest/branches/editor")) {
            request.setRequestURI(path);
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, (in, out) -> assertFalse(BranchContexts.current().isBranch()));
            assertEquals(200, response.getStatus());
        }
        assertEquals(42, ((BranchSelection.Selected) request.getSession().getAttribute(BranchSelection.SESSION_KEY)).branchId());
    }

    @Test void permitsOnlyOrdinaryEditorPostsAndRejectsLegacyImportActions() throws Exception {
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);
        request.setMethod("POST"); request.addHeader(BranchSelection.ID_HEADER,"42"); request.addHeader(BranchSelection.DOMAIN_HEADER,"1");
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
        request.setRequestURI("/apps/news/admin/"); request.setParameter("branchId","42"); request.setParameter("branchDomain","1");
        filter.doFilter(request,response,(in,out)->assertEquals(branch,BranchContexts.current())); assertEquals(200,response.getStatus());
        response=new MockHttpServletResponse(); request.setParameter("branchDomain","b.example");
        filter.doFilter(request,response,(in,out)->fail("Foreign iframe domain dispatched")); assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse(); request.setParameter("branchDomain","1");
        selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(BranchContext.live(DOMAIN,1));
        filter.doFilter(request,response,(in,out)->fail("Stale branch iframe fell back to LIVE")); assertEquals(409,response.getStatus());
    }

    @Test void fileManagerDependenciesAndUploadsKeepTheirScreenContext() throws Exception {
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
        request.setRequestURI("/p04-frame.html"); request.setParameter("inlineEditorAdmin","true"); request.setParameter("branchId","42"); request.setParameter("branchDomain","1");
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
        response=new MockHttpServletResponse();request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(42,1));request.setRequestURI("/components/news/admin_save.jsp");
        filter.doFilter(request,response,(in,out)->fail("Disabled stale branch executed a legacy write"));assertEquals(409,response.getStatus());request.getSession().removeAttribute(BranchSelection.SESSION_KEY);request.setRequestURI("/admin/webpages/branch-preview/42/images/a.png");
        response=new MockHttpServletResponse();constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);request.getSession().removeAttribute(Constants.USER_KEY);
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
        request.setRequestURI("/admin/rest/web-pages/all");request.addHeader(BranchSelection.ID_HEADER,"0");request.addHeader(BranchSelection.DOMAIN_HEADER,"2");
        filter.doFilter(request,response,(in,out)->fail("Stale LIVE domain dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.removeHeader(BranchSelection.ID_HEADER);request.removeHeader(BranchSelection.DOMAIN_HEADER);request.setMethod("POST");
        filter.doFilter(request,response,(in,out)->fail("Unscoped LIVE write dispatched"));assertEquals(409,response.getStatus());
    }

    @Test void reloadingBranchManagementAfterPublicationReturnsToLive() throws Exception {
        selection.close();
        var published = new BranchEntity();
        published.setId(42L);
        published.setDomainId(1);
        published.setState("MERGED");
        var service = mock(BranchService.class);
        when(service.find(42L, DOMAIN)).thenReturn(Optional.of(published));
        var previous = new BranchSelection.Selected(42, 1);
        request.getSession().setAttribute(BranchSelection.SESSION_KEY, previous);
        request.getSession().setAttribute(BranchProductionSection.ATTRIBUTE, new Object());
        request.setRequestURI("/admin/v9/webpages/branches/");
        request.setParameter("id", "42");
        request.setParameter("tab", "changes");
        request.addHeader("Sec-Fetch-Dest", "document");
        try (var readiness = mockStatic(BranchReadiness.class); var services = mockStatic(BranchService.class)) {
            services.when(BranchService::getInstance).thenReturn(service);
            request.setParameter("branchId", "42");
            request.setParameter("branchDomain", "1");
            filter.doFilter(request, response, (in, out) -> fail("An explicit WORK request must never switch to LIVE"));
            assertEquals(409, response.getStatus());
            assertEquals(previous, request.getSession().getAttribute(BranchSelection.SESSION_KEY));

            request.removeParameter("branchId");
            request.removeParameter("branchDomain");
            response = new MockHttpServletResponse();
            AtomicBoolean rendered = new AtomicBoolean();
            filter.doFilter(request, response, (in, out) -> {
                rendered.set(true);
                assertEquals(BranchContext.live(DOMAIN, 1), BranchContexts.current());
                assertEquals("42", in.getParameter("id"));
                assertEquals("changes", in.getParameter("tab"));
            });
            assertEquals(200, response.getStatus(), response.getErrorMessage());
            assertTrue(rendered.get());
            assertNull(request.getSession().getAttribute(BranchSelection.SESSION_KEY));
            assertNull(request.getSession().getAttribute(BranchProductionSection.ATTRIBUTE));

            request.setMethod("POST");
            request.setRequestURI("/admin/rest/web-pages/all");
            headers(42, DOMAIN);
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, (in, out) -> fail("An old WORK form must never write to LIVE"));
            assertEquals(409, response.getStatus());
        }
    }

    @Test void activeBranchRequiresMatchingHeadersAndBlocksMutations()throws Exception {
        selected();request.setRequestURI("/admin/rest/web-pages/all");
        filter.doFilter(request,response,(in,out)->fail("Unscoped branch list dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();headers(41,DOMAIN);filter.doFilter(request,response,(in,out)->fail("Old branch screen dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();headers(42,DOMAIN);request.setMethod("POST");filter.doFilter(request,response,(in,out)->fail("P03 content write dispatched"));assertEquals(409,response.getStatus());
        response=new MockHttpServletResponse();request.setMethod("GET");request.setRequestURI("/admin/restart.jsp");filter.doFilter(request,response,(in,out)->fail("Unsupported admin GET dispatched"));assertEquals(409,response.getStatus());
    }

    /** Dynamic public assets and application requests keep their own response instead of a production dialog or 409. */
    @Test void publicApplicationRequestsDoNotBecomeAdministrationScreens() throws Exception {
        selected();
        var pkg = new BranchEntity();pkg.setName("Autumn");
        selection.when(() -> BranchSelection.requireBranch(42, DOMAIN)).thenReturn(pkg);
        request.addHeader("Referer", "http://admin.example/admin/webpages/branch-preview/42/news.html");
        request.addHeader("Accept", "text/html");
        for (String path : List.of("/components/_common/combine.jsp", "/components/_common/javascript/page_functions.js.jsp",
                "/components/catalog/list.jsp", "/apps/catalog/data.action", "/apps/catalog/", "/templates/site/theme.jsp",
                "/components/catalog/app.js", "/apps/catalog/style.css", "/templates/site/main.css", "/templates/site/admin-theme.jsp")) {
            request.setRequestURI(path);
            for (String destination : List.of("style", "script", "iframe", "document", "")) {
                request.removeHeader("Sec-Fetch-Dest");
                if (!destination.isEmpty()) request.addHeader("Sec-Fetch-Dest", destination);
                for (String method : List.of("GET", "HEAD", "POST")) {
                    request.setMethod(method);
                    response = new MockHttpServletResponse();
                    AtomicBoolean dispatched = new AtomicBoolean();
                    filter.doFilter(request, response, (in, out) -> {
                        dispatched.set(true);
                        assertFalse(BranchContexts.current().isBranch());
                        out.setContentType("text/css");
                    });
                    assertTrue(dispatched.get(), method + " " + path + " / " + destination + " returned " + response.getStatus());
                    assertNull(response.getForwardedUrl());
                    assertEquals("text/css", response.getContentType());
                }
            }
            assertFalse(BranchProductionSection.needsConfirmation(path), path);
        }
    }

    @Test void publicPageLinkReadsLiveWhileAPackageIsSelected() throws Exception {
        selected();
        request.setRequestURI("/showdoc.do");
        request.setParameter("docid", "123");
        for (String method : List.of("GET", "HEAD")) {
            request.setMethod(method);
            response = new MockHttpServletResponse();
            AtomicBoolean dispatched = new AtomicBoolean();
            filter.doFilter(request, response, (in, out) -> {
                dispatched.set(true);
                assertFalse(BranchContexts.current().isBranch());
            });
            assertTrue(dispatched.get(), method + " returned " + response.getStatus());
        }

        request.setMethod("POST");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("Dispatched a public POST"));
        assertEquals(409, response.getStatus());

        request.setMethod("GET");
        request.setParameter("historyid", "456");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("Dispatched a history request"));
        assertEquals(409, response.getStatus());
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

    /** CKEditor loads this JSP as JavaScript, so it must never receive a production confirmation response. */
    @Test void ckeditorDialogScriptLoadsWhileAWorkingVersionIsSelected() throws Exception {
        selected();
        request.setRequestURI("/admin/skins/webjet8/ckeditor/dist/plugins/webjet/js/fck_dialog_common.jsp");
        request.addHeader("Sec-Fetch-Dest", "script");
        request.addHeader("Referer", "http://admin.example/admin/v9/files/wj_image/?branchId=42&branchDomain=1");
        AtomicBoolean dispatched = new AtomicBoolean();
        filter.doFilter(request, response, (in, out) -> {
            dispatched.set(true);
            out.setContentType("text/javascript");
            out.getWriter().write("function GetE(id) { return document.getElementById(id); }");
        });
        assertTrue(dispatched.get(), "The CKEditor dialog script returned " + response.getStatus());
        assertEquals(200, response.getStatus());
        assertEquals("text/javascript", response.getContentType());
        assertTrue(response.getContentAsString().contains("function GetE("));
        assertNull(response.getForwardedUrl());
    }

    /** Helper requests retain WORK and their original response even when opened as a document or nested frame. */
    @Test void helperUrlsOutsideTheMenuDoNotRequireProductionConsent() throws Exception {
        selected();
        request.setParameter("set", "customEditorAssets");
        for (String path : List.of("/admin/skins/webjet8/ckeditor/config.jsp",
                "/admin/skins/webjet8/ckeditor/dist/plugins/custom/dialog.jsp",
                "/admin/scripts/combine.jsp", "/admin/elFinder/helper.jsp", "/admin/v9/future-section/",
                "/apps/catalog/admin/helper.jsp")) {
            assertFalse(BranchProductionSection.needsConfirmation(path), path);
            for (String destination : List.of("script", "iframe", "document", "")) {
                request.setRequestURI(path);
                request.removeHeader("Sec-Fetch-Dest");
                request.addHeader("Accept", "text/html");
                if (!destination.isEmpty()) request.addHeader("Sec-Fetch-Dest", destination);
                for (String method : List.of("GET", "HEAD")) {
                    request.setMethod(method);
                    response = new MockHttpServletResponse();
                    AtomicBoolean dispatched = new AtomicBoolean();
                    filter.doFilter(request, response, (in, out) -> {
                        dispatched.set(true);
                        assertEquals(branch, BranchContexts.current());
                    });
                    assertTrue(dispatched.get(), method + " " + path + " / " + destination);
                    assertEquals(200, response.getStatus());
                    assertNull(response.getForwardedUrl());
                    assertNull(response.getHeader("Location"));
                }
            }
        }
        assertFalse(BranchContexts.current().isBranch());
    }

    /** Unsaved editor previews use private WORK rendering and retain the frozen screen selection. */
    @Test void editorPreviewUsesWorkAssetsWithoutSavingThePage() throws Exception {
        selected();
        headers(42, DOMAIN);
        request.setMethod("POST");
        request.setRequestURI("/admin/rest/web-pages/preview/");
        AtomicBoolean dispatched = new AtomicBoolean();
        filter.doFilter(request, response, (in, out) -> {
            dispatched.set(true);
            assertEquals(branch, BranchContexts.current());
        });
        assertTrue(dispatched.get());

        request.setMethod("GET");
        request.setRequestURI("/admin/webpages/preview/");
        request.removeHeader(BranchSelection.ID_HEADER);
        request.removeHeader(BranchSelection.DOMAIN_HEADER);
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("An unfrozen preview was rendered"));
        assertEquals(409, response.getStatus());

        request.setParameter("branchId", "42");
        request.setParameter("branchDomain", "1");
        response = new MockHttpServletResponse();
        try (var views = mockStatic(BranchView.class); var properties = mockStatic(sk.iway.iwcm.i18n.Prop.class)) {
            var prop = mock(sk.iway.iwcm.i18n.Prop.class);
            properties.when(() -> sk.iway.iwcm.i18n.Prop.getInstance(request)).thenReturn(prop);
            when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            filter.doFilter(request, response, (in, out) -> {
                assertEquals(BranchContext.Mode.PREVIEW, BranchContexts.current().mode());
                assertEquals(42, BranchContexts.current().branchId());
                request.setAttribute(BranchPreviewUrls.LOGICAL_PATH, "/autotest.html");
                out.setContentType("text/html");
                out.getWriter().write("<p>Unsaved content</p><img src='/images/autotest.png'>");
            });
        }
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unsaved content"));
        assertTrue(response.getContentAsString().contains("/admin/webpages/branch-preview/42/images/autotest.png"));
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        assertFalse(BranchContexts.current().isBranch());

        request.setParameter("branchId", "43");
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (in, out) -> fail("A stale preview was rendered"));
        assertEquals(409, response.getStatus());
    }

    private static ModuleInfo menuItem(String path) {
        ModuleInfo item = new ModuleInfo();
        item.setPath(path);
        item.setItemKey("test-menu");
        return item;
    }

    private void selected(){request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(42,1));selection.when(()->BranchSelection.selected(request,admin,DOMAIN)).thenReturn(branch);}
    private void headers(long id,String domain){request.removeHeader(BranchSelection.ID_HEADER);request.removeHeader(BranchSelection.DOMAIN_HEADER);request.addHeader(BranchSelection.ID_HEADER,Long.toString(id));request.addHeader(BranchSelection.DOMAIN_HEADER,DOMAIN.equals(domain)?"1":"2");}
}
