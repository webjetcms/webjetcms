package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.util.EditorUtils;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Ordinary publication must reject private assets before writing live content or history. */
class EditorPrivateReferencesTest {
    /** The real editor save path checks inherited content fields and decodes private references. */
    @ParameterizedTest
    @ValueSource(strings = {
        "/files/protected/planned-changes/42/images/future.webp",
        "%2Ffiles%2Fprotected%2Fplanned-changes%2F42%2Fimages%2Ffuture.webp",
        "&#47;files&#47;protected&#47;planned-changes&#47;42&#47;images&#47;future.webp"
    })
    void rejectsPrivateReferencesBeforeWritingPageOrHistory(String reference) {
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        DocHistoryRepository histories = mock(DocHistoryRepository.class);
        GroupsDB folderCache = mock(GroupsDB.class);
        when(documents.getGroupId(42)).thenReturn(1L);
        doAnswer(call -> {
            fail("Private content reached live-page persistence");
            return null;
        }).when(documents).save(any());
        MockHttpServletRequest request = new MockHttpServletRequest();
        EditorService editor = spy(new EditorService());
        Identity user = new Identity();
        user.setUserId(7);
        ReflectionTestUtils.setField(editor, "currentUser", user);
        ReflectionTestUtils.setField(editor, "docRepo", documents);
        ReflectionTestUtils.setField(editor, "historyRepo", histories);
        ReflectionTestUtils.setField(editor, "groupsDB", folderCache);
        ReflectionTestUtils.setField(editor, "request", request);
        doNothing().when(editor).checkPermissions(any(), any(), anyBoolean());
        doNothing().when(editor).setVirtualPath(any());

        DocDetails submitted = new DocDetails();
        submitted.setDocId(42);
        submitted.setGroupId(1);
        submitted.setTitle("Ordinary publication");
        submitted.setFullPath("/ordinary-publication.html");
        submitted.setDocLink("/ordinary-publication.html");
        submitted.setData("<img src='" + reference + "'>");
        DocEditorFields fields = new DocEditorFields();
        fields.setRequestPublish(true);
        submitted.setEditorFields(fields);

        try (var events = mockStatic(WebjetEventPublisher.class);
             var cache = mockStatic(DocDB.class);
             var folders = mockStatic(GroupsDB.class);
             var groups = mockStatic(GroupsService.class);
             var access = mockStatic(PlannedAccess.class);
             var constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
             var cloud = mockStatic(CloudToolsForCore.class);
             var context = mockStatic(ContextFilter.class);
             var facade = mockStatic(EditorFacade.class);
             var utils = mockStatic(EditorUtils.class, CALLS_REAL_METHODS);
             var translations = mockStatic(Prop.class)) {
            folders.when(GroupsDB::getInstance).thenReturn(folderCache);
            constants.when(() -> Constants.getBoolean("editorEscapeInvalidCharacters")).thenReturn(false);
            facade.when(() -> EditorFacade.isSkipApproving(1)).thenReturn(true);
            utils.when(() -> EditorUtils.getDataAsc(anyString(), eq(submitted), eq(false), eq(request))).thenReturn("Image");
            Prop prop = mock(Prop.class);
            when(prop.getText("planned.privateReferencesNotAllowed")).thenReturn("Private references are not allowed");
            translations.when(Prop::getInstance).thenReturn(prop);
            ReflectionTestUtils.setField(editor, "prop", prop);

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> editor.saveEditedDoc(submitted));
            assertEquals("Private references are not allowed", error.getMessage());
            verify(documents, never()).save(any());
            verify(documents, never()).saveAndFlush(any());
            verifyNoInteractions(histories);
        }
    }
}
