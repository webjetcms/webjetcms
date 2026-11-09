package sk.iway.iwcm.branch;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Converts logical content URLs at the transport boundary without changing stored content. */
public final class BranchPreviewUrls {
    public static final String PREFIX = "/admin/webpages/branch-preview/";
    public static final String LOGICAL_PATH = BranchPreviewUrls.class.getName()+".path";
    private BranchPreviewUrls() { }

    public static String toPreviewUrl(String value, HttpServletRequest request) {
        BranchContext context = BranchContexts.current(request);
        if (context.mode() != BranchContext.Mode.PREVIEW || value == null || value.isBlank() || value.startsWith("#")) return value;
        String prefix = request.getContextPath()+PREFIX+context.branchId();
        if (value.startsWith(prefix+"/") || value.startsWith(PREFIX)) return value;
        try {
            URI url = URI.create(value.replace(" ", "%20"));
            if (url.isAbsolute() && !"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme())) return value;
            if (url.getRawAuthority() != null && !context.domainName().equalsIgnoreCase(url.getHost())) return value;
            String path = (String)request.getAttribute(LOGICAL_PATH);
            if (path == null) path = "/";
            if(value.startsWith("?")) url=URI.create(path+value);
            url = URI.create("https://"+context.domainName()+path).resolve(url).normalize();
            if (!allowed(url.getPath())) return value;
            return prefix+url.getRawPath()+(url.getRawQuery()==null ? "" : "?"+url.getRawQuery())+(url.getRawFragment()==null ? "" : "#"+url.getRawFragment());
        } catch (IllegalArgumentException ex) { return value; }
    }

    static boolean allowed(String path) {
        if (path == null || !path.startsWith("/") || path.contains("\\") || path.contains("..") || path.contains(";") || path.startsWith("//")) return false;
        if (path.startsWith("/admin/") || path.startsWith("/WEB-INF/") || path.startsWith("/META-INF/") || path.startsWith(PREFIX)) return false;
        if (path.startsWith("/images/") || path.startsWith("/files/") || path.startsWith("/thumb/")) return true;
        if (path.startsWith("/templates/") || path.startsWith("/components/") || path.startsWith("/apps/"))
            return path.matches("(?i).*\\.(css|js|png|jpg|jpeg|gif|svg|webp|woff2?|ttf|ico)$");
        return !path.matches("(?i).*\\.(jsp|action|do|php|aspx)$");
    }

    public static String rewriteHtml(String html, HttpServletRequest request) {
        Document document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        Object originalPath=request.getAttribute(LOGICAL_PATH);
        Element base=document.selectFirst("base[href]");
        if(base!=null) {
            try {
                String path=originalPath instanceof String current?current:"/";
                URI resolved=URI.create("https://"+BranchContexts.current(request).domainName()+path).resolve(base.attr("href"));
                if(BranchContexts.current(request).domainName().equalsIgnoreCase(resolved.getHost())) request.setAttribute(LOGICAL_PATH,resolved.getPath());
            } catch(IllegalArgumentException ignored) { }
        }
        for (Element element : document.getAllElements()) {
            for (String attribute : new String[]{"href","src","poster","action","formaction","data-src","data-original","data-background","data-lazy-src"})
                if (element.hasAttr(attribute)) element.attr(attribute,("action".equals(attribute)||"formaction".equals(attribute))?toPreviewAction(element.attr(attribute),request):toPreviewUrl(element.attr(attribute),request));
            if (element.hasAttr("srcset")) {
                String[] candidates = element.attr("srcset").split(",");
                for (int i=0;i<candidates.length;i++) {
                    String[] parts=candidates[i].strip().split("\\s+",2);
                    candidates[i]=toPreviewUrl(parts[0],request)+(parts.length==2 ? " "+parts[1] : "");
                }
                element.attr("srcset",String.join(", ",candidates));
            }
            if (element.hasAttr("style")) element.attr("style",rewriteCss(element.attr("style"),request));
        }
        for (Element style:document.select("style")) style.html(rewriteCss(style.html(),request));
        for (Element button:document.select("form button, form input[type=submit], form input[type=image]")) button.attr("disabled","");
        BranchView view=BranchView.current();
        document.body().prependElement("aside").attr("role","status").attr("id","branch-preview-status")
            .text("Pracovná verzia: "+(view==null?BranchContexts.current(request).branchId():view.name())+" · Náhľad iba na čítanie"
                +(view==null?"":" · Novinky vyhodnotené k "+java.time.Instant.ofEpochMilli(view.previewAt()).atZone(java.time.ZoneId.systemDefault()))
                +(view!=null&&view.hasConflicts()?" · Nevyriešené konflikty zobrazujú pracovný obsah":""));
        if(originalPath==null)request.removeAttribute(LOGICAL_PATH);else request.setAttribute(LOGICAL_PATH,originalPath);
        return document.outerHtml();
    }

    public static String rewriteCss(String css, HttpServletRequest request) {
        var matcher=java.util.regex.Pattern.compile("url\\(\\s*(['\"]?)([^)'\"]+)\\1\\s*\\)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(css);
        StringBuffer output=new StringBuffer();
        while(matcher.find()) matcher.appendReplacement(output,java.util.regex.Matcher.quoteReplacement("url(\""+toPreviewUrl(matcher.group(2).strip(),request)+"\")"));
        matcher.appendTail(output);
        return output.toString();
    }

    private static String toPreviewAction(String value,HttpServletRequest request) {
        BranchContext context=BranchContexts.current(request);
        if(value.startsWith(request.getContextPath()+PREFIX))return value;
        try {
            String path=java.util.Objects.toString(request.getAttribute(LOGICAL_PATH),"/");
            URI url=URI.create("https://"+context.domainName()+path).resolve(value);
            if(!context.domainName().equalsIgnoreCase(url.getHost())||url.getPath().startsWith("/admin/"))return value;
            return request.getContextPath()+PREFIX+context.branchId()+url.getRawPath()+(url.getRawQuery()==null?"":"?"+url.getRawQuery());
        } catch(IllegalArgumentException ex) {return value;}
    }
}
