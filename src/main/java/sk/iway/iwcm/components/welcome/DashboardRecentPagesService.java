package sk.iway.iwcm.components.welcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

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

/** Recent edits filtered against current page permissions and domain before limiting the list. */
@Service
public class DashboardRecentPagesService {
    private final DashboardSettingsRepository.ConnectionFactory connections;

    public DashboardRecentPagesService() {
        this(DBPool::getConnection);
    }

    DashboardRecentPagesService(DashboardSettingsRepository.ConnectionFactory connections) {
        this.connections = connections;
    }

    /**
     * Lists the current user's most recently edited distinct pages in the active domain.
     * Historical rows cannot grant access to a page that was moved or had its permissions changed.
     */
    public List<DocDetailsDto> getRecentPages(Identity user, String domain, int size) {
        if (size < 1 || size > 20) throw new IllegalArgumentException("Recent page count must be between 1 and 20");
        if (!user.isEnabledItem("menuWebpages")) throw new org.springframework.security.access.AccessDeniedException("Web page access is required");
        List<DocDetailsDto> pages = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        String sql = "SELECT doc_id, save_date FROM documents_history WHERE author_id=? ORDER BY save_date DESC, history_id DESC";
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getUserId());
            statement.setFetchSize(100);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next() && pages.size() < size) {
                    int docId = rows.getInt("doc_id");
                    if (!visited.add(docId)) continue;
                    DocDetails current = DocDB.getInstance().getBasicDocDetails(docId, false);
                    if (!isAccessible(current, user, domain)) continue;
                    if (current.getVirtualPath() != null && current.getVirtualPath().startsWith("/files/")) continue;

                    DocDetailsDto dto = new DocDetailsDto();
                    dto.setDocId(docId);
                    dto.setGroupId(current.getGroupId());
                    dto.setTitle(Tools.replace(current.getTitle(), "&#47;", "/"));
                    dto.setVirtualPath(current.getVirtualPath());
                    dto.setFullPath(current.getFullPath());
                    java.sql.Timestamp saved = rows.getTimestamp("save_date");
                    dto.setSaveDate(saved == null ? "" : Tools.formatDateTimeSeconds(saved.getTime()));
                    dto.setCreatedByUserId(user.getUserId());
                    dto.setCreatedByUserName(user.getFullName());
                    dto.setCreatedByUserLogin(user.getLogin());
                    pages.add(dto);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load recent dashboard pages", exception);
        }
        return pages;
    }

    /** Checks the current location and editing permissions of a page without using historical scope. */
    static boolean isAccessible(DocDetails currentDoc, Identity user, String domain) {
        if (currentDoc == null || user == null || !user.isEnabledItem("menuWebpages") || Tools.isEmpty(domain)) return false;
        GroupDetails group = GroupsDB.getInstance().getGroup(currentDoc.getGroupId());
        if (group == null || group.isHiddenInAdmin() || !domain.equalsIgnoreCase(group.getDomainName())) return false;
        String path = group.getFullPath();
        String trashPath = GroupsTreeService.getTrashDirPath();
        if (path != null && Tools.isNotEmpty(trashPath) && path.contains(trashPath)) return false;
        return EditorDB.isPageEditable(user, new EditorForm(currentDoc));
    }
}
