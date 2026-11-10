package sk.iway.iwcm.branch;

import java.io.Serializable;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.RequestBean;

/** Identifies WORK screens and carries explicit production consent through a single browser document and its requests. */
public final class BranchProductionSection {
    static final String ATTRIBUTE = BranchProductionSection.class.getName();
    private static final String PARAMETER = "wjProductionPage";
    private static final Pattern TOKEN = Pattern.compile("(?:^|&)" + PARAMETER + "=([a-f0-9-]{36})(?=&|$)");

    // Only screens which actually read/write WORK belong here. Unknown administration screens use the production dialog.
    private static final Set<String> BRANCH_PAGES = Set.of(
        "/admin/v9/", "/admin/v9/webpages/web-pages-list/", "/admin/v9/webpages/media/",
        "/admin/v9/webpages/component/", "/apps/news/admin/", "/admin/v9/files/index/",
        "/admin/v9/files/dialog/", "/admin/v9/files/wj_link/", "/admin/v9/files/wj_image/"
    );

    private BranchProductionSection() { }

    public static BranchContext currentSelection() {
        return (BranchContext) RequestBean.getAttribute(ATTRIBUTE);
    }

    public static boolean needsConfirmation(String href) {
        if (href == null) return false;
        String path = href.split("[?#]", 2)[0];
        return adminDocumentPath(path) && !supportsBranch(path);
    }

    static boolean supportsBranch(String path) {
        return BRANCH_PAGES.contains(path.endsWith("/") ? path : path + "/");
    }

    static boolean adminDocumentPath(String path) {
        return (path.startsWith("/admin/") || path.startsWith("/components/") || path.startsWith("/apps/"))
            && !path.startsWith("/admin/rest/");
    }

    private record Grant(String path, long branch, String domain, int user, String method, boolean opened) implements Serializable {
        boolean matches(String document, BranchContext context) {
            return context.isBranch() && path.equals(document) && branch == context.branchId()
                && domain.equals(context.domainName()) && user == context.userId();
        }
    }

    private static final class Grants extends LinkedHashMap<String, Grant> {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Grant> eldest) { return size() > 128; }
    }

    /** A relative continuation keeps the original path and query (including repeated parameters) on the same origin. */
    static String continuation(HttpServletRequest request, BranchContext selection) {
        String key = UUID.randomUUID().toString();
        HttpSession session = request.getSession();
        synchronized (session) {
            Grants grants = (Grants) session.getAttribute(ATTRIBUTE);
            if (grants == null) grants = new Grants();
            grants.put(key, new Grant(request.getRequestURI(), selection.branchId(), selection.domainName(), selection.userId(), request.getMethod(), false));
            session.setAttribute(ATTRIBUTE, grants);
        }
        String preserved = originalQuery(request);
        return "?" + (preserved.isEmpty() ? "" : preserved + "&") + PARAMETER + "=" + key;
    }

    static String originalQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? "" : String.join("&", java.util.Arrays.stream(query.split("&"))
            .filter(part -> !part.isEmpty() && !part.startsWith(PARAMETER + "=")).toList());
    }

    record Source(String path, String key) {
        boolean authorize(HttpServletRequest request, BranchContext context, boolean openDocument) {
            HttpSession session = request.getSession(false);
            if (session == null) return false;
            synchronized (session) {
                Grants grants = (Grants) session.getAttribute(ATTRIBUTE);
                Grant grant = grants == null ? null : grants.get(key);
                if (grant == null || !grant.matches(path, context)) return false;
                if (!openDocument) return grant.opened();
                if (grant.opened() || !grant.method().equals(request.getMethod())) return false;
                grants.put(key, new Grant(path, grant.branch(), grant.domain(), grant.user(), grant.method(), true));
                session.setAttribute(ATTRIBUTE, grants);
                return true;
            }
        }
    }

    private static Source source(String path, String query) {
        if (query == null) return null;
        Matcher match = TOKEN.matcher(query);
        return match.find() ? new Source(path, match.group(1)) : null;
    }

    static Source document(HttpServletRequest request) {
        return source(request.getRequestURI(), request.getQueryString());
    }

    /** Native forms, uploads, legacy frames and XMLHttpRequest all carry the document's same-origin Referer. */
    static Source referrer(HttpServletRequest request) {
        String value = request.getHeader("Referer");
        if (value == null || !value.contains(PARAMETER + "=")) return null;
        try {
            URI ref = URI.create(value);
            URI target = URI.create(request.getRequestURL().toString());
            if (!target.resolve("/").equals(ref.resolve("/"))) return null;
            return source(ref.getRawPath(), ref.getRawQuery());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
