package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
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

    public static boolean canEdit(Identity user, DocDetails doc) {
        return user != null && user.isAdmin() && user.isEnabledItem("menuWebpages") && user.isEnabledItem("pageSave") &&
            doc != null && !GroupsDB.getInstance().isInTrash(doc.getGroupId()) && EditorDB.isPageEditable(user, new EditorForm(doc));
    }

    public static DocDetails requirePage(int docId, Identity user) {
        DocDetails doc = Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class).findById(masterId(docId));
        GroupDetails group = doc == null ? null : GroupsDB.getInstance().getGroup(doc.getGroupId());
        if (!canEdit(user, doc) || group == null || !group.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName())) {
            throw new IllegalArgumentException("Access to planned page denied");
        }
        return doc;
    }

    public static void privateHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        response.setDateHeader("Expires", 0);
    }

    /** Must run before existence checks, conditional responses and derivative cache reads. */
    public static boolean check(String path, HttpServletRequest request, HttpServletResponse response) {
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

    /** Fails closed even when history lookup or authorization raises an exception. */
    public static boolean historyPreview(int historyId, HttpServletRequest request, HttpServletResponse response) {
        try {
            DocHistory history = Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class).findById(Long.valueOf(historyId)).orElse(null);
            if (history == null) { response.setStatus(404); return false; }
            if (enabled() || Boolean.TRUE.equals(history.getPublicable()) || Integer.valueOf(2).equals(history.getSyncStatus()) || PlannedChangesService.hasPrivateReferences(history)) {
                privateHeaders(response);
                DocDetails page = requirePage(history.getDocId(), UsersDB.getCurrentUser(request));
                request.setAttribute("isPreview", Boolean.TRUE);
                if (Boolean.TRUE.equals(history.getPublicable()) && Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class).isPlannedMediaInitialized(page.getDocId())) request.setAttribute(PREVIEW_DOC, page.getDocId());
            }
            return true;
        } catch (RuntimeException exception) {
            privateHeaders(response);
            response.setStatus(404);
            return false;
        }
    }

    public static boolean previewFor(HttpServletRequest request, int docId) {
        Object plannedDoc = request.getAttribute(PREVIEW_DOC);
        return plannedDoc != null && Integer.valueOf(masterId(docId)).equals(plannedDoc);
    }
}
