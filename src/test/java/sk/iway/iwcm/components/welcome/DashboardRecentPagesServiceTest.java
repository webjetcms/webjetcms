package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;

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

/** Verifies authorized search previews and shared page visibility and thumbnail checks. */
class DashboardRecentPagesServiceTest {
    /** Search previews batch authorized IDs and show the latest save, including changes by other authors. */
    @Test
    void searchPreviewsLoadImagesAndLatestSaveInOneQuery() throws Exception {
        Identity user = mock(Identity.class);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, true, false);
        when(rows.getInt("doc_id")).thenReturn(13, 14);
        when(rows.getString("perex_image")).thenReturn("/images/news/cover.jpg", "https://external.example/image.jpg");
        Timestamp saved = Timestamp.valueOf("2026-09-27 18:00:00");
        Timestamp created = Timestamp.valueOf("2026-09-25 10:00:00");
        when(rows.getTimestamp("save_date")).thenReturn(saved, (Timestamp) null);
        when(rows.getTimestamp("date_created")).thenReturn(created);
        DocDB docs = mock(DocDB.class);
        GroupsDB groups = mock(GroupsDB.class);
        doReturn(page(12, 102, "Other domain")).when(docs).getBasicDocDetails(12, false);
        doReturn(page(13, 103, "First")).when(docs).getBasicDocDetails(13, false);
        doReturn(page(14, 103, "Second")).when(docs).getBasicDocDetails(14, false);
        doReturn(page(15, 103, "Denied")).when(docs).getBasicDocDetails(15, false);
        doReturn(group("other.example", "/Other")).when(groups).getGroup(102);
        doReturn(group("current.example", "/Allowed")).when(groups).getGroup(103);
        try (MockedStatic<DocDB> docStatic = mockStatic(DocDB.class);
             MockedStatic<GroupsDB> groupStatic = mockStatic(GroupsDB.class);
             MockedStatic<GroupsTreeService> treeStatic = mockStatic(GroupsTreeService.class);
             MockedStatic<EditorDB> editorStatic = mockStatic(EditorDB.class)) {
            docStatic.when(DocDB::getInstance).thenReturn(docs);
            groupStatic.when(GroupsDB::getInstance).thenReturn(groups);
            treeStatic.when(GroupsTreeService::getTrashDirPath).thenReturn("/Trash");
            editorStatic.when(() -> EditorDB.isPageEditable(eq(user), any(EditorForm.class)))
                .thenAnswer(invocation -> ((EditorForm) invocation.getArgument(1)).getDocId() != 15);
            Map<Integer, DocDetailsDto> result = new DashboardRecentPagesService(() -> connection)
                .getPagePreviews(user, "current.example", List.of(12, 13, 14, 15));
            assertEquals(List.of(13, 14), List.copyOf(result.keySet()));
            assertEquals("/First", result.get(13).getFullPath());
            assertEquals("/images/news/cover.jpg", result.get(13).getPerexImage());
            assertEquals("", result.get(14).getPerexImage());
            assertEquals(Tools.formatDateTimeSeconds(saved.getTime()), result.get(13).getSaveDate());
            assertEquals(Tools.formatDateTimeSeconds(created.getTime()), result.get(14).getSaveDate());
            verify(connection).prepareStatement(contains("MAX(h.save_date)"));
            verify(statement).setInt(1, 13);
            verify(statement).setInt(2, 14);
            verify(statement).executeQuery();
        }
    }

    /** Empty, oversized and unauthorized preview requests do not query the database. */
    @Test
    void searchPreviewLimitsAreCheckedBeforeQuerying() {
        Identity user = mock(Identity.class);
        DashboardSettingsRepository.ConnectionFactory connections = mock(DashboardSettingsRepository.ConnectionFactory.class);
        DashboardRecentPagesService service = new DashboardRecentPagesService(connections);
        assertThrows(AccessDeniedException.class, () -> service.getPagePreviews(user, "current.example", List.of(13)));
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.getPagePreviews(user, "current.example", java.util.Collections.nCopies(21, 13)));
        assertTrue(service.getPagePreviews(user, "current.example", List.of()).isEmpty());
        verifyNoInteractions(connections);
    }

    @Test
    void excludesDeletedHiddenAndTrashedPages() {
        Identity user = mock(Identity.class);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        GroupsDB groups = mock(GroupsDB.class);
        GroupDetails hidden = group("current.example", "/Hidden");
        GroupDetails trash = group("current.example", "/Trash/Old");
        when(hidden.isHiddenInAdmin()).thenReturn(true);
        when(groups.getGroup(101)).thenReturn(hidden);
        when(groups.getGroup(102)).thenReturn(trash);

        try (MockedStatic<GroupsDB> groupStatic = mockStatic(GroupsDB.class);
             MockedStatic<GroupsTreeService> treeStatic = mockStatic(GroupsTreeService.class);
             MockedStatic<EditorDB> editorStatic = mockStatic(EditorDB.class)) {
            groupStatic.when(GroupsDB::getInstance).thenReturn(groups);
            treeStatic.when(GroupsTreeService::getTrashDirPath).thenReturn("/Trash");

            assertFalse(DashboardRecentPagesService.isAccessible(null, user, "current.example"));
            assertFalse(DashboardRecentPagesService.isAccessible(page(1, 101, "Hidden"), user, "current.example"));
            assertFalse(DashboardRecentPagesService.isAccessible(page(2, 102, "Trash"), user, "current.example"));
            editorStatic.verifyNoInteractions();
        }
    }

    /** Prevents external fetches and non-image paths from being used as dashboard thumbnails. */
    @Test
    void previewImagesOnlyUseLocalRasterAssets() {
        assertEquals("/images/news/cover.JPG", DashboardRecentPagesService.previewImage("/images/news/cover.JPG"));
        assertEquals("/files/article image.webp", DashboardRecentPagesService.previewImage("/files/article image.webp"));
        for (String value : List.of("//external.example/image.jpg", "https://external.example/image.jpg", "javascript:alert(1)",
                "/images/icon.svg", "/images/../admin/page.jpg", "/images/%2e%2e/admin.jpg", "/images/cover.jpg?redirect=true", "/images/cover.jpg\n")) {
            assertEquals("", DashboardRecentPagesService.previewImage(value), value);
        }
    }

    private static DocDetails page(int id, int groupId, String title) {
        DocDetails page = mock(DocDetails.class);
        when(page.getDocId()).thenReturn(id);
        when(page.getGroupId()).thenReturn(groupId);
        when(page.getTitle()).thenReturn(title);
        when(page.getFullPath()).thenReturn("/" + title);
        return page;
    }

    private static GroupDetails group(String domain, String path) {
        GroupDetails group = mock(GroupDetails.class);
        when(group.getDomainName()).thenReturn(domain);
        when(group.getFullPath()).thenReturn(path);
        return group;
    }
}
