package sk.iway.iwcm.doc;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Validates the private staging namespace and its corresponding public paths. */
public final class PlannedAssetPaths {
    public static final String ROOT = "/files/protected/planned-changes/";

    private PlannedAssetPaths() { }

    public static boolean containsPlannedPath(String value) {
        if (value == null) return false;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT)
                .contains("/files/protected/planned-changes");
        } catch (IllegalArgumentException exception) {
            return value.toLowerCase(Locale.ROOT).contains("planned-changes");
        }
    }

    /** Returns a decoded, traversal-free local path, also usable for directory ACL checks. */
    public static String localPath(String value) {
        String path = URI.create(value.replace(" ", "%20")).getRawPath();
        if (path == null) throw new IllegalArgumentException("Missing asset path");
        path = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
        if (!path.startsWith("/") || path.contains("\\") || path.contains("%") || path.contains(";") ||
            path.contains("//") || path.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IllegalArgumentException("Invalid asset path");
        }
        for (String part : path.split("/")) {
            if (part.equals(".") || part.equals("..") || part.endsWith(".") || part.endsWith(" ")) {
                throw new IllegalArgumentException("Invalid asset path segment");
            }
        }
        return path;
    }

    public static int docId(String path) {
        if (!path.startsWith(ROOT)) throw new IllegalArgumentException("Invalid planned asset root");
        int slash = path.indexOf('/', ROOT.length());
        if (slash < 0) throw new IllegalArgumentException("Missing planned asset owner");
        int id = Integer.parseInt(path.substring(ROOT.length(), slash));
        if (id < 1 || !path.startsWith(ROOT + id + "/")) throw new IllegalArgumentException("Invalid planned asset owner");
        return id;
    }

    public static String target(String source, int owner) {
        String path = localPath(source);
        if (docId(path) != owner) throw new IllegalArgumentException("Asset belongs to another planned page");
        String target = path.substring((ROOT + owner).length());
        validateTarget(target);
        return target;
    }

    /** Returns a validated public target, leaving ordinary paths unchanged. */
    public static String removePlannedPrefix(String value) {
        if (!containsPlannedPath(value)) return value;
        String path = localPath(value);
        return target(path, docId(path));
    }

    public static void validateTarget(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if ((!path.startsWith("/images/") && !path.startsWith("/files/")) ||
            lower.contains("/protected/") || lower.contains("/web-inf/") || lower.contains("/meta-inf/") ||
            lower.matches(".*\\.(?:jsp|jspx|php|asp|aspx|html?|shtml|js|mjs|css|class|jar|war)$")) {
            throw new IllegalArgumentException("Unsupported public asset destination");
        }
    }

    public static String source(int docId, String target) {
        validateTarget(localPath(target));
        return ROOT + docId + target;
    }
}
