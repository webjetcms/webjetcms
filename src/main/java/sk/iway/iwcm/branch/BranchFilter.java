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
            boolean staleBranch=(selected instanceof BranchSelection.Selected value&&value.branchId()>0)
                ||(request.getHeader(BranchSelection.ID_HEADER)!=null&&!"0".equals(request.getHeader(BranchSelection.ID_HEADER)));
            boolean authenticationPath=path.equals("/logoff.do")||path.startsWith("/admin/logoff")||path.startsWith("/admin/logon/");
            if(preview)response.sendError(404);
            else if(staleBranch && !authenticationPath && (path.startsWith("/admin/rest/")||path.matches("(?i).*\\.(jsp|action|do)$")||path.equals("/XhrFileUpload")||!Set.of("GET","HEAD").contains(request.getMethod())))response.sendError(409,"Branches are disabled; sign in again before editing production");
            else chain.doFilter(input,output);return;
        }
        if(!preview && !path.startsWith("/admin/") && !path.contains("/admin/")) {
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
        boolean resource=Set.of("GET","HEAD").contains(request.getMethod()) && (path.startsWith("/admin/v9/dist/")||path.startsWith("/admin/skins/")||path.startsWith("/admin/scripts/")||path.startsWith("/admin/css/")||path.startsWith("/admin/images/"))
            && path.matches("(?i).*\\.(css|js|png|jpg|gif|svg|woff2?|ttf|ico|map)$");
        if(!preview && (resource || path.startsWith("/admin/logon/") || path.startsWith("/admin/logoff"))) {chain.doFilter(input,output);return;}
        RequestBean bean=SetCharacterEncodingFilter.getCurrentRequestBean();
        String oldDomain=bean==null?null:bean.getDomain(),oldUrl=bean==null?null:bean.getUrl();
        Object oldPreview=request.getAttribute("isPreview");
        try {
            Identity user;
            if(preview) user=access.requireUser(request);
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
                boolean management=path.startsWith("/admin/rest/branches")||path.equals("/admin/rest/refresher");
                context=management?BranchContext.live(domain,user.getUserId()):BranchSelection.selected(request,user,domain);
                String explicit=request.getHeader(BranchSelection.ID_HEADER),expectedDomain=request.getHeader(BranchSelection.DOMAIN_HEADER);
                if(!management && (explicit!=null || expectedDomain!=null)) {
                    try {BranchContexts.requireSameSelection(context,Long.parseLong(explicit),expectedDomain);}
                    catch(RuntimeException ex) {response.sendError(409,"Branch context changed; reload the screen");return;}
                } else if(!management && path.startsWith("/admin/rest/") && (context.isBranch() || !Set.of("GET","HEAD").contains(request.getMethod()))) {
                    response.sendError(409,"Missing screen branch context; reload the screen");return;
                }
                if(context.isBranch()) {
                    access.requireUser(request);
                    boolean readPost="POST".equals(request.getMethod()) && Set.of("/admin/rest/groups/tree/tree","/admin/rest/news/news-list/tree","/admin/rest/news/news-list/convertIdsToNamePair").contains(path);
                    if(!Set.of("GET","HEAD").contains(request.getMethod())&&!readPost) {response.sendError(409,"Content editing is not supported in this preview");return;}
                    if(!readOnlyAdminPath(path)) {response.sendError(409,"This function is unavailable in a publishing branch; select production");return;}
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

    private static boolean readOnlyAdminPath(String path) {
        return path.startsWith("/admin/v9/webpages/")||path.startsWith("/apps/news/admin/")
            ||path.startsWith("/admin/rest/web-pages")||path.startsWith("/admin/rest/groups/tree")
            ||path.startsWith("/admin/rest/groups/parents/")
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
