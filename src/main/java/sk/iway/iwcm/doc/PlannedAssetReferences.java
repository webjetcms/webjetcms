package sk.iway.iwcm.doc;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.parser.Parser;

/** Rewrites supported inline references without reserializing page-builder HTML or INCLUDE macros. */
public final class PlannedAssetReferences {
    private static final Pattern ATTRIBUTE = Pattern.compile("(?i)(\\b(?:src|href|srcset|poster|data-src|data-srcset|data-original|data-lazy-src|data-lazy-srcset)\\s*=\\s*)(?:([\"'])(.*?)\\2|([^\\s\"'=<>`]+))", Pattern.DOTALL);
    private static final Pattern CSS = Pattern.compile("(?i)(url\\(\\s*)([\"']?)([^)\"']+)\\2(\\s*\\))");
    private static final Pattern URL = Pattern.compile("(?:https?://[^/\\s\"'<>]+|//[^/\\s\"'<>]+)?/(?:thumb/)?files/protected/planned-changes/[^\\s\"'<>(),]+", Pattern.CASE_INSENSITIVE);
    private final int docId;
    private final String domain;
    private final String version;
    private final Map<String, String> files = new LinkedHashMap<>();

    public PlannedAssetReferences(int docId, String domain, String version) {
        this.docId = docId;
        this.domain = domain;
        this.version = version;
    }

    public Map<String, String> getFiles() { return Map.copyOf(files); }

    /** Accepts HTML, inline CSS, or a single URL; unknown uses fail closed. */
    public String rewrite(String value) {
        if (value == null || !PlannedAssetPaths.containsPlannedPath(Parser.unescapeEntities(value, false))) return value;
        if (!value.contains("<") && value.trim().matches("(?i)(https?://|//|/).*")) return rewriteUrl(value.trim());
        Matcher attributes = ATTRIBUTE.matcher(value);
        StringBuffer result = new StringBuffer();
        while (attributes.find()) {
            String quote = attributes.group(2) == null ? "" : attributes.group(2);
            String rewritten = rewriteUrls(attributes.group(3) == null ? attributes.group(4) : attributes.group(3));
            attributes.appendReplacement(result, Matcher.quoteReplacement(attributes.group(1) + quote + rewritten + quote));
        }
        attributes.appendTail(result);
        Matcher css = CSS.matcher(result.toString());
        result = new StringBuffer();
        while (css.find()) {
            css.appendReplacement(result, Matcher.quoteReplacement(css.group(1) + css.group(2) + rewriteUrls(css.group(3)) + css.group(2) + css.group(4)));
        }
        css.appendTail(result);
        if (PlannedAssetPaths.containsPlannedPath(Parser.unescapeEntities(result.toString(), false))) {
            throw new IllegalArgumentException("Unsupported planned asset reference");
        }
        return result.toString();
    }

    private String rewriteUrls(String value) {
        Matcher urls = URL.matcher(value);
        StringBuffer result = new StringBuffer();
        while (urls.find()) urls.appendReplacement(result, Matcher.quoteReplacement(rewriteUrl(urls.group())));
        urls.appendTail(result);
        return result.toString();
    }

    private String rewriteUrl(String value) {
        boolean escaped = value.contains("&amp;");
        URI uri = URI.create(Parser.unescapeEntities(value, false));
        if (uri.getRawAuthority() != null && (!uri.getRawAuthority().equalsIgnoreCase(domain) || uri.getUserInfo() != null)) {
            throw new IllegalArgumentException("Planned asset URL must belong to the page domain");
        }
        if (uri.getScheme() != null && !"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Unsupported planned asset URL scheme");
        }
        String path = PlannedAssetPaths.localPath(value);
        boolean thumb = path.startsWith("/thumb/");
        String source = thumb ? path.substring(6) : path;
        String target = PlannedAssetPaths.target(source, docId);
        files.put(source, target);
        if (!uri.getRawPath().contains(PlannedAssetPaths.ROOT + docId + "/")) throw new IllegalArgumentException("Use the canonical planned asset URL");
        String rawPath = uri.getRawPath().replace(PlannedAssetPaths.ROOT + docId + "/", "/");
        String query = uri.getRawQuery();
        if (query != null) query = query.replaceAll("(^|&)v=[^&]*", "").replaceAll("^&|&$", "");
        query = (query == null || query.isEmpty() ? "" : query + "&") + "v=" + version;
        String result = (uri.getRawAuthority() == null ? "" : (uri.getScheme() == null ? "" : uri.getScheme() + ":") + "//" + uri.getRawAuthority()) + rawPath + "?" + query;
        if (uri.getRawFragment() != null) result += "#" + uri.getRawFragment();
        return escaped ? result.replace("&", "&amp;") : result;
    }
}
