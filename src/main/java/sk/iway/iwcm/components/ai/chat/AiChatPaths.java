package sk.iway.iwcm.components.ai.chat;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

import sk.iway.iwcm.Tools;

/** Keeps private chat files out of generic download and file-management routes. */
public final class AiChatPaths {
    // /files is shared through NFS in clustered installations; generic file routes must not expose this subtree.
    public static final String ROOT = "/files/protected/ai-chat";

    private AiChatPaths() { }

    public static boolean isPrivate(String value) {
        if (value == null) return false;
        String path = value.replace('\\', '/');
        try {
            for (int i = 0; i < 3 && path.contains("%"); i++) {
                path = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
            }
            path = Path.of(path.replace('\\', '/')).normalize().toString();
            path = path.replace('\\', '/').replaceAll(";[^/]*", "").replaceAll("[. ]+/", "/").replaceAll("[. ]+$", "");
            path = Path.of(path).normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return path.toLowerCase(Locale.ROOT).contains("ai-chat");
        }
        return path.endsWith(ROOT) || path.contains(ROOT + "/");
    }

    public static boolean isPrivate(File file) {
        if (isPrivate(file.getPath())) return true;
        try {
            return isPrivate(file.getCanonicalPath());
        } catch (IOException exception) {
            return true;
        }
    }

    public static boolean containsPrivateTree(File file) {
        if (isPrivate(file)) return true;
        try {
            File root = new File(Tools.getRealPath(ROOT));
            return root.exists() && root.getCanonicalFile().toPath().startsWith(file.getCanonicalFile().toPath());
        } catch (IOException exception) {
            return true;
        }
    }

    public static void requireGenericAccess(File file) {
        if (isPrivate(file)) throw new SecurityException("Private AI chat files require the owner-scoped chat API");
    }
}
