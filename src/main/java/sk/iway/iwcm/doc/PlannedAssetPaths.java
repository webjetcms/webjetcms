package sk.iway.iwcm.doc;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import sk.iway.iwcm.Tools;

/**
 * Maps files and folders between a page's protected planned directory and their public paths.
 *
 * <p>For example, page {@code 42} stages {@code /images/photo.webp} at
 * {@code /files/protected/planned-changes/42/images/photo.webp}. These helpers
 * rewrite paths and validate their syntax and destinations without accessing the filesystem.</p>
 */
public final class PlannedAssetPaths {
    public static final String ROOT = "/files/protected/planned-changes/";

    private PlannedAssetPaths() { }

    /**
     * Replaces a page's planned asset prefix with the public root in URLs or page content.
     *
     * <p>This literal replacement preserves surrounding HTML, URL encoding, query strings
     * and fragments. It does not validate paths or change prefixes belonging to other pages.</p>
     *
     * @param value URL or content containing planned asset references; may be {@code null}
     * @param docId page ID identifying the planned prefix to replace
     * @return content with each matching prefix replaced by {@code /}, or {@code null} for null input
     */
    public static String rewrite(String value, int docId) {
        return Tools.replace(value, ROOT + docId + "/", "/");
    }

    /**
     * Detects a planned-directory reference anywhere in a URL or text.
     *
     * <p>The check decodes URL escapes and ignores case. If decoding fails, it checks
     * for {@code planned-changes} in the original text so malformed references still
     * receive access checks. A match does not mean the path or page ID is valid.</p>
     *
     * @param value URL or text to inspect; may be {@code null}
     * @return {@code true} if a planned-directory reference is detected; {@code false} for null input
     */
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

    /**
     * Extracts and decodes a root-relative path while rejecting unsafe path syntax.
     *
     * <p>Query strings and fragments are discarded, URL escapes are decoded as UTF-8,
     * and literal {@code +} characters are preserved. The path must start with {@code /}
     * and must not contain traversal segments, backslashes, repeated slashes, semicolons,
     * remaining percent signs, control characters, or segments ending in a dot or space.</p>
     *
     * <p>A URL's scheme and authority are ignored. This method does not verify the origin,
     * require a planned prefix, check permissions or inspect the filesystem.</p>
     *
     * @param value non-null path or URL; literal spaces are accepted
     * @return decoded root-relative path, suitable for directory permission checks
     * @throws IllegalArgumentException if the URI is malformed, has no path, or has unsafe path syntax
     */
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

    /**
     * Extracts the page ID immediately after the protected planned-directory prefix.
     *
     * <p>For example, {@code /files/protected/planned-changes/42/images/photo.webp}
     * returns {@code 42}. The ID must be positive, written without leading zeros
     * or a plus sign, and followed by {@code /}. The remaining path is not validated.</p>
     *
     * @param path non-null decoded local path starting with {@link #ROOT}
     * @return ID of the page owning the planned directory
     * @throws IllegalArgumentException if the prefix is missing or the page ID is missing,
     *         malformed, non-positive, outside the integer range, or not followed by a slash
     */
    private static int parseDocIdFromPath(String path) {
        if (!path.startsWith(ROOT)) throw new IllegalArgumentException("Invalid planned asset root");
        int slash = path.indexOf('/', ROOT.length());
        if (slash < 0) throw new IllegalArgumentException("Missing planned asset owner");
        int id = Integer.parseInt(path.substring(ROOT.length(), slash));
        if (id < 1 || !path.startsWith(ROOT + id + "/")) throw new IllegalArgumentException("Invalid planned asset owner");
        return id;
    }

    /**
     * Resolves a planned file or folder to the public path used when publishing its page.
     *
     * <p>For example, {@code /files/protected/planned-changes/42/images/photo.webp}
     * becomes {@code /images/photo.webp} when {@code docId} is {@code 42}.
     * The source is decoded and checked by {@link #localPath(String)}, its page ID
     * must match {@code docId}, and the resulting public path must pass
     * {@link #validatePublicPath(String)}. No files are copied or modified.</p>
     *
     * @param plannedPath non-null planned asset path or URL
     * @param docId ID of the page whose planned directory must contain the asset, not a user ID
     * @return decoded public path without the planned prefix, query string or fragment
     * @throws IllegalArgumentException if the source path is invalid, belongs to another page,
     *         or maps to an unsupported public destination
     */
    public static String getPublicPath(String plannedPath, int docId) {
        String path = localPath(plannedPath);
        if (parseDocIdFromPath(path) != docId) throw new IllegalArgumentException("Asset belongs to another planned page");
        String publicPath = path.substring((ROOT + docId).length());
        validatePublicPath(publicPath);
        return publicPath;
    }

    /**
     * Converts a planned file or folder reference to its validated public path.
     *
     * <p>The owning page ID is read from the path. Planned references are decoded and
     * lose their URL authority, query string and fragment. Values without a planned
     * reference are returned unchanged, without validation.</p>
     *
     * @param value path or URL to convert; may be {@code null}
     * @return validated public path for a planned reference, or the original value otherwise
     * @throws IllegalArgumentException if a detected planned reference has an invalid path,
     *         page ID or public destination
     */
    public static String removePlannedPrefix(String value) {
        if (!containsPlannedPath(value)) return value;
        String path = localPath(value);
        return getPublicPath(path, parseDocIdFromPath(path));
    }

    /**
     * Checks whether a public destination is allowed for a planned file or folder.
     *
     * <p>The path must start with {@code /images/} or {@code /files/}. Protected
     * and application-internal directories are rejected, as are blocked file
     * suffixes such as {@code .jsp}, {@code .html}, {@code .js}, {@code .css},
     * {@code .jar} and {@code .war}. For example, {@code /files/catalog.pdf} is
     * allowed, while {@code /files/protected/catalog.pdf} is rejected.</p>
     *
     * <p>This check does not decode the path, detect traversal, check permissions
     * or inspect the filesystem. Call {@link #localPath(String)} first to validate
     * the path syntax.</p>
     *
     * @param path non-null decoded public destination, with the planned prefix already removed
     * @throws IllegalArgumentException if the destination directory or file suffix is not allowed
     */
    private static void validatePublicPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if ((!path.startsWith("/images/") && !path.startsWith("/files/")) ||
            lower.contains("/protected/") || lower.contains("/web-inf/") || lower.contains("/meta-inf/") ||
            lower.matches(".*\\.(?:jsp|jspx|php|asp|aspx|html?|shtml|js|mjs|css|class|jar|war)$")) {
            throw new IllegalArgumentException("Unsupported public asset destination");
        }
    }

    /**
     * Prepends a page's protected planned-directory prefix to a public file or folder path.
     *
     * <p>For example, page {@code 42} and {@code /files/catalog.pdf} produce
     * {@code /files/protected/planned-changes/42/files/catalog.pdf}. The decoded target
     * is validated, but the original target is appended unchanged, preserving its URL
     * encoding, query string and fragment. No directories are created.</p>
     *
     * @param docId page ID to place in the prefix; expected to be positive but not validated here
     * @param target non-null root-relative public file or folder path
     * @return protected planned path containing the page ID and the original target
     * @throws IllegalArgumentException if the target has invalid path syntax or an unsupported destination
     */
    public static String rewriteToProtectedFolder(int docId, String target) {
        validatePublicPath(localPath(target));
        return ROOT + docId + target;
    }
}
