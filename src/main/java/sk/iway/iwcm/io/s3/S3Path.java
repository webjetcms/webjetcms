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

    /**
     * Normalizes separators and rejects traversal instead of resolving it across storage roots.
     *
     * @param path CMS path beginning with a forward slash
     * @return absolute path with forward slashes, no repeated separators and no trailing slash except for the root
     * @throws IllegalArgumentException if the path is null, relative, contains a NUL character or has dot segments
     */
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

    /**
     * Checks configured S3 directory boundaries while excluding temporary and executable resources.
     *
     * @param path absolute CMS path; may be null
     * @return whether S3 is enabled and the normalized path belongs to an eligible configured root
     * @throws IllegalArgumentException if enabled storage receives an invalid path or root configuration
     */
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

    /**
     * Normalizes the nonblank directory roots in {@code s3UseForDirs}.
     *
     * @return configured roots without trailing slashes
     * @throws IllegalArgumentException if a root is invalid or selects the entire CMS root
     */
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

    /**
     * Resolves the domain namespace, using the shared namespace for shared files and their thumbnails.
     * Callers retain this value so a later request-context change cannot redirect a pending write.
     *
     * @param path normalized absolute CMS path
     * @return storage namespace with forward slashes and a trailing slash
     * @throws IllegalStateException if multidomain storage has no explicit domain context
     */
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

    /**
     * Tests an exact directory match or a descendant separated by a forward slash.
     *
     * @param path normalized path to test
     * @param root directory path without a trailing slash
     * @return whether the path is the directory itself or one of its descendants
     */
    public static boolean within(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }
}
