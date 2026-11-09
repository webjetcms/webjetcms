package sk.iway.iwcm.branch;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;

/** Establishes the verified content context after authentication and before virtual routing. */
public final class BranchFilter implements Filter {
    private final BranchAccess access=new BranchAccess();

    @Override public void doFilter(ServletRequest input,ServletResponse output,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest request=(HttpServletRequest)input;HttpServletResponse response=(HttpServletResponse)output;
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean preview=path.startsWith(BranchPreviewUrls.PREFIX);
        if(preview) {response.setHeader("Cache-Control","private, no-store, max-age=0");response.setHeader("X-Robots-Tag","noindex, nofollow, noarchive");}
        if(!Constants.getBoolean("branchesEnabled")) {
            Object selected=request.getSession(false)==null?null:request.getSession(false).getAttribute(BranchSelection.SESSION_KEY);
            boolean frameContext=request.getParameter("branchId")!=null && ("true".equals(request.getParameter("inlineEditorAdmin")) || path.startsWith("/apps/news/admin/") || path.startsWith("/admin/v9/webpages/component/"));
            boolean staleBranch=(frameContext && !"0".equals(request.getParameter("branchId")))
                ||(selected instanceof BranchSelection.Selected value&&value.branchId()>0)
                ||(request.getHeader(BranchSelection.ID_HEADER)!=null&&!"0".equals(request.getHeader(BranchSelection.ID_HEADER)));
            boolean authenticationPath=path.equals("/logoff.do")||path.startsWith("/admin/logoff")||path.startsWith("/admin/logon/");
            if(preview)response.sendError(404);
            else if(staleBranch && !authenticationPath && (frameContext || path.startsWith("/admin/rest/")||path.matches("(?i).*\\.(jsp|action|do)$")||path.equals("/XhrFileUpload")||!Set.of("GET","HEAD").contains(request.getMethod())))response.sendError(409,"Branches are disabled; sign in again before editing production");
            else chain.doFilter(input,output);return;
        }
        if (!preview && "true".equals(request.getParameter("inlineEditorAdmin")) && request.getParameter("branchId")!=null) {
            inlineEditor(request,response,chain,path); return;
        }
        if(!preview && !path.startsWith("/admin/") && !path.contains("/admin/") && !path.equals("/components/_common/javascript/page_functions.js.jsp")) {
            Object selection=request.getSession(false)==null?null:request.getSession(false).getAttribute(BranchSelection.SESSION_KEY);
            Identity user=sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
            if(selection instanceof BranchSelection.Selected selected && selected.branchId()>0 && user!=null && user.isAdmin()) {
                Object domain=request.getSession(false).getAttribute("preview.editorDomainName");
                boolean sameDomain=domain==null||selected.domainName().equals(domain);
                boolean legacy=path.matches("(?i).*\\.(jsp|action|do)$")||path.equals("/XhrFileUpload");
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
        try {
            Identity user;
            if(preview) {
                if(!PathFilter.checkAdminRequest(request,response))return;
                user=access.requireUser(request);
            }
            else {
                user=sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
                if(user==null||!user.isAdmin()) {chain.doFilter(input,output);return;}
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
                boolean management=(path.equals("/admin/rest/branches")||path.equals("/admin/rest/branches/context")||path.matches("/admin/rest/branches/[0-9]+(?:/(?:preview-link|prepare|resolve|merge|resume|progress|file-version))?"))||path.equals("/admin/rest/refresher");
                context=management?BranchContext.live(domain,user.getUserId()):BranchSelection.selected(request,user,domain);
                String explicit=request.getHeader(BranchSelection.ID_HEADER),expectedDomain=request.getHeader(BranchSelection.DOMAIN_HEADER);
                if (explicit==null && request.getParameter("branchId")!=null) {
                    explicit=request.getParameter("branchId"); expectedDomain=request.getParameter("branchDomain");
                }
                if(!management && (explicit!=null || expectedDomain!=null)) {
                    try {BranchContexts.requireSameSelection(context,Long.parseLong(explicit),expectedDomain);}
                    catch(RuntimeException ex) {response.sendError(409,"Branch context changed; reload the screen");return;}
                } else if(!management && (path.startsWith("/admin/rest/") || path.startsWith("/admin/upload/")
                        || path.equals("/admin/web-pages/upload/") || path.equals("/admin/elfinder-connector/"))
                        && (context.isBranch() || !Set.of("GET","HEAD").contains(request.getMethod()))) {
                    response.sendError(409,"Missing screen branch context; reload the screen");return;
                }
                if(context.isBranch()) {
                    access.requireUser(request);
                    boolean readPost="POST".equals(request.getMethod()) && Set.of("/admin/rest/groups/tree/tree","/admin/rest/news/news-list/tree","/admin/rest/news/news-list/convertIdsToNamePair","/admin/elFinder/gethash.jsp").contains(path);
                    if(!Set.of("GET","HEAD").contains(request.getMethod())&&!readPost&&!supportedPost(path)) {response.sendError(409,"This write is unavailable in a publishing branch");return;}
                    if (path.equals("/admin/scripts/combine.jsp") && !Set.of("adminStandardJs","adminStandardCss","adminStandardCssWj9","adminJqueryJs","adminJqueryJs2","adminInlineJs","adminInlineCss").contains(java.util.Objects.toString(request.getParameter("set"),""))) {
                        response.sendError(409,"This script set is unavailable in a publishing branch"); return;
                    }
                    if(!isSupportedBranchAdminPath(path)) {response.sendError(409,"This function is unavailable in a publishing branch; select production");return;}
                }
            }
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
                } else chain.doFilter(request,response);
            }
        } catch(ResponseStatusException ex) {if(!response.isCommitted())response.sendError(ex.getStatusCode().value(),ex.getReason());}
          catch(BranchDecision.StaleChoiceException ex) {if(!response.isCommitted())response.sendError(409,"Preview choice is stale; compare again");}
          catch(NumberFormatException ex) {response.sendError(404);}
        finally {
            if(bean!=null){bean.setDomain(oldDomain);bean.setUrl(oldUrl);}
            if(oldPreview==null)request.removeAttribute("isPreview");else request.setAttribute("isPreview",oldPreview);
            request.removeAttribute(BranchPreviewUrls.LOGICAL_PATH);
        }
    }

    /** The ordinary Page Builder frame reads WORK; it never falls through to a production frame. */
    private void inlineEditor(HttpServletRequest request,HttpServletResponse response,FilterChain chain,String path) throws IOException,ServletException {
        RequestBean bean=SetCharacterEncodingFilter.getCurrentRequestBean();
        String oldDomain=bean==null?null:bean.getDomain();
        Object oldPreview=request.getAttribute("isPreview");
        response.setHeader("Cache-Control","private, no-store, max-age=0");
        response.setHeader("X-Robots-Tag","noindex, nofollow, noarchive");
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
            "/admin/rest/media/editor","/admin/upload/chunk","/admin/upload/overwrite","/admin/upload/keepboth","/admin/upload/skipkey",
            "/admin/web-pages/upload/","/admin/elfinder-connector/",
            "/admin/v9/webpages/component/","/admin/rest/components","/admin/rest/branch-content/revert",
            "/admin/rest/web-pages/deletion-preview","/admin/rest/groups/deletion-preview","/admin/rest/web-pages/http-headers",
            "/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp").contains(path);
    }

    private static boolean isSupportedBranchAdminPath(String path) {
        return path.startsWith("/admin/v9/webpages/")||path.startsWith("/apps/news/admin/")
            ||path.startsWith("/admin/rest/media")||path.startsWith("/admin/v9/files/")||path.equals("/admin/elfinder-connector/")
            ||path.equals("/admin/upload/chunk")||path.equals("/admin/upload/overwrite")||path.equals("/admin/upload/keepboth")||path.equals("/admin/upload/skipkey")
            ||path.equals("/admin/web-pages/upload/")||path.equals("/admin/elFinder/gethash.jsp")
            ||path.startsWith("/admin/rest/web-pages")||path.startsWith("/admin/rest/groups/tree")
            ||path.startsWith("/admin/rest/groups/parents/")||path.equals("/admin/rest/groups")||path.equals("/admin/rest/groups/editor")||path.matches("/admin/rest/groups/(-?[0-9]+|all|search/findByColumns)")||path.equals("/admin/rest/groups/deletion-preview")
            ||path.startsWith("/admin/rest/branch-content")||path.equals("/admin/rest/components")
            ||path.equals("/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/dialogs/webjetcomponet.jsp")
            ||path.equals("/admin/skins/webjet8/ckeditor/config.jsp")||path.equals("/admin/skins/webjet8/ckeditor/editor_magicline.jsp")
            ||path.equals("/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp")
            ||path.equals("/admin/scripts/combine.jsp")||path.equals("/admin/scripts/common.jsp")
            ||path.equals("/components/_common/javascript/page_functions.js.jsp")
            ||path.equals("/admin/webpages/webpages.js.jsp")||path.equals("/admin/webpages/page-builder/scripts/ninja-page-builder.js.jsp")
            ||path.equals("/admin/iframe_blank.jsp")||path.equals("/admin/rest/forms-list/regexps")
            ||path.startsWith("/admin/rest/news")||path.startsWith("/admin/rest/branches")
            ||path.equals("/admin/v9/")||path.startsWith("/admin/layout/");
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
