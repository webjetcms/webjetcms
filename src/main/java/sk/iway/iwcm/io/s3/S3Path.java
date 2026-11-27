package sk.iway.iwcm.io.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FilePathTools;

/** Maps CMS paths to S3 keys without depending on a physical external directory. */
public final class S3Path {
    private S3Path() { }

    /** Normalizes separators and rejects traversal instead of resolving it across storage roots. */
    public static String normalize(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("An absolute CMS path is required");
        }
        List<String> parts = new ArrayList<>();
        for (String part : path.replace('\\', '/').split("/+")) {
            if (".".equals(part) || "..".equals(part)) throw new IllegalArgumentException("Path traversal is not allowed");
            if (!part.isEmpty()) parts.add(part);
        }
        return "/" + String.join("/", parts);
    }

    /** Tests whole directory boundaries; temporary and executable resources always remain local. */
    public static boolean usesS3(String path) {
        if (!Constants.getBoolean("s3Enabled") || path == null) return false;
        String normalized = normalize(path);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (within(lower, "/admin") || within(lower, "/web-inf/tmp") || within(lower, "/meta-inf")
                || lower.endsWith(".jsp") || lower.endsWith(".jspx") || lower.endsWith(".class")) return false;
        for (String root : roots()) {
            if (within(normalized, root)) return true;
        }
        return false;
    }

    /** Returns configured roots, normalized in the same way as file paths. */
    public static List<String> roots() {
        List<String> roots = new ArrayList<>();
        for (String value : Constants.getArray("s3UseForDirs")) {
            if (!value.isBlank()) {
                String root = normalize(value.trim());
                if ("/".equals(root)) throw new IllegalArgumentException("S3 storage must use explicit directories");
                roots.add(root);
            }
        }
        return roots;
    }

    /** Captures the namespace once; streams must not resolve the domain again at close time. */
    public static String namespace(String path) {
        String cacheRoot = Constants.getString("thumbServletCacheDir");
        if (!cacheRoot.endsWith("/")) cacheRoot += "/";
        String domain = within(path, "/shared") || within(path, cacheRoot + "shared")
                ? "shared" : CloudToolsForCore.getDomainName();
        if (Constants.getBoolean("multiDomainEnabled") && (domain == null || "unknown".equals(domain))) {
            throw new IllegalStateException("S3 storage requires an explicit domain context");
        }
        return FilePathTools.getDomainFolderName(domain).replace('\\', '/') + "/";
    }

    /** Returns whether a path names the root itself or a descendant. */
    public static boolean within(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }
}
