package sk.iway.iwcm.components.news;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ScopedGroupsTreeItem;
import sk.iway.iwcm.doc.ScopedGroupsTreeService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Covers recursive configuration and visibility rules beyond the News E2E scenarios. */
class NewsTreeServiceTest {
    private final Map<Integer, GroupDetails> folders = new LinkedHashMap<>();
    private final Set<Integer> editable = new HashSet<>();
    private final Set<Integer> viewable = new HashSet<>();
    private final Identity user = mock(Identity.class);
    private MockedStatic<GroupsDB> groupsMock;
    private MockedStatic<Constants> constantsMock;
    private MockedStatic<WebpagesService> webpagesMock;

    @BeforeEach
    void setUp() {
        GroupsDB groupsDB = mock(GroupsDB.class);
        groupsMock = mockStatic(GroupsDB.class);
        constantsMock = mockStatic(Constants.class);
        webpagesMock = mockStatic(WebpagesService.class);
        groupsMock.when(GroupsDB::getInstance).thenReturn(groupsDB);
        groupsMock.when(() -> GroupsDB.sanitizeGroupName(anyString(), eq(false))).thenAnswer(call -> call.getArgument(0));
        groupsMock.when(() -> GroupsDB.isGroupEditable(eq(user), anyInt())).thenAnswer(call -> editable.contains(call.getArgument(1)));
        groupsMock.when(() -> GroupsDB.isGroupViewable(eq(user), anyInt())).thenAnswer(call -> viewable.contains(call.getArgument(1)));
        constantsMock.when(() -> Constants.getBoolean("multiDomainEnabled")).thenReturn(true);
        webpagesMock.when(() -> WebpagesService.getTreeSortType(user)).thenReturn("priority");
        webpagesMock.when(() -> WebpagesService.isTreeSortOrderAsc(user)).thenReturn(true);
        when(groupsDB.getGroup(anyInt())).thenAnswer(call -> folders.get(call.getArgument(0)));
        when(groupsDB.getGroups(anyInt())).thenAnswer(call -> folders.values().stream()
                .filter(group -> group.getParentGroupId() == (int) call.getArgument(0)).toList());
        addFolder(10, 1, "News");
        addFolder(11, 10, "2026");
        addFolder(12, 11, "Press releases");
        addFolder(13, 10, "Archive");
        addFolder(20, 1, "Other news");
    }

    @AfterEach
    void tearDown() {
        webpagesMock.close();
        constantsMock.close();
        groupsMock.close();
    }

    @Test
    void deduplicatesOverlappingRootsAndRetainsExplicitChildRecursion() {
        List<ScopedGroupsTreeItem> items = tree("10", "11*", "10*").getItems(0, 11, null, "contains");
        assertEquals(ids(items).size(), new HashSet<>(ids(items)).size());
        ScopedGroupsTreeItem child = items.stream().filter(item -> item.getId().equals("11")).findFirst().orElseThrow();
        assertEquals("10", child.getParent());
        assertEquals("11*", child.getGroupIdList());
        assertEquals("10*", items.get(0).getGroupIdList());
    }

    @Test
    void excludesHiddenAndCrossDomainFoldersEvenFromSearch() {
        folders.get(11).setHiddenInAdmin(true);
        folders.get(20).setDomainName("other.example");
        when(user.isDisabledItem("editor_show_hidden_folders")).thenReturn(true);
        ScopedGroupsTreeService tree = tree("10", "20");
        assertEquals(List.of("10"), ids(tree.getItems(0, -1, null, "contains")));
        assertEquals(List.of("13"), ids(tree.getItems(10, -1, null, "contains")));
        assertTrue(tree.getItems(0, -1, "Press", "contains").isEmpty());
        assertTrue(tree.getItems(20, -1, null, "contains").isEmpty());
    }

    @Test
    void keepsViewableAncestorsDisabledAndSelectsPermittedDescendant() {
        editable.clear();
        editable.add(12);
        viewable.addAll(Set.of(10, 11));
        List<ScopedGroupsTreeItem> items = tree("10").getItems(0, -1, null, "contains");
        assertEquals(Set.of("10", "11", "12"), new HashSet<>(ids(items)));
        assertTrue(items.get(0).getState().isDisabled());
        assertEquals("12", items.stream().filter(item -> item.getState().isSelected()).findFirst().orElseThrow().getId());
    }

    private ScopedGroupsTreeService tree(String... values) {
        return new ScopedGroupsTreeService(java.util.Arrays.stream(values).map(value -> new LabelValue(value, value)).toList(), user, "news.example");
    }

    private static List<String> ids(List<ScopedGroupsTreeItem> items) {
        return items.stream().map(ScopedGroupsTreeItem::getId).toList();
    }

    private void addFolder(int id, int parentId, String name) {
        GroupDetails folder = spy(new GroupDetails());
        doReturn("/").when(folder).getVirtualPath();
        folder.setGroupId(id);
        folder.setParentGroupId(parentId);
        folder.setGroupName(name);
        folder.setFullPath((folders.containsKey(parentId) ? folders.get(parentId).getFullPath() : "") + "/" + name);
        folder.setDomainName("news.example");
        folders.put(id, folder);
        editable.add(id);
    }
}
