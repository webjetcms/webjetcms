package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

/** Covers user group deltas, editor field synchronization and authorization before saving. */
class WebpagesUserGroupsTest {
    private WebpagesRestController controller;
    private EditorService editor;
    private UserGroupsDB groups;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = mock(WebpagesRestController.class, CALLS_REAL_METHODS);
        editor = mock(EditorService.class);
        groups = mock(UserGroupsDB.class);
        request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(controller, "editorService", editor);
        doReturn(mock(Identity.class)).when(controller).getUser();
        doReturn(request).when(controller).getRequest();
        doAnswer(call -> call.getArgument(0)).when(controller).editItem(any(), anyLong());
        doThrow(new IllegalArgumentException("invalid group")).when(controller).throwError(anyString());
        List<UserGroupDetails> available = List.of(group(11, UserGroupDetails.TYPE_PERMS), group(22, UserGroupDetails.TYPE_EMAIL),
            group(30, UserGroupDetails.TYPE_PERMS), group(31, UserGroupDetails.TYPE_EMAIL));
        when(groups.getUserGroups()).thenReturn(available);
        when(groups.getUserGroup(anyInt())).thenAnswer(call -> available.stream()
            .filter(group -> group.getUserGroupId() == (int) call.getArgument(0)).findFirst().orElse(null));
    }

    /** Both types preserve existing assignments and synchronize the values consumed by the facade. */
    @ParameterizedTest
    @ValueSource(ints = {30, 31})
    void preservesBothGroupTypesAndSkipsRepeatedChanges(int groupId) {
        request.setParameter("customData", String.valueOf(groupId));
        try (var database = mockStatic(UserGroupsDB.class)) {
            database.when(UserGroupsDB::getInstance).thenReturn(groups);
            DocDetails page = page(1, "11,22");
            controller.processAction(page, "addUserGroup");
            String expected = groupId == 30 ? "11,30,22" : "11,22,31";
            assertEquals(expected, page.getPasswordProtected());
            assertEquals(expected, UserDetailsService.getUserGroupIds(page.getEditorFields().getPermisions(), page.getEditorFields().getEmails()));
            controller.processAction(page, "addUserGroup");
            verify(controller, times(1)).editItem(page, 1);
            controller.processAction(page, "removeUserGroup");
            controller.processAction(page, "removeUserGroup");
            assertEquals("11,22", page.getPasswordProtected());
            assertArrayEquals(new Integer[] {11}, page.getEditorFields().getPermisions());
            assertArrayEquals(new Integer[] {22}, page.getEditorFields().getEmails());
            assertArrayEquals(new Integer[] {99}, page.getPerexGroups());
            verify(controller, times(2)).editItem(page, 1);
        }
    }

    /** Adding to an empty page and removing its last group updates both editor arrays. */
    @ParameterizedTest
    @ValueSource(ints = {30, 31})
    void handlesEmptyAndLastGroup(int groupId) {
        request.setParameter("customData", String.valueOf(groupId));
        try (var database = mockStatic(UserGroupsDB.class)) {
            database.when(UserGroupsDB::getInstance).thenReturn(groups);
            DocDetails page = page(1, "");
            controller.processAction(page, "addUserGroup");
            assertEquals(String.valueOf(groupId), page.getPasswordProtected());
            controller.processAction(page, "removeUserGroup");
            assertEquals("", page.getPasswordProtected());
            assertArrayEquals(new Integer[0], page.getEditorFields().getPermisions());
            assertArrayEquals(new Integer[0], page.getEditorFields().getEmails());
        }
    }

    /** Only the domain-filtered user group list may be used, even if an ID resolves globally. */
    @ParameterizedTest
    @ValueSource(strings = {"addUserGroup", "removeUserGroup"})
    void rejectsUnavailableGroups(String action) {
        request.setParameter("customData", "999");
        when(groups.getUserGroup(999)).thenReturn(group(999, UserGroupDetails.TYPE_PERMS));
        try (var database = mockStatic(UserGroupsDB.class)) {
            database.when(UserGroupsDB::getInstance).thenReturn(groups);
            DocDetails page = page(1, "11,22");
            assertThrows(IllegalArgumentException.class, () -> controller.processAction(page, action));
            assertEquals("11,22", page.getPasswordProtected());
            verify(controller, never()).editItem(any(), anyLong());
        }
    }

    /** Both actions check page permissions before attempting a change or no-op. */
    @ParameterizedTest
    @ValueSource(strings = {"addUserGroup", "removeUserGroup"})
    void rejectsUnauthorizedActionsAndHistoricalViews(String action) {
        DocDetails page = page(1, "11");
        doThrow(new SecurityException("denied")).when(editor).checkPermissions(any(), eq(page), eq(false));
        assertThrows(SecurityException.class, () -> controller.processAction(page, action));
        doNothing().when(editor).checkPermissions(any(), eq(page), eq(false));
        request.setParameter("historyId", "100");
        assertThrows(IllegalArgumentException.class, () -> controller.processAction(page, action));
        request.removeParameter("historyId");
        request.setParameter("groupId", String.valueOf(Constants.getInt("systemPagesDocsToApprove")));
        assertThrows(IllegalArgumentException.class, () -> controller.processAction(page, action));
        verify(controller, never()).editItem(any(), anyLong());
    }

    /** Removal uses the union of resolved editor assignments, including linked-copy data. */
    @Test
    void offersBothTypesAndOnlyAssignedGroupsForRemoval() {
        DocDetails first = page(1, "11");
        DocDetails linkedCopy = page(2, "22");
        when(editor.prepareDocForEditor(1, -1)).thenReturn(first);
        when(editor.prepareDocForEditor(2, -1)).thenReturn(linkedCopy);
        try (var database = mockStatic(UserGroupsDB.class)) {
            database.when(UserGroupsDB::getInstance).thenReturn(groups);
            Long[] ids = {1L, 2L};
            assertEquals(List.of("11", "22", "30", "31"), controller.userGroupOptions(ids, false).stream().map(OptionDto::getValue).toList());
            assertEquals(List.of("11", "22"), controller.userGroupOptions(ids, true).stream().map(OptionDto::getValue).toList());
            verify(editor, times(2)).checkPermissions(any(), eq(linkedCopy), eq(false));
            doThrow(new SecurityException("denied")).when(editor).checkPermissions(any(), eq(linkedCopy), eq(false));
            assertThrows(SecurityException.class, () -> controller.userGroupOptions(ids, false));
            assertThrows(SecurityException.class, () -> controller.userGroupOptions(ids, true));
        }
    }

    /** Large selections bind from JSON without placing page IDs in the request URL. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsLargeSelectionsInJsonBody(boolean remove) throws Exception {
        Long[] ids = LongStream.range(100000, 101000).boxed().toArray(Long[]::new);
        doReturn(List.of()).when(controller).userGroupOptions(any(Long[].class), anyBoolean());
        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(post("/admin/rest/web-pages/user-groups").queryParam("remove", String.valueOf(remove))
                .contentType(MediaType.APPLICATION_JSON).content(Arrays.toString(ids)))
            .andExpect(status().isOk());
        verify(controller).userGroupOptions(aryEq(ids), eq(remove));
    }

    private UserGroupDetails group(int id, int type) {
        UserGroupDetails group = new UserGroupDetails();
        group.setUserGroupId(id);
        group.setUserGroupType(type);
        group.setUserGroupName("Group " + id);
        return group;
    }

    private DocDetails page(int id, String passwordProtected) {
        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setGroupId(10);
        page.setPasswordProtected(passwordProtected);
        page.setEditorFields(new DocEditorFields());
        page.setPerexGroups(new Integer[] {99});
        return page;
    }
}
