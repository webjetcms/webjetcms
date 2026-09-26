package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;

/** Verifies that denied or cross-domain history cannot crowd authorized recent pages out of a preview. */
class DashboardRecentPagesServiceTest {
    @Test
    void checksCurrentDomainAndRightsBeforeApplyingTheRequestedLimit() throws Exception {
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, true, true, true, false);
        when(rows.getInt("doc_id")).thenReturn(11, 12, 13, 14);

        DocDB docs = mock(DocDB.class);
        GroupsDB groups = mock(GroupsDB.class);
        DocDetails otherDomain = page(11, 101, "Other domain");
        DocDetails denied = page(12, 102, "Denied");
        DocDetails first = page(13, 103, "Allowed first");
        DocDetails second = page(14, 103, "Allowed second");
        when(first.getPerexImage()).thenReturn("/images/news/cover.jpg");
        when(second.getPerexImage()).thenReturn("https://external.example/cover.jpg");
        when(docs.getBasicDocDetails(11, false)).thenReturn(otherDomain);
        when(docs.getBasicDocDetails(12, false)).thenReturn(denied);
        when(docs.getBasicDocDetails(13, false)).thenReturn(first);
        when(docs.getBasicDocDetails(14, false)).thenReturn(second);
        GroupDetails otherGroup = group("other.example", "/Other");
        GroupDetails deniedGroup = group("current.example", "/Denied");
        GroupDetails allowedGroup = group("current.example", "/Allowed");
        when(groups.getGroup(101)).thenReturn(otherGroup);
        when(groups.getGroup(102)).thenReturn(deniedGroup);
        when(groups.getGroup(103)).thenReturn(allowedGroup);

        try (MockedStatic<DocDB> docStatic = mockStatic(DocDB.class);
             MockedStatic<GroupsDB> groupStatic = mockStatic(GroupsDB.class);
             MockedStatic<GroupsTreeService> treeStatic = mockStatic(GroupsTreeService.class);
             MockedStatic<EditorDB> editorStatic = mockStatic(EditorDB.class)) {
            docStatic.when(DocDB::getInstance).thenReturn(docs);
            groupStatic.when(GroupsDB::getInstance).thenReturn(groups);
            treeStatic.when(GroupsTreeService::getTrashDirPath).thenReturn("/Trash");
            editorStatic.when(() -> EditorDB.isPageEditable(eq(user), any(EditorForm.class)))
                .thenAnswer(invocation -> ((EditorForm) invocation.getArgument(1)).getDocId() >= 13);

            List<DocDetailsDto> result = new DashboardRecentPagesService(() -> connection).getRecentPages(user, "current.example", 2);

            assertEquals(List.of(13, 14), result.stream().map(DocDetailsDto::getDocId).toList());
            assertEquals("/images/news/cover.jpg", result.get(0).getPerexImage());
            assertEquals("", result.get(1).getPerexImage());
            verify(statement).setInt(1, 7);
        }
    }

    @Test
    void deniedModuleDoesNotOpenTheDatabase() {
        Identity user = mock(Identity.class);
        DashboardSettingsRepository.ConnectionFactory connections = mock(DashboardSettingsRepository.ConnectionFactory.class);

        assertThrows(AccessDeniedException.class, () -> new DashboardRecentPagesService(connections).getRecentPages(user, "current.example", 6));
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
