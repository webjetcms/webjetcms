package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;

/** Common authorization for private originals, derivatives, history and editor file operations. */
public final class PlannedAccess {
    public static final String PREVIEW_DOC = "planned.preview.docId";

    private PlannedAccess() { }

    public static boolean enabled() { return Constants.getBoolean("plannedPublishingEnabled"); }

    public static int masterId(int docId) {
        int master = MultigroupMappingDB.getMasterDocId(docId);
        return master > 0 ? master : docId;
    }

    public static void privateHeaders(HttpServletResponse response) {
        PathFilter.setNoCacheHeaders(response);
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
    }

    /** Must run before existence checks, conditional responses and derivative cache reads. */
    public static boolean isPathAccessible(String path, HttpServletRequest request, HttpServletResponse response) {
        if (!PlannedAssetPaths.containsPlannedPath(path)) return true;
        privateHeaders(response);
        Identity user = UsersDB.getCurrentUser(request);
        if (user != null && user.isAdmin()) {
            try {
                String localPath = PlannedAssetPaths.localPath(path);
                if (localPath.startsWith("/thumb/")) localPath = localPath.substring(6);
                if (user.isFolderWritable(localPath.substring(0, localPath.lastIndexOf('/') + 1))) return true;
            } catch (IllegalArgumentException exception) {
                // Invalid URLs receive the same response as inaccessible files.
            }
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

    /** Marks loaded history as a preview and selects its pending media set when initialized. */
    public static void historyPreview(DocDetails history, HttpServletRequest request, HttpServletResponse response) {
        privateHeaders(response);
        request.setAttribute("isPreview", Boolean.TRUE);
        int docId = masterId(history.getDocId());
        if (history.isPublicable() && Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class).isPlannedMediaInitialized(docId)) {
            request.setAttribute(PREVIEW_DOC, docId);
        }
    }

    public static boolean isPlannedPreview(HttpServletRequest request, int docId) {
        Object plannedDoc = request.getAttribute(PREVIEW_DOC);
        return plannedDoc != null && Integer.valueOf(masterId(docId)).equals(plannedDoc);
    }
}
