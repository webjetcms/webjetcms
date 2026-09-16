package sk.iway.iwcm.stat.heat_map;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.stat.StatisticsMode;

/** Captures immutable click cookies on ordinary requests and supplies a cache-safe tracker bootstrap. */
public final class HeatMapTrackingService {
    public static final String PREVIEW_ATTRIBUTE = "heatMapPreview";
    public static final String COOKIE_PREFIX = "wj_hm_";
    public static final int MAX_PENDING_EVENTS = 16;
    public static final int MAX_COOKIE_BYTES = 2048;
    public static final long MAX_AGE_SECONDS = 86400;
    private static final Pattern EVENT_ID = Pattern.compile("[a-f0-9]{32}");
    private static final String RECEIVED_ATTRIBUTE = HeatMapTrackingService.class.getName() + ".received";

    private HeatMapTrackingService() { }

    public static boolean isPreview(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(PREVIEW_ATTRIBUTE));
    }

    public static boolean isEnabledForRequest(HttpServletRequest request) {
        if (isPreview(request) || Boolean.TRUE.equals(request.getAttribute("isPreview"))
                || request.getAttribute("is404") != null || request.getParameter("historyid") != null
                || request.getParameter("isPdfVersion") != null || request.getHeader("dmail") != null
                || "true".equals(request.getParameter("inlineEditorAdmin"))
                || request.getParameter("notemp") != null) return false;
        int docId = Tools.getIntValue(String.valueOf(request.getAttribute("doc_id")), -1);
        return docId > 0 && StatisticsMode.forDocument(docId) == StatisticsMode.HEATMAP
                && !BrowserDetector.getInstance(request).isAmp();
    }

    /** Returns stable page/configuration metadata; consent is evaluated in the browser and on receipt. */
    public static String bootstrap(HttpServletRequest request) {
        if (!isEnabledForRequest(request)) return "";
        int docId = Tools.getIntValue(String.valueOf(request.getAttribute("doc_id")), -1);
        return "<script id=\"wj-heatmap-tracker\" defer src=\"/components/stat/heat-map-tracker.js\" data-doc-id=\"" + docId
                + "\" data-cookie-name=\"" + escapeAttribute(Constants.getString("gdprCookieName"))
                + "\" data-allow-all=\"" + Constants.getBoolean("gdprAllowAllCookies")
                + "\" data-before-consent=\"" + Constants.getBoolean("gdprInsertAllScriptsBeforeAccept")
                + "\" data-decline-name=\"" + escapeAttribute(Constants.getString("disableCookiesCookieName"))
                + "\" data-decline-value=\"" + escapeAttribute(Constants.getString("disableCookiesCookieValue")) + "\"></script>";
    }

    public static String insertBeforeBodyEnd(String html, String script) {
        if (html == null || script.isEmpty() || html.contains("id=\"wj-heatmap-tracker\"")
                || html.contains("id='wj-heatmap-tracker'")) return html;
        int index = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return index < 0 ? html : html.substring(0, index) + script + html.substring(index);
    }

    /** Strictly parses bounded, versioned event data before any lookup or database work. */
    public static HeatMapEvent parseCookie(String name, String value, long nowSeconds) {
        if (name == null || !name.startsWith(COOKIE_PREFIX) || value == null || value.length() > 128) return null;
        String eventId = name.substring(COOKIE_PREFIX.length());
        if (!EVENT_ID.matcher(eventId).matches()) return null;
        String[] values = value.split("\\.", -1);
        if (values.length != 6 || !"v1".equals(values[0])) return null;
        try {
            for (int i = 1; i < values.length; i++) {
                if (!values[i].matches("[0-9]{1,10}")) return null;
            }
            int docId = Integer.parseInt(values[1]);
            int width = Integer.parseInt(values[2]);
            long time = Long.parseLong(values[3]);
            int x = Integer.parseInt(values[4]);
            int y = Integer.parseInt(values[5]);
            if (docId < 1 || width < 1 || width > 16384 || x > 1_000_000 || y > 1_000_000
                    || time < nowSeconds - MAX_AGE_SECONDS || time > nowSeconds + 300) return null;
            return new HeatMapEvent(eventId, docId, width, time, x, y);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Consumes cookies independently of the destination page, including existing asynchronous statistics calls. */
    public static void receiveCookies(HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute(RECEIVED_ATTRIBUTE) != null || request.getCookies() == null || isPreview(request)) return;
        String path = request.getRequestURI();
        if (path.startsWith("/admin/") || path.matches("/apps/[^/]+/admin(?:/.*)?")
                || request.getParameter("historyid") != null || HeatMapPreviewRequest.isPreviewReferrer(request)) return;
        boolean hasEvents = false;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().startsWith(COOKIE_PREFIX)) {
                hasEvents = true;
                break;
            }
        }
        if (!hasEvents) return;
        request.setAttribute(RECEIVED_ATTRIBUTE, Boolean.TRUE);
        boolean allowed = Tools.canSetCookie("statisticke", request.getCookies())
                && !"none".equals(Constants.getString("statMode")) && Constants.getBoolean("statEnableClickTracking")
                && BrowserDetector.isStatIpAllowedFast(request);
        String declineName = Constants.getString("disableCookiesCookieName");
        String declineValue = Constants.getString("disableCookiesCookieValue");
        if (Tools.isNotEmpty(declineName) && Tools.isNotEmpty(declineValue)
                && declineValue.equals(Tools.getCookieValue(request.getCookies(), declineName, ""))) allowed = false;
        long now = System.currentTimeMillis() / 1000;
        record Pending(Cookie cookie, HeatMapEvent event) { }
        List<Pending> pending = new ArrayList<>();
        for (Cookie cookie : request.getCookies()) {
            if (!cookie.getName().startsWith(COOKIE_PREFIX)) continue;
            pending.add(new Pending(cookie, parseCookie(cookie.getName(), cookie.getValue(), now)));
        }
        // Concurrent tabs can briefly exceed the client limit. Retain their newest events on receipt too.
        Collections.reverse(pending);
        pending.sort(Comparator.comparingLong((Pending item) -> item.event() == null
                ? Long.MIN_VALUE : item.event().epochSeconds()).reversed());
        int count = 0;
        int bytes = 0;
        for (Pending item : pending) {
            Cookie cookie = item.cookie();
            HeatMapEvent event = item.event();
            try {
                int eventBytes = cookie.getName().length() + (cookie.getValue() == null ? 0 : cookie.getValue().length()) + 2;
                if (allowed && event != null && count < MAX_PENDING_EVENTS && bytes + eventBytes <= MAX_COOKIE_BYTES) {
                    count++;
                    bytes += eventBytes;
                    DocDetails doc = DocDB.getInstance().getBasicDocDetails(event.docId(), false);
                    GroupDetails group = doc == null ? null : GroupsDB.getInstance().getGroup(doc.getGroupId());
                    String domain = Tools.getServerName(request);
                    if (group != null && group.getDomainName().equalsIgnoreCase(domain)
                            && doc.isAvailable() && StatisticsMode.forDocument(doc) == StatisticsMode.HEATMAP) {
                        HeatMapStorage.record(event, group.getDomainName().toLowerCase(Locale.ROOT));
                    }
                }
                Cookie expired = new Cookie(cookie.getName(), "");
                expired.setPath("/");
                expired.setMaxAge(0);
                expired.setSecure(Tools.isSecure(request));
                expired.setAttribute("SameSite", "Lax");
                response.addCookie(expired);
            } catch (RuntimeException ex) {
                Logger.error(HeatMapTrackingService.class, ex);
            }
        }
    }

    private static String escapeAttribute(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
