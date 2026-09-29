package sk.iway.iwcm.components.welcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;

/** Provides dashboard search previews and page access checks using current permissions and domain membership. */
public class DashboardRecentPagesService {
    private final DashboardSettingsRepository.ConnectionFactory connections;

    public DashboardRecentPagesService() {
        this(DBPool::getConnection);
    }

    DashboardRecentPagesService(DashboardSettingsRepository.ConnectionFactory connections) {
        this.connections = connections;
    }

    /**
     * Loads current thumbnail paths and the latest save by any author for accessible search results.
     * Pages outside the active domain or the user's editing permissions are omitted; pages without
     * a history save date use their creation date.
     *
     * @param user user whose web page access and editing permissions are checked
     * @param domain active domain name used to filter the pages
     * @param docIds non-null list of at most twenty page IDs in the requested order
     * @return previews keyed by page ID in request order, excluding inaccessible or missing pages
     * @throws IllegalArgumentException if more than twenty page IDs are requested
     * @throws org.springframework.security.access.AccessDeniedException if the user lacks web page access
     * @throws IllegalStateException if preview data cannot be loaded from the database
     */
    public Map<Integer, DocDetailsDto> getPagePreviews(Identity user, String domain, List<Integer> docIds) {
        if (docIds.size() > 20) throw new IllegalArgumentException("Page preview count must not exceed 20");
        if (user == null || !user.isEnabledItem("menuWebpages")) throw new org.springframework.security.access.AccessDeniedException("Web page access is required");
        Map<Integer, DocDetailsDto> pages = new LinkedHashMap<>();
        for (int docId : docIds) {
            DocDetails current = DocDB.getInstance().getBasicDocDetails(docId, false);
            if (!isAccessible(current, user, domain)) continue;
            DocDetailsDto page = new DocDetailsDto();
            page.setDocId(docId);
            page.setFullPath(current.getFullPath());
            pages.put(docId, page);
        }
        if (pages.isEmpty()) return pages;
        String placeholders = String.join(",", Collections.nCopies(pages.size(), "?"));
        String sql = "SELECT d.doc_id, d.perex_image, d.date_created,"
            + " (SELECT MAX(h.save_date) FROM documents_history h WHERE h.doc_id=d.doc_id) AS save_date"
            + " FROM documents d WHERE d.doc_id IN (" + placeholders + ")";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (int docId : pages.keySet()) statement.setInt(index++, docId);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    DocDetailsDto page = pages.get(rows.getInt("doc_id"));
                    page.setPerexImage(previewImage(rows.getString("perex_image")));
                    java.sql.Timestamp saved = rows.getTimestamp("save_date");
                    if (saved == null) saved = rows.getTimestamp("date_created");
                    page.setSaveDate(saved == null ? "" : Tools.formatDateTimeSeconds(saved.getTime()));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load dashboard page previews", exception);
        }
        return pages;
    }

    /**
     * Filters image paths for use with the administration thumbnail endpoint.
     * Only supported image extensions under {@code /images/} or {@code /files/} are accepted,
     * without traversal segments, encoded characters, query strings or fragments.
     *
     * @param path candidate image path, or {@code null}
     * @return the original path if accepted, otherwise an empty string
     */
    static String previewImage(String path) {
        if (path == null || !(path.startsWith("/images/") || path.startsWith("/files/"))
                || path.indexOf('\\') >= 0 || path.indexOf('?') >= 0 || path.indexOf('#') >= 0
                || path.indexOf('%') >= 0 || path.contains("/../") || path.contains("/./")
                || path.chars().anyMatch(character -> Character.isISOControl(character))) return "";
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.matches(".*\\.(png|jpe?g|gif|webp|avif)") ? path : "";
    }

    /**
     * Checks whether a page is editable in the active domain using its current location.
     * Missing pages and pages in hidden groups or the trash are excluded.
     *
     * @param currentDoc current page details, or {@code null}
     * @param user user whose web page access and editing permissions are checked, or {@code null}
     * @param domain required domain name; a missing or empty value denies access
     * @return {@code true} if the page passes the location and permission checks
     */
    public static boolean isAccessible(DocDetails currentDoc, Identity user, String domain) {
        if (currentDoc == null || user == null || !user.isEnabledItem("menuWebpages") || Tools.isEmpty(domain)) return false;
        GroupDetails group = GroupsDB.getInstance().getGroup(currentDoc.getGroupId());
        if (group == null || group.isHiddenInAdmin() || !domain.equalsIgnoreCase(group.getDomainName())) return false;
        String path = group.getFullPath();
        String trashPath = GroupsTreeService.getTrashDirPath();
        if (path != null && Tools.isNotEmpty(trashPath) && path.contains(trashPath)) return false;
        return EditorDB.isPageEditable(user, new EditorForm(currentDoc));
    }
}
