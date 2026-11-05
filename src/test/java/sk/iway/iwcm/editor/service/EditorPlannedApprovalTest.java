package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Checks approval timing and delegation to the scheduled publisher. */
class EditorPlannedApprovalTest {
    private static final long NOW = 2000000000000L;

    /** Future approval only schedules; approval at or after the deadline publishes immediately. */
    @ParameterizedTest
    @ValueSource(longs = {60000, 0, -60000})
    void approvesAndPublishesOnlyWhenDue(long offset) throws Exception {
        DocDB cache = mock(DocDB.class);
        DocHistoryRepository histories = mock(DocHistoryRepository.class);
        DocHistory history = history(offset);
        doAnswer(call -> {
            verify(histories).saveAndFlush(history);
            assertNull(history.getAwaitingApprove());
            assertNull(history.getDisapprovedBy());
            assertEquals(7, history.getApprovedBy());
            return null;
        }).when(cache).publishDocHistory(history);

        try (var access = mockStatic(PlannedAccess.class); var docCache = mockStatic(DocDB.class)) {
            access.when(PlannedAccess::enabled).thenReturn(true);

            editor(cache, histories).approveDocHistory(history);

            assertEquals(7, history.getApprovedBy());
            assertEquals(new Date(NOW), history.getApproveDate());
            assertNull(history.getAwaitingApprove());
            assertNull(history.getDisapprovedBy());
            assertFalse(history.getActual());
            verify(histories).saveAndFlush(history);
            verify(cache).readPagesToPublic();
            if (offset <= 0) {
                verify(cache).publishDocHistory(history);
            } else {
                verify(cache, never()).publishDocHistory(any());
            }
        }
    }

    /** Ordinary approval retains its existing path, also when planned publishing is disabled. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void keepsOrdinaryApproval(boolean enabled) {
        DocDB cache = mock(DocDB.class);
        DocHistoryRepository histories = mock(DocHistoryRepository.class);
        DocHistory history = history(60000);
        history.setPublicable(!enabled);
        try (var access = mockStatic(PlannedAccess.class);
             var docCache = mockStatic(DocDB.class);
             var groups = mockStatic(GroupsService.class);
             var events = mockStatic(WebjetEventPublisher.class);
             var fields = mockConstruction(DocEditorFields.class, withSettings().defaultAnswer(CALLS_REAL_METHODS),
                 (mock, context) -> doNothing().when(mock).fromDocDetails(any(), anyBoolean(), anyBoolean()))) {
            access.when(PlannedAccess::enabled).thenReturn(enabled);

            editor(cache, histories).approveDocHistory(history);

            assertEquals(7, history.getApprovedBy());
            assertTrue(history.getActual());
            assertNull(history.getAwaitingApprove());
            verify(histories).save(history);
            verify(histories, never()).saveAndFlush(any());
            verify(cache, never()).publishDocHistory(any());
        }
    }

    /** A denied editor cannot persist approval or trigger publication. */
    @Test
    void checksPermissionsBeforeSavingApproval() {
        DocDB cache = mock(DocDB.class);
        DocHistoryRepository histories = mock(DocHistoryRepository.class);
        EditorService editor = editor(cache, histories);
        doThrow(new IllegalArgumentException("Denied")).when(editor).checkPermissions(any(), any(), anyBoolean());
        try (var access = mockStatic(PlannedAccess.class); var docCache = mockStatic(DocDB.class)) {
            access.when(PlannedAccess::enabled).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> editor.approveDocHistory(history(-60000)));
            verifyNoInteractions(histories, cache);
        }
    }

    private static EditorService editor(DocDB cache, DocHistoryRepository histories) {
        EditorService editor = spy(new EditorService());
        Identity user = new Identity();
        user.setUserId(7);
        ReflectionTestUtils.setField(editor, "currentUser", user);
        ReflectionTestUtils.setField(editor, "historyRepo", histories);
        ReflectionTestUtils.setField(editor, "docDB", cache);
        ReflectionTestUtils.setField(editor, "now", NOW);
        doNothing().when(editor).checkPermissions(any(), any(), anyBoolean());
        return editor;
    }

    private static DocHistory history(long offset) {
        DocHistory history = new DocHistory();
        history.setId(123L);
        history.setDocId(42);
        history.setGroupId(1);
        history.setDocLink("/approved.html");
        history.setFullPath("/approved.html");
        history.setPublicable(true);
        history.setPublishAfterStart(true);
        history.setPublishStartDate(new Date(NOW + offset));
        history.setAwaitingApprove(",7,");
        history.setDisapprovedBy(8);
        return history;
    }
}
