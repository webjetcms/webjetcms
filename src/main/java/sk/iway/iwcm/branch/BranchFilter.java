package sk.iway.iwcm.branch;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.apache.commons.lang3.Strings;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.spring.ApiTokenAuthFilter;
import sk.iway.iwcm.users.UsersDB;

/** Establishes the verified content context after authentication and before virtual routing. */
public final class BranchFilter implements Filter {
    private final BranchAccess access=new BranchAccess();

    @Override public void doFilter(ServletRequest input,ServletResponse output,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest request=(HttpServletRequest)input;HttpServletResponse response=(HttpServletResponse)output;
        boolean enabled = !ClusterDB.isPublicNode() && BranchAccess.isEnabled();
        if (!enabled) {
            if (allowWhenDisabled(request, response)) chain.doFilter(input, output);
            return;
        }
        HttpSession session = request.getSession(false);
        Identity user = UsersDB.getCurrentUser(session);
        boolean branchAdmin = BranchAccess.hasPermission(user);
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean preview=path.startsWith(BranchPreviewUrls.PREFIX);
        boolean dialog = "dialog".equals(request.getHeader("X-WebJET-Production-Navigation"));
        boolean checkProductionPage = branchAdmin || BranchProductionSection.hasProductionMarker(request);
        if (!checkProductionPage && !preview && !dialog && !requiresContextCheck(request, session, user, path)) {
            chain.doFilter(input, output);
            return;
        }
        if (preview) setPreviewHeaders(response);
        var document = checkProductionPage ? BranchProductionSection.document(request) : null;
        var source = checkProductionPage ? BranchProductionSection.referrer(request) : null;
        boolean navigation = isDocumentNavigation(request);
        // Fresh top-level navigation chooses its own mode. Embedded dialogs inherit their calling screen.
        if (source != null && navigation && !"0".equals(request.getParameter("branchId"))) source = null;
        boolean productionRequest = document != null || source != null;
        // A menu click requests just the dialog so the current page stays visible underneath it.
        if (dialog) {
            try {
                user = access.requireUser(request);
                if (!"GET".equals(request.getMethod()) || !BranchProductionSection.needsConfirmation(path)) {
                    response.sendError(400);
                    return;
                }
                String domain = access.requireDomain(request, user);
                BranchContext selection = BranchSelection.selected(request, user, domain);
                if (!selection.isBranch() || !Long.toString(selection.branchId()).equals(request.getHeader(BranchSelection.ID_HEADER))
                    || !Integer.toString(BranchDomains.id(domain)).equals(request.getHeader(BranchSelection.DOMAIN_HEADER))) {
                    response.sendError(409);
                    return;
                }
                showProductionDialog(request, response, selection);
            } catch (ResponseStatusException ex) {
                response.sendError(ex.getStatusCode().value(), ex.getReason());
            }
            return;
        }
        if (!preview && branchAdmin && BranchProductionSection.publicApplicationPath(path)) {
            chain.doFilter(input, output);
            return;
        }
        if (!preview && !productionRequest && "true".equals(request.getParameter("inlineEditorAdmin")) && request.getParameter("branchId")!=null) {
            inlineEditor(request,response,chain,path); return;
        }
        if (!preview && (user == null || !user.isAdmin())) {
            chain.doFilter(input, output);
            return;
        }
        String destination = request.getHeader("Sec-Fetch-Dest");
        boolean frame = "iframe".equals(destination) || "frame".equals(destination);
        boolean adminScreen = !preview && branchAdmin && (navigation || frame && "GET".equals(request.getMethod()))
            && BranchProductionSection.adminDocumentPath(path);
        if(!preview && !adminScreen && !productionRequest && !isAdminPath(path)) {
            Object selection=session.getAttribute(BranchSelection.SESSION_KEY);
            if(selection instanceof BranchSelection.Selected selected && selected.branchId()>0) {
                Object domain=session.getAttribute("preview.editorDomainName");
                boolean sameDomain=domain==null||domain instanceof String name && selected.domainId()==BranchDomains.id(name);
                boolean legacy=(path.matches("(?i).*\\.(jsp|action|do)$")||path.equals("/XhrFileUpload")) && !isPublicPageLink(path,request);
                if(sameDomain && !path.equals("/logoff.do") && (legacy||!Set.of("GET","HEAD").contains(request.getMethod()))) {
                    response.sendError(409,"This write path is unavailable in a publishing branch; select production");return;
                }
            }
            chain.doFilter(input,output);return;
        }
        // Login and static administration resources remain available even after a package closes.
        boolean resource=Set.of("GET","HEAD").contains(request.getMethod()) && (path.startsWith("/admin/v9/dist/")||path.startsWith("/admin/skins/")||path.startsWith("/admin/scripts/")||path.startsWith("/admin/css/")||path.startsWith("/admin/images/")||path.startsWith("/admin/elFinder/")||path.startsWith("/admin/codemirror/")||path.startsWith("/admin/webpages/page-builder/")||path.startsWith("/components/_common/admin/"))
            && path.matches("(?i).*\\.(css|js|png|jpg|gif|svg|woff2?|ttf|ico|map)$");
        if(!preview && (resource || path.startsWith("/admin/logon/") || path.startsWith("/admin/logoff"))) {chain.doFilter(input,output);return;}
        RequestBean bean=SetCharacterEncodingFilter.getCurrentRequestBean();
        String oldDomain=bean==null?null:bean.getDomain(),oldUrl=bean==null?null:bean.getUrl();
        Object oldPreview=request.getAttribute("isPreview");
        var previousPage = BranchProductionSection.currentSelection();
        try {
            if(preview) {
                if(!PathFilter.checkAdminRequest(request,response))return;
                user=access.requireUser(request);
            }
            BranchContext context;
            String logicalPath=path;
            if(preview) {
                if(!Set.of("GET","HEAD").contains(request.getMethod())) {response.sendError(405,"Preview is read only");return;}
                String rest=path.substring(BranchPreviewUrls.PREFIX.length());int slash=rest.indexOf('/');
                if(slash<1) {response.sendError(404);return;}
                long id=Long.parseLong(rest.substring(0,slash));
                try {logicalPath=URLDecoder.decode(rest.substring(slash).replace("+","%2B"),StandardCharsets.UTF_8);}
                catch(IllegalArgumentException ex) {response.sendError(400,"Invalid preview path");return;}
                if(!BranchPreviewUrls.allowed(logicalPath) || logicalPath.contains("%")) {response.sendError(404);return;}
                for(String parameter:Set.of("historyid","historyId","forward","forwarddoccompare","inlineEditorAdmin","docid","docId"))
                    if(request.getParameter(parameter)!=null) {response.sendError(400,"Unsupported preview parameter");return;}
                String domain=BranchSelection.previewDomain(id,user);
                context=new BranchContext(id,domain,user.getUserId(),BranchContext.Mode.PREVIEW);
            } else {
                String domain=access.requireDomain(request,user);
                boolean management=(path.equals("/admin/rest/branches")||path.startsWith("/admin/rest/branches/"))||path.equals("/admin/rest/refresher")
                    || ("GET".equals(request.getMethod()) && path.matches("/admin/rest/properties/[a-zA-Z_-]+/"));
                String explicit=request.getHeader(BranchSelection.ID_HEADER),expectedDomain=request.getHeader(BranchSelection.DOMAIN_HEADER);
                if (explicit==null && request.getParameter("branchId")!=null) {
                    explicit=request.getParameter("branchId"); expectedDomain=request.getParameter("branchDomain");
                }
                boolean clearClosed = navigation && !productionRequest && explicit == null && expectedDomain == null
                    && BranchProductionSection.isBranchPage(path);
                context=management?BranchContext.live(domain,user.getUserId()):BranchSelection.selected(request,user,domain,clearClosed);
                if (!management && (productionRequest || adminScreen && context.isBranch())) {
                    boolean confirmed = document != null && document.authorize(request, context, true);
                    boolean inherited = source != null && source.authorize(request, context, false);
                    boolean branchScreen = adminScreen && BranchProductionSection.supportsBranch(path)
                        && !confirmed && !(inherited && (frame || "0".equals(explicit)));
                    if (!branchScreen && (confirmed || inherited)) {
                        access.requireUser(request);
                        if (explicit != null && (!Integer.toString(context.domainId()).equals(expectedDomain)
                                || !("0".equals(explicit) || Long.toString(context.branchId()).equals(explicit)))) {
                            response.sendError(409, "Screen context changed; reload the screen");
                            return;
                        }
                        // Editor popup URLs can carry the selected branch. Their verified data context is now LIVE.
                        if (explicit != null) explicit = "0";
                        // Give child documents their own URL so subsequent native requests retain the same context.
                        if (!confirmed && (frame || isHtmlDocument(request)) && BranchProductionSection.adminDocumentPath(path)) {
                            response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                            response.setHeader("Location", BranchProductionSection.continuation(request, context));
                            response.setHeader("Cache-Control", "private, no-store");
                            return;
                        }
                        response.setHeader("Referrer-Policy", "same-origin");
                        response.setHeader("Cache-Control", "private, no-store");
                        if (path.startsWith("/admin/rest/") && !Set.of("GET", "HEAD").contains(request.getMethod()))
                            access.requireCsrf(request);
                        RequestBean.setAttribute(BranchProductionSection.ATTRIBUTE, context);
                        context = BranchContext.live(domain, user.getUserId());
                    } else if (!branchScreen && adminScreen && context.isBranch()) {
                        access.requireUser(request);
                        showProductionDialog(request, response, context);
                        return;
                    } else if (document != null && adminScreen && !context.isBranch()) {
                        // A copied/bookmarked URL also opens normally when the user is already in production.
                        String query = BranchProductionSection.originalQuery(request);
                        response.sendRedirect(request.getRequestURI() + (query.isEmpty() ? "" : "?" + query));
                        return;
                    } else if (!branchScreen && productionRequest) {
                        response.sendError(409, "Production confirmation expired; reopen the screen");
                        return;
                    }
                }
                boolean restRequiresScreen = path.startsWith("/admin/rest/")
                        && (context.isBranch() || !ApiTokenAuthFilter.isAuthenticatedByTokenOnly(request));
                if(!management && (explicit!=null || expectedDomain!=null)) {
                    try {BranchContexts.requireSameSelection(context,Long.parseLong(explicit),expectedDomain);}
                    catch(RuntimeException ex) {response.sendError(409,"Branch context changed; reload the screen");return;}
                } else if(!management && BranchProductionSection.currentSelection() == null && (restRequiresScreen || path.startsWith("/admin/upload/")
                        || path.equals("/admin/web-pages/upload/") || path.equals("/admin/elfinder-connector/") || path.equals("/admin/webpages/preview/"))
                        && (context.isBranch() || !Set.of("GET","HEAD").contains(request.getMethod()))) {
                    response.sendError(409,"Missing screen branch context; reload the screen");return;
                }
                if(context.isBranch()) {
                    access.requireUser(request);
                    boolean readPost="POST".equals(request.getMethod()) && Set.of("/admin/rest/groups/tree/tree","/admin/rest/news/news-list/tree","/admin/rest/news/news-list/convertIdsToNamePair","/admin/elFinder/gethash.jsp").contains(path);
                    if(!Set.of("GET","HEAD").contains(request.getMethod())&&!readPost&&!supportedPost(path)) {response.sendError(409,"This write is unavailable in a publishing branch");return;}
                    if(BranchProductionSection.needsConfirmation(path)) {response.sendError(409,"This function is unavailable in a publishing branch; select production");return;}
                }
            }
            boolean editorPreview = context.isBranch() && path.equals("/admin/webpages/preview/");
            if (editorPreview) context = new BranchContext(context.branchId(), context.domainName(), context.userId(), BranchContext.Mode.PREVIEW);
            try(BranchContexts.Scope ignored=BranchContexts.open(request,context)) {
                if(bean!=null) {bean.setDomain(context.domainName());bean.setUrl(logicalPath);}
                if(preview) {
                    request.setAttribute("isPreview",Boolean.TRUE);request.setAttribute(BranchPreviewUrls.LOGICAL_PATH,logicalPath);
                    BranchPreviewResponseWrapper wrapped=new BranchPreviewResponseWrapper(response,request);
                    HttpServletRequest logical=new LogicalRequest(request,logicalPath);
                    if(logicalPath.startsWith("/images/")||logicalPath.startsWith("/files/")) BranchFiles.serve(logicalPath,logical,wrapped);
                    else if(logicalPath.startsWith("/thumb/")) BranchFiles.thumbnail(logicalPath.substring(6),logical,wrapped);
                    else {
                        boolean asset=logicalPath.startsWith("/templates/")||logicalPath.startsWith("/components/")||logicalPath.startsWith("/apps/");
                        if(!asset && BranchReadService.docIdFromUrl(logicalPath)<=0) wrapped.sendError(404);
                        else chain.doFilter(logical,wrapped);
                    }
                    wrapped.finish();
                } else if (editorPreview) {
                    BranchPreviewResponseWrapper wrapped = new BranchPreviewResponseWrapper(response, request);
                    chain.doFilter(request, wrapped);
                    wrapped.finish();
                } else chain.doFilter(request,response);
            }
        } catch(ResponseStatusException ex) {if(!response.isCommitted())response.sendError(ex.getStatusCode().value(),ex.getReason());}
          catch(BranchDecision.StaleChoiceException ex) {if(!response.isCommitted())response.sendError(409,"Preview choice is stale; compare again");}
          catch(NumberFormatException ex) {response.sendError(404);}
        finally {
            RequestBean.setAttribute(BranchProductionSection.ATTRIBUTE, previousPage);
            if(bean!=null){bean.setDomain(oldDomain);bean.setUrl(oldUrl);}
            if(oldPreview==null)request.removeAttribute("isPreview");else request.setAttribute("isPreview",oldPreview);
            request.removeAttribute(BranchPreviewUrls.LOGICAL_PATH);
        }
    }

    private static boolean isAdminPath(String path) {
        return path.contains("/admin/") || path.equals("/components/_common/javascript/page_functions.js.jsp");
    }

    /** LIVE administration still validates its domain. Old WORK forms must not fall through to LIVE writes. */
    private static boolean requiresContextCheck(HttpServletRequest request, HttpSession session, Identity user, String path) {
        if (user != null && user.isAdmin() && isAdminPath(path)) return true;
        if (session != null && session.getAttribute(BranchSelection.SESSION_KEY) instanceof BranchSelection.Selected selected
                && selected.branchId() > 0) return true;
        return request.getHeader(BranchSelection.ID_HEADER) != null || request.getParameter("branchId") != null;
    }

    private static boolean allowWhenDisabled(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean preview = path.startsWith(BranchPreviewUrls.PREFIX);
        if (preview || "dialog".equals(request.getHeader("X-WebJET-Production-Navigation"))) {
            if (preview) setPreviewHeaders(response);
            response.sendError(404);
            return false;
        }
        if (isStaleWrite(request, path) && !BranchProductionSection.publicApplicationPath(path)) {
            response.sendError(409, "Branches are disabled; sign in again before editing production");
            return false;
        }
        return true;
    }

    private static boolean isStaleWrite(HttpServletRequest request, String path) {
        if (path.equals("/logoff.do") || path.startsWith("/admin/logoff") || path.startsWith("/admin/logon/")) return false;
        String branchId = request.getParameter("branchId");
        boolean frameContext = branchId != null && ("true".equals(request.getParameter("inlineEditorAdmin"))
            || path.startsWith("/apps/news/admin/") || path.startsWith("/admin/v9/webpages/component/"));
        String method = request.getMethod();
        if (!frameContext && ("GET".equals(method) || "HEAD".equals(method)) && !path.startsWith("/admin/rest/")
                && !path.equals("/XhrFileUpload") && !Strings.CI.endsWithAny(path, ".jsp", ".action", ".do")) return false;

        String header = request.getHeader(BranchSelection.ID_HEADER);
        if ((frameContext && !"0".equals(branchId)) || (header != null && !"0".equals(header))) return true;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(BranchSelection.SESSION_KEY) instanceof BranchSelection.Selected selected
                && selected.branchId() > 0) return true;
        if (!BranchProductionSection.hasProductionMarker(request)) return false;
        return BranchProductionSection.document(request) != null || (BranchProductionSection.referrer(request) != null
            && (!isDocumentNavigation(request) || "0".equals(branchId)));
    }

    private static void setPreviewHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
    }

    private static void showProductionDialog(HttpServletRequest request, HttpServletResponse response, BranchContext selection)
            throws ServletException, IOException {
        request.setAttribute("productionBranchName", BranchSelection.requireBranch(selection.branchId(), selection.domainName()).getName());
        request.setAttribute("productionContinuation", BranchProductionSection.continuation(request, selection));
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Referrer-Policy", "same-origin");
        request.getRequestDispatcher("/WEB-INF/branch-production.jsp").forward(request, response);
    }

    private static boolean isDocumentNavigation(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && isHtmlDocument(request);
    }

    private static boolean isHtmlDocument(HttpServletRequest request) {
        String destination = request.getHeader("Sec-Fetch-Dest");
        if (destination != null) return "document".equals(destination);
        String accept = request.getHeader("Accept");
        return request.getHeader("X-Requested-With") == null && accept != null && accept.contains("text/html");
    }

    private static boolean isPublicPageLink(String path, HttpServletRequest request) {
        if (!"/showdoc.do".equals(path) || !Set.of("GET", "HEAD").contains(request.getMethod())) return false;
        String[] ids = request.getParameterValues("docid");
        return request.getParameterMap().size() == 1 && ids != null && ids.length == 1 && ids[0].matches("[1-9][0-9]*");
    }

    /** The ordinary Page Builder frame reads WORK; it never falls through to a production frame. */
    private void inlineEditor(HttpServletRequest request,HttpServletResponse response,FilterChain chain,String path) throws IOException,ServletException {
        RequestBean bean=SetCharacterEncodingFilter.getCurrentRequestBean();
        String oldDomain=bean==null?null:bean.getDomain();
        Object oldPreview=request.getAttribute("isPreview");
        setPreviewHeaders(response);
        try {
            Identity user=access.requireUser(request);
            String domain=access.requireDomain(request,user);
            BranchContext selected=BranchSelection.selected(request,user,domain);
            try { BranchContexts.requireSameSelection(selected,Long.parseLong(request.getParameter("branchId")),request.getParameter("branchDomain")); }
            catch(RuntimeException ex) { response.sendError(409,"Branch context changed; reload the editor"); return; }
            if (!selected.isBranch() || !Set.of("GET","HEAD").contains(request.getMethod()) || !BranchPreviewUrls.allowed(path)) { response.sendError(409); return; }
            if (request.getParameter("historyid")!=null || request.getParameter("historyId")!=null || request.getParameter("forward")!=null) { response.sendError(409); return; }
            try(BranchContexts.Scope ignored=BranchContexts.open(request,selected)) {
                if(bean!=null) bean.setDomain(domain);
                var doc=BranchView.current().document(BranchView.current().docIdFromUrl(path),false);
                if(doc==null) {response.sendError(404);return;}
                if(!sk.iway.iwcm.editor.service.EditorService.hasPagePermission(user,doc)) {response.sendError(403);return;}
                request.setAttribute("isPreview",Boolean.TRUE);
                chain.doFilter(request,response);
            }
        } catch(ResponseStatusException ex) { response.sendError(ex.getStatusCode().value(),ex.getReason()); }
        finally {
            if(bean!=null) bean.setDomain(oldDomain);
            if(oldPreview==null) request.removeAttribute("isPreview");else request.setAttribute("isPreview",oldPreview);
        }
    }

    private static boolean supportedPost(String path) {
        return Set.of("/admin/rest/web-pages/editor","/admin/rest/groups/editor","/admin/rest/news/news-list/editor",
            "/admin/rest/media/editor","/admin/rest/web-pages/preview/","/admin/upload/chunk","/admin/upload/overwrite","/admin/upload/keepboth","/admin/upload/skipkey",
            "/admin/web-pages/upload/","/admin/elfinder-connector/",
            "/admin/v9/webpages/component/","/admin/rest/components","/admin/rest/branch-content/revert",
            "/admin/rest/web-pages/deletion-preview","/admin/rest/groups/deletion-preview","/admin/rest/web-pages/http-headers",
            "/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp").contains(path);
    }

    private static final class LogicalRequest extends HttpServletRequestWrapper {
        private final String path;
        LogicalRequest(HttpServletRequest request,String path){super(request);this.path=path;}
        @Override public String getRequestURI(){return getDispatcherType()==DispatcherType.REQUEST?getContextPath()+path:super.getRequestURI();}
        @Override public String getServletPath(){return getDispatcherType()==DispatcherType.REQUEST?path:super.getServletPath();}
        @Override public String getPathInfo(){return getDispatcherType()==DispatcherType.REQUEST?null:super.getPathInfo();}
        @Override public StringBuffer getRequestURL(){
            if(getDispatcherType()!=DispatcherType.REQUEST)return super.getRequestURL();
            StringBuffer url=new StringBuffer(getScheme()+"://"+getServerName());if(getServerPort()!=80&&getServerPort()!=443)url.append(':').append(getServerPort());return url.append(getRequestURI());
        }
        @Override public long getDateHeader(String name){return name.equalsIgnoreCase("If-Modified-Since")?-1:super.getDateHeader(name);}
        @Override public String getHeader(String name){return name.equalsIgnoreCase("If-None-Match")||name.equalsIgnoreCase("If-Modified-Since")?null:super.getHeader(name);}
    }
}
