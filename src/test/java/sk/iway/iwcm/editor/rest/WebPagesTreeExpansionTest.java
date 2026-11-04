package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsJsTreeItem;
import sk.iway.iwcm.doc.GroupsTreeService;

/** Covers direct links to nested folders in trees whose visible roots differ from the full hierarchy. */
class WebPagesTreeExpansionTest {

    /** Expands and selects the requested folder even when System or Trash ancestors are outside the visible tree. */
    @ParameterizedTest
    @ValueSource(strings = { "dt-tree-group-filter-system-trash", "dt-tree-group-system", "dt-tree-group-trash" })
    void expandsFromTheVisibleRoot(String click) {
        GroupsDB groups = mock(GroupsDB.class);
        GroupsTreeService tree = mock(GroupsTreeService.class);
        Identity user = mock(Identity.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        GroupDetails system = group(1);
        GroupDetails trash = group(2);
        GroupDetails parent = group(3);
        GroupDetails child = group(4);
        List<GroupDetails> ancestry = new ArrayList<>(List.of(child, parent));
        if (click.endsWith("-trash") && !click.contains("filter")) ancestry.add(trash);
        if (!click.contains("filter")) ancestry.add(system);
        GroupsJsTreeItem parentNode = node(parent);
        GroupsJsTreeItem childNode = node(child);
        when(tree.getItems(user, 3, false, click, null, request)).thenReturn(List.of(childNode));
        when(tree.getItems(user, 4, false, click, null, request)).thenReturn(List.of());
        when(groups.getParentGroups(4)).thenAnswer(invocation -> new ArrayList<>(ancestry));
        when(groups.getParentGroups(4, true)).thenAnswer(invocation -> new ArrayList<>(ancestry));

        try (MockedStatic<GroupsDB> access = mockStatic(GroupsDB.class)) {
            access.when(GroupsDB::getInstance).thenReturn(groups);
            WebPagesListener listener = new WebPagesListener();
            ReflectionTestUtils.setField(listener, "groupsTreeService", tree);
            List<JsTreeItem> expanded = ReflectionTestUtils.invokeMethod(listener, "expandGroupsLastFolder",
                List.of(parentNode), 4, user, false, click, request);

            assertNotNull(expanded);
            assertEquals(List.of("3", "4"), expanded.stream().map(JsTreeItem::getId).toList());
            assertTrue(parentNode.getState().isOpened());
            assertTrue(childNode.getState().isSelected());
            assertFalse(parentNode.getState().isSelected());
            verify(tree, never()).getItems(eq(user), eq(1), anyBoolean(), anyString(), any(), any());
            verify(tree, never()).getItems(eq(user), eq(2), anyBoolean(), anyString(), any(), any());
        }
    }

    private static GroupDetails group(int id) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        return group;
    }

    private static GroupsJsTreeItem node(GroupDetails group) {
        GroupsJsTreeItem node = mock(GroupsJsTreeItem.class);
        when(node.getId()).thenReturn(String.valueOf(group.getGroupId()));
        when(node.getGroup()).thenReturn(group);
        when(node.getState()).thenReturn(new JsTreeItemState());
        return node;
    }
}
