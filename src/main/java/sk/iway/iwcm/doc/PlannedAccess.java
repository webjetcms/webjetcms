package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UsersDB;

/**
 * Checks access to planned files and tracks which page's planned media a preview should use.
 *
 * <p>Planned originals and thumbnails require an administrator with write access to the
 * corresponding public folder. Preview helpers resolve multigroup copies to their master
 * page; authorization to view page history remains the caller's responsibility.</p>
 */
public final class PlannedAccess {
    public static final String PREVIEW_DOC = "planned.preview.docId";

    private PlannedAccess() { }

    /**
     * Reads whether private planned publishing is enabled for this installation.
     *
     * @return current value of the {@code plannedPublishingEnabled} configuration option
     */
    public static boolean enabled() { return Constants.getBoolean("plannedPublishingEnabled"); }

    /**
     * Resolves a multigroup copy to the master page that owns its planned media.
     *
     * @param docId page ID to resolve
     * @return positive master page ID when mapped, otherwise the supplied page ID
     */
    public static int masterId(int docId) {
        int master = MultigroupMappingDB.getMasterDocId(docId);
        return master > 0 ? master : docId;
    }

    /**
     * Checks whether the current user may read a planned original or thumbnail.
     *
     * <p>Ordinary paths pass through without changing the response. For a planned path,
     * caching is disabled and access requires an administrator with write permission
     * for the corresponding public folder. Thumbnail paths beginning with {@code /thumb/}
     * use the same folder check as the original file.</p>
     *
     * <p>Denied or malformed planned paths receive status {@code 404} without a response
     * body. Call this before checking file existence, handling conditional requests or
     * reading cached thumbnails, and stop processing when it returns {@code false}.
     * This check applies even when planned publishing is disabled.</p>
     *
     * @param path requested path or URL; null is treated as an ordinary path
     * @param request request used to obtain the current user
     * @param response response receiving cache headers and, when denied, status {@code 404}
     * @return {@code true} for an ordinary path or an authorized planned path
     */
    public static boolean isPathAccessible(String path, HttpServletRequest request, HttpServletResponse response) {
        if (!PlannedAssetPaths.containsPlannedPath(path)) return true;
        PathFilter.setNoCacheHeaders(response);
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

    /**
     * Marks an authorized history view as a preview and selects its planned media when available.
     *
     * <p>Disables response caching and sets the {@code isPreview} request attribute.
     * If the history is scheduled for publication and its master page has initialized
     * planned media, stores that master ID in {@link #PREVIEW_DOC}. Viewing an older
     * or unscheduled revision does not select the planned media set.</p>
     *
     * @param history loaded history revision whose access has already been checked
     * @param request request receiving the preview attributes
     * @param response response whose caching should be disabled
     */
    public static void historyPreview(DocDetails history, HttpServletRequest request, HttpServletResponse response) {
        PathFilter.setNoCacheHeaders(response);
        request.setAttribute("isPreview", Boolean.TRUE);
        int docId = masterId(history.getDocId());
        if (history.isPublicable() && Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class).isPlannedMediaInitialized(docId)) {
            request.setAttribute(PREVIEW_DOC, docId);
        }
    }

    /**
     * Checks whether this request has selected planned media for the given page.
     *
     * <p>Multigroup copies match the same master ID, so a preview can use the master's
     * planned media without selecting that media for unrelated pages.</p>
     *
     * @param request request carrying the {@link #PREVIEW_DOC} attribute
     * @param docId page ID whose media are being requested
     * @return {@code true} if the selected preview ID matches this page's master ID
     */
    public static boolean isPlannedPreview(HttpServletRequest request, int docId) {
        Object plannedDoc = request.getAttribute(PREVIEW_DOC);
        return plannedDoc != null && Integer.valueOf(masterId(docId)).equals(plannedDoc);
    }
}
