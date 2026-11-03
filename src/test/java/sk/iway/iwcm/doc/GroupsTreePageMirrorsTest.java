package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.settings.AdminSettingsService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.doc.mirroring.dto.MirroringDTO;
import sk.iway.iwcm.doc.mirroring.rest.MirroringService;
import sk.iway.iwcm.i18n.Prop;

/** Covers page language links and diagnostics when the tree's basic page cache has no sync IDs. */
class GroupsTreePageMirrorsTest {
    /** Filters inaccessible, foreign-domain and deleted pages and reuses the request-local batch. */
    @Test
    void resolvesPageLinksWithoutCachedSyncIds() throws Exception {
        Identity user = mock(Identity.class);
        DocDB docs = mock(DocDB.class);
        GroupsDB groups = mock(GroupsDB.class);
        Prop prop = mock(Prop.class);
        try (MockedStatic<DocDB> docAccess = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groupAccess = mockStatic(GroupsDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<Prop> translations = mockStatic(Prop.class);
                MockedStatic<EditorService> permissions = mockStatic(EditorService.class);
                MockedStatic<GroupMirroringServiceV9> languages = mockStatic(GroupMirroringServiceV9.class);
                MockedStatic<MirroringService> diagnostics = mockStatic(MirroringService.class);
                MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class, (query, context) -> {
                    when(query.setSql(anyString())).thenReturn(query);
                    when(query.setParams(any(Object[].class))).thenReturn(query);
                    when(query.list(any())).thenAnswer(invocation -> {
                        Mapper<?> mapper = invocation.getArgument(0);
                        for (int id : new int[] { 11, 12, 13, 14, 15, 16 }) {
                            ResultSet row = mock(ResultSet.class);
                            when(row.getInt("source_id")).thenReturn(10);
                            when(row.getInt("doc_id")).thenReturn(id);
                            mapper.map(row);
                        }
                        return List.of();
                    });
                })) {
            docAccess.when(DocDB::getInstance).thenReturn(docs);
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            groupAccess.when(() -> GroupsDB.isGroupEditable(user, 100)).thenReturn(true);
            constants.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(true);
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            domains.when(CloudToolsForCore::getDomainName).thenReturn("autotest.example");
            translations.when(() -> Prop.getInstance("sk")).thenReturn(prop);
            when(prop.getText("config.trash_dir")).thenReturn("/System/Trash");

            GroupDetails group = group(100, "autotest.example", "/autotest");
            when(groups.getGroup(100)).thenReturn(group);
            languages.when(() -> GroupMirroringServiceV9.getLanguage(group)).thenReturn("EN");
            GroupDetails foreign = group(200, "foreign.example", "/autotest-foreign");
            GroupDetails trash = group(300, "autotest.example", "/System/Trash/autotest");
            GroupDetails czech = group(400, "autotest.example", "/autotest-czech");
            when(groups.getGroup(200)).thenReturn(foreign);
            when(groups.getGroup(300)).thenReturn(trash);
            when(groups.getGroup(400)).thenReturn(czech);
            groupAccess.when(() -> GroupsDB.isGroupEditable(user, 400)).thenReturn(true);
            languages.when(() -> GroupMirroringServiceV9.getLanguage(czech)).thenReturn("cz");

            DocDetails source = page(10, 100);
            permissions.when(() -> EditorService.hasPagePermission(user, source)).thenReturn(true);
            for (int id = 11; id <= 16; id++) {
                int groupId = switch (id) { case 13 -> 200; case 14 -> 300; case 16 -> 400; default -> 100; };
                DocDetails page = page(id, groupId);
                when(docs.getBasicDocDetails(id, false)).thenReturn(page);
                permissions.when(() -> EditorService.hasPagePermission(user, page)).thenReturn(id != 12);
            }
            MockHttpServletRequest request = request();
            GroupsTreeService service = new GroupsTreeService();
            DocumentsJsTreeItem node = new DocumentsJsTreeItem(source, 10);
            service.setMirroringLinks(List.of(node), user, "dt-tree-filter-system-trash", request);
            assertEquals(List.of(16, 11, 15), node.getMirrors().stream().map(DocumentsJsTreeItem.MirrorLink::docId).toList());
            assertEquals("cs", node.getMirrors().get(0).language());
            assertEquals("/autotest-page-11", node.getMirrors().get(1).path());

            DocumentsJsTreeItem reloaded = new DocumentsJsTreeItem(source, 10);
            service.setMirroringLinks(List.of(reloaded), user, "dt-tree-filter-system-trash", request);
            assertEquals(node.getMirrors(), reloaded.getMirrors());
            assertEquals(1, queries.constructed().size());

            // Diagnostics carry the mapping ID, which differs from both page and folder IDs.
            when(user.isEnabledItem("cmp_mirroring")).thenReturn(true);
            when(user.isEnabledItem("menuWebpages")).thenReturn(true);
            translations.when(() -> Prop.getInstance(request)).thenReturn(prop);
            MirroringDTO pageProblem = new MirroringDTO();
            pageProblem.setId(500L);
            pageProblem.setNestingWarn(true);
            MirroringDTO groupProblem = new MirroringDTO();
            groupProblem.setId(700L);
            groupProblem.setMultipleErr(true);
            groupProblem.setNestingWarn(true);
            group.setSyncId(700);
            diagnostics.when(() -> MirroringService.getGroupProblems(request)).thenReturn(Map.of(700, groupProblem));
            diagnostics.when(() -> MirroringService.getPageProblems(request, List.of(10))).thenReturn(Map.of(10, pageProblem));
            GroupsJsTreeItem folder = new GroupsJsTreeItem(group, user, true);
            service.setMirroringIcons(List.of(folder, node), user, "dt-tree", request);
            assertEquals(500, node.getMirroringProblem().syncId());
            assertEquals("ti ti-alert-triangle text-warning", node.getMirroringProblem().icon());
            assertEquals(700, folder.getMirroringProblem().syncId());
            assertEquals("ti ti-exclamation-circle text-danger", folder.getMirroringProblem().icon());
            service.setMirroringIcons(List.of(reloaded), user, "dt-tree", request);
            assertEquals(node.getMirroringProblem(), reloaded.getMirroringProblem());
            diagnostics.verify(() -> MirroringService.getPageProblems(request, List.of(10)), times(1));

            // Later branches in the same request must be checked, including pages with no problem.
            DocumentsJsTreeItem clean = new DocumentsJsTreeItem(page(20, 100), 10);
            permissions.when(() -> EditorService.hasPagePermission(user, clean.getDocDetails())).thenReturn(true);
            diagnostics.when(() -> MirroringService.getPageProblems(request, List.of(20))).thenReturn(Map.of());
            service.setMirroringIcons(List.of(reloaded, clean), user, "dt-tree", request);
            service.setMirroringIcons(List.of(clean), user, "dt-tree", request);
            assertNull(clean.getMirroringProblem());
            diagnostics.verify(() -> MirroringService.getPageProblems(request, List.of(20)), times(1));
            diagnostics.verify(() -> MirroringService.getPageProblems(request, List.of(10)), times(1));
            permissions.when(() -> EditorService.hasPagePermission(user, source)).thenReturn(false);
            DocumentsJsTreeItem forbidden = new DocumentsJsTreeItem(source, 10);
            service.setMirroringIcons(List.of(forbidden), user, "dt-tree", request);
            assertNull(forbidden.getMirroringProblem());
        }
    }

    /** Both the saved preference and the AJAX toggle stop all mirror lookups when switched off. */
    @Test
    void disabledSettingSkipsLinksAndDiagnostics() {
        Identity user = mock(Identity.class);
        when(user.isEnabledItem("cmp_mirroring")).thenReturn(true);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        GroupsTreeService service = new GroupsTreeService();
        DocumentsJsTreeItem page = new DocumentsJsTreeItem(page(10, 100), 10);
        try (MockedStatic<MirroringService> diagnostics = mockStatic(MirroringService.class);
                MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class);
                MockedConstruction<AdminSettingsService> settings = mockConstruction(AdminSettingsService.class, (setting, context) -> {
                    when(setting.getJsonBooleanValue("jstreeSettings_web-pages-list", "showMirrorings")).thenReturn(false);
                })) {
            MockHttpServletRequest initial = new MockHttpServletRequest();
            initial.setRequestURI("/admin/v9/webpages/web-pages-list/");
            service.setMirroringLinks(List.of(page), user, "dt-tree", initial);
            service.setMirroringIcons(List.of(page), user, "dt-tree", initial);
            assertEquals(2, settings.constructed().size());
            for (AdminSettingsService setting : settings.constructed()) {
                verify(setting).getJsonBooleanValue("jstreeSettings_web-pages-list", "showMirrorings");
                verifyNoMoreInteractions(setting);
            }

            MockHttpServletRequest ajax = request();
            ajax.setParameter("showMirrorings", "false");
            service.setMirroringLinks(List.of(page), user, "dt-tree", ajax);
            service.setMirroringIcons(List.of(page), user, "dt-tree", ajax);
            assertEquals(2, settings.constructed().size());
            diagnostics.verifyNoInteractions();
            assertTrue(queries.constructed().isEmpty());
            assertNull(page.getMirrors());
            assertNull(page.getMirroringProblem());
        }
    }

    /** Disabled mirroring and selector/system/trash modes never query or expose page links. */
    @Test
    void skipsDisabledAndNonMainTrees() {
        Identity user = mock(Identity.class);
        GroupsTreeService service = new GroupsTreeService();
        DocumentsJsTreeItem page = new DocumentsJsTreeItem(page(10, 100), 10);
        try (MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class)) {
            for (String mode : List.of("dt-tree-system", "dt-tree-trash", "select")) {
                service.setMirroringLinks(List.of(page), user, mode, request());
            }
            MockHttpServletRequest hidden = request();
            hidden.setParameter("showMirrorings", "false");
            service.setMirroringLinks(List.of(page), user, "dt-tree", hidden);
            when(user.isDisabledItem("cmp_mirroring")).thenReturn(true);
            service.setMirroringLinks(List.of(page), user, "dt-tree", request());
            assertNull(page.getMirrors());
            assertTrue(queries.constructed().isEmpty());
        }
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/rest/groups/tree");
        request.setParameter("showMirrorLinks", "true");
        request.setParameter("showMirrorings", "true");
        return request;
    }

    private GroupDetails group(int id, String domain, String path) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        group.setGroupName("autotest");
        group.setDomainName(domain);
        group.setFullPath(path);
        return group;
    }

    private DocDetails page(int id, int groupId) {
        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setGroupId(groupId);
        page.setTitle("autotest-page-" + id);
        page.setFullPath("/autotest-page-" + id);
        return page;
    }
}
