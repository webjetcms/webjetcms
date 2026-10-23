package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.datatable.OptionDto;

/** Covers group deltas and authorization before delegating to the editor save workflow. */
class WebpagesPerexGroupsTest {
    private WebpagesRestController controller;
    private EditorService editor;
    private DocDB documents;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = mock(WebpagesRestController.class, CALLS_REAL_METHODS);
        editor = mock(EditorService.class);
        documents = mock(DocDB.class);
        request = new MockHttpServletRequest();
        request.setParameter("customData", "30");
        ReflectionTestUtils.setField(controller, "editorService", editor);
        doReturn(mock(Identity.class)).when(controller).getUser();
        doReturn(request).when(controller).getRequest();
        doAnswer(call -> call.getArgument(0)).when(controller).editItem(any(), anyLong());
        doThrow(new IllegalArgumentException("invalid group")).when(controller).throwError(anyString());
        PerexGroupBean group = new PerexGroupBean();
        group.setPerexGroupId(30);
        when(documents.getPerexGroups()).thenReturn(List.of(group));
        when(documents.getPerexGroups(10)).thenReturn(List.of(group));
    }

    /** Large selections bind from JSON without placing page IDs in the request URL. */
    @ParameterizedTest
    @CsvSource({"1000,false", "1000,true"})
    void acceptsLargeSelectionsInJsonBody(int count, boolean remove) throws Exception {
        Long[] ids = LongStream.range(100000, 100000 + count).boxed().toArray(Long[]::new);
        doReturn(List.of()).when(controller).perexGroupOptions(any(Long[].class), anyBoolean());

        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(post("/admin/rest/web-pages/perex-groups")
                .queryParam("remove", String.valueOf(remove))
                .contentType(MediaType.APPLICATION_JSON)
                .content(Arrays.toString(ids)))
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        ArgumentCaptor<Long[]> receivedIds = ArgumentCaptor.forClass(Long[].class);
        verify(controller).perexGroupOptions(receivedIds.capture(), eq(remove));
        assertArrayEquals(ids, receivedIds.getValue());
    }

    /** Different starting groups survive add/remove, and repeating either action does not save again. */
    @Test
    void preservesOtherGroupsAndSkipsUnchangedPages() {
        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            DocDetails first = page(1, 11, 12);
            DocDetails second = page(2, 22, 30);
            controller.processAction(first, "addPerexGroup");
            controller.processAction(second, "addPerexGroup");
            controller.processAction(first, "addPerexGroup");
            assertArrayEquals(new Integer[] {11, 12, 30}, first.getPerexGroups());
            assertArrayEquals(new Integer[] {22, 30}, second.getPerexGroups());
            verify(controller, times(1)).editItem(first, 1);
            verify(controller, never()).editItem(second, 2);

            controller.processAction(first, "removePerexGroup");
            controller.processAction(second, "removePerexGroup");
            controller.processAction(second, "removePerexGroup");
            assertArrayEquals(new Integer[] {11, 12}, first.getPerexGroups());
            assertArrayEquals(new Integer[] {22}, second.getPerexGroups());
            verify(controller, times(2)).editItem(first, 1);
            verify(controller, times(1)).editItem(second, 2);
        }
    }

    /** Removing the last group persists an empty array and adding to an empty page works. */
    @Test
    void handlesEmptyGroupLists() {
        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            DocDetails page = page(1);
            controller.processAction(page, "addPerexGroup");
            assertArrayEquals(new Integer[] {30}, page.getPerexGroups());
            controller.processAction(page, "removePerexGroup");
            assertArrayEquals(new Integer[0], page.getPerexGroups());
        }
    }

    /** Foreign or unavailable group IDs are rejected without changing or saving the page. */
    @Test
    void rejectsUnavailableGroups() {
        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            request.setParameter("customData", "999");
            DocDetails page = page(1, 11);
            assertThrows(IllegalArgumentException.class, () -> controller.processAction(page, "addPerexGroup"));
            assertThrows(IllegalArgumentException.class, () -> controller.processAction(page, "removePerexGroup"));
            assertArrayEquals(new Integer[] {11}, page.getPerexGroups());
            verify(controller, never()).editItem(any(), anyLong());
        }
    }

    /** The editor's page-level authorization is required even for a no-op request. */
    @Test
    void checksPermissionsBeforeChangingGroups() {
        DocDetails page = page(1, 30);
        doThrow(new SecurityException("denied")).when(editor).checkPermissions(any(), eq(page), eq(false));
        assertThrows(SecurityException.class, () -> controller.processAction(page, "addPerexGroup"));
        assertThrows(SecurityException.class, () -> controller.processAction(page, "removePerexGroup"));
        assertArrayEquals(new Integer[] {30}, page.getPerexGroups());
        verify(controller, never()).editItem(any(), anyLong());
    }

    /** Add choices are an intersection; removal includes groups assigned to either page. */
    @Test
    void offersChoicesForAllSelectedPages() {
        PerexGroupBean common = group(30);
        PerexGroupBean firstOnly = group(31);
        PerexGroupBean secondOnly = group(32);
        DocDetails first = page(1, 31);
        DocDetails second = page(2, 32);
        second.setGroupId(20);
        when(editor.prepareDocForEditor(1, -1)).thenReturn(first);
        when(editor.prepareDocForEditor(2, -1)).thenReturn(second);
        when(documents.getPerexGroups()).thenReturn(List.of(common, firstOnly, secondOnly));
        when(documents.getPerexGroups(10)).thenReturn(List.of(common, firstOnly));
        when(documents.getPerexGroups(20)).thenReturn(List.of(common, secondOnly));
        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            database.when(() -> DocDB.fixPerexNameDuplicityForOptions(anyList())).thenAnswer(call -> {
                List<PerexGroupBean> groups = call.getArgument(0);
                return groups.stream().map(group -> new OptionDto("", String.valueOf(group.getPerexGroupId()), null)).toList();
            });
            assertEquals(List.of("30"), controller.perexGroupOptions(new Long[] {1L, 2L}, false).stream().map(OptionDto::getValue).toList());
            assertEquals(List.of("31", "32"), controller.perexGroupOptions(new Long[] {1L, 2L}, true).stream().map(OptionDto::getValue).toList());
            verify(editor, times(2)).checkPermissions(any(), eq(first), eq(false));
            verify(editor, times(2)).checkPermissions(any(), eq(second), eq(false));
        }
    }

    /** Linked copies offer the master's available or assigned groups, which the action can then apply. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usesEditorDataForLinkedCopyOptionsAndActions(boolean remove) {
        PerexGroupBean masterGroup = group(30);
        PerexGroupBean copyGroup = group(31);
        DocDetails copy = page(2, 31);
        copy.setGroupId(20);
        // Editor data retains the copy's ID but uses the master's folder and content.
        DocDetails editorDoc = page(2, 11);
        if (remove) editorDoc.setPerexGroups(new Integer[] {11, 30});
        when(documents.getDoc(2)).thenReturn(copy);
        when(editor.prepareDocForEditor(2, -1)).thenReturn(editorDoc);
        when(documents.getPerexGroups()).thenReturn(List.of(masterGroup, copyGroup));
        when(documents.getPerexGroups(10)).thenReturn(List.of(masterGroup));
        when(documents.getPerexGroups(20)).thenReturn(List.of(copyGroup));

        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            database.when(() -> DocDB.fixPerexNameDuplicityForOptions(anyList())).thenAnswer(call -> {
                List<PerexGroupBean> groups = call.getArgument(0);
                return groups.stream().map(group -> new OptionDto("", String.valueOf(group.getPerexGroupId()), null)).toList();
            });
            List<OptionDto> options = controller.perexGroupOptions(new Long[] {2L}, remove);
            assertEquals(List.of("30"), options.stream().map(OptionDto::getValue).toList());

            request.setParameter("customData", options.get(0).getValue());
            assertTrue(controller.processAction(editorDoc, remove ? "removePerexGroup" : "addPerexGroup"));
            Integer[] expectedGroups = remove ? new Integer[] {11} : new Integer[] {11, 30};
            assertArrayEquals(expectedGroups, editorDoc.getPerexGroups());
            verify(controller).editItem(editorDoc, 2);
        }
    }

    /** Option loading must authorize the same resolved page that the action will modify. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsOptionsWhenResolvedPageIsNotEditable(boolean remove) {
        DocDetails copy = page(2, 30);
        copy.setGroupId(20);
        DocDetails editorDoc = page(2, 30);
        when(documents.getDoc(2)).thenReturn(copy);
        when(editor.prepareDocForEditor(2, -1)).thenReturn(editorDoc);
        doThrow(new SecurityException("denied")).when(editor).checkPermissions(any(), eq(editorDoc), eq(false));

        try (var database = mockStatic(DocDB.class)) {
            database.when(DocDB::getInstance).thenReturn(documents);
            assertThrows(SecurityException.class, () -> controller.perexGroupOptions(new Long[] {2L}, remove));
            verify(controller, never()).editItem(any(), anyLong());
        }
    }

    /** History IDs must never be treated as live page IDs by a bulk action. */
    @Test
    void rejectsHistoricalRevision() {
        request.setParameter("historyId", "100");
        assertThrows(IllegalArgumentException.class, () -> controller.processAction(page(1), "addPerexGroup"));
        verify(controller, never()).editItem(any(), anyLong());
    }

    private PerexGroupBean group(int id) {
        PerexGroupBean group = new PerexGroupBean();
        group.setPerexGroupId(id);
        return group;
    }

    private DocDetails page(int id, Integer... groups) {
        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setGroupId(10);
        page.setPerexGroups(groups);
        return page;
    }
}
