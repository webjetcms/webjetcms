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

/** Search previews and shared page checks using current permissions and domain. */
public class DashboardRecentPagesService {
    private final DashboardSettingsRepository.ConnectionFactory connections;

    public DashboardRecentPagesService() {
        this(DBPool::getConnection);
    }

    DashboardRecentPagesService(DashboardSettingsRepository.ConnectionFactory connections) {
        this.connections = connections;
    }

    /** Loads current thumbnails and the latest save by any author for up to twenty authorized search results. */
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

    /** Only local image assets may be passed to the administration thumbnail endpoint. */
    static String previewImage(String path) {
        if (path == null || !(path.startsWith("/images/") || path.startsWith("/files/"))
                || path.indexOf('\\') >= 0 || path.indexOf('?') >= 0 || path.indexOf('#') >= 0
                || path.indexOf('%') >= 0 || path.contains("/../") || path.contains("/./")
                || path.chars().anyMatch(character -> Character.isISOControl(character))) return "";
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.matches(".*\\.(png|jpe?g|gif|webp|avif)") ? path : "";
    }

    /** Checks the current location and editing permissions of a page without using historical scope. */
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
