package sk.iway.iwcm.stat.heat_map;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;

/** Limits a preview forward to the server-selected page and identifies its asynchronous statistics requests. */
public final class HeatMapPreviewRequest extends HttpServletRequestWrapper {

    public static final String PREVIEW_PATH = "/admin/rest/stat/heat-map/preview";
    private final String docId;

    public HeatMapPreviewRequest(HttpServletRequest request, int docId) {
        super(request);
        this.docId = Integer.toString(docId);
    }

    @Override
    public String getParameter(String name) {
        return "docid".equals(name) ? docId : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return "docid".equals(name) ? new String[] { docId } : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.singletonMap("docid", new String[] { docId });
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(Collections.singleton("docid"));
    }

    @Override
    public String getQueryString() {
        return "docid=" + docId;
    }

    /**
     * Recognizes asynchronous statistics loads initiated by an authenticated preview.
     * This only suppresses statistics; it never authorizes access to preview content.
     */
    public static boolean isPreviewReferrer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) return false;
        try {
            URI uri = URI.create(referer);
            if (!(request.getContextPath() + PREVIEW_PATH).equals(uri.getPath())
                    || uri.getHost() == null || !uri.getHost().equalsIgnoreCase(Tools.getServerName(request, false))) {
                return false;
            }
            Identity user = UsersDB.getCurrentUser(request);
            return user != null && user.isAdmin() && user.isEnabledItem("cmp_stat");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
