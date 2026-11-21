package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.i18n.Prop;

/** The editor JSON omits synchronization fields, while WORK writes retain their server-owned values. */
class BranchEditorValidationTest {
    /** A page with syncStatus=1 accepts text edits even though Jackson intentionally ignores its synchronization fields. */
    @Test void ignoredSynchronizationFieldsDoNotBlockTextChangesOrOverwriteStoredValues() {
        var request = new MockHttpServletRequest();
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);when(user.getUserId()).thenReturn(1);
        when(user.isEnabledItem(BranchAccess.PERMISSION)).thenReturn(true);
        request.getSession().setAttribute(Constants.USER_KEY,user);
        try (var constants = mockStatic(Constants.class);var contexts = mockStatic(BranchContexts.class)) {
            constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            contexts.when(() -> BranchContexts.current(request)).thenReturn(new BranchContext(1,"example.sk",1,BranchContext.Mode.ADMIN));
            BranchEditor editor = new BranchEditor(request);
            DocDetails stored = new DocDetails();stored.setSyncId(7);stored.setSyncStatus(1);
            DocDetails submitted = JsonMapper.builder().build().readValue("{\"docId\":12,\"data\":\"<p>Changed text</p>\",\"syncId\":99,\"syncStatus\":99}",DocDetails.class);
            submitted.setEditorFields(new DocEditorFields());
            assertEquals(0,submitted.getSyncStatus());assertEquals(0,submitted.getSyncId());
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(editor,"rejectUnsupported",submitted,stored));
            var row = new LinkedHashMap<>(BranchDocumentMapper.initialValues(12,1,Instant.EPOCH,false));
            row.put("sync_id",7);row.put("sync_status",1);
            var work = BranchEditorRows.document(submitted,row);
            assertEquals("<p>Changed text</p>",work.get("data"));
            assertEquals(7,work.get("sync_id"));assertEquals(1,work.get("sync_status"));
        }
    }

    /** Actual changes to individual publication dates still receive a translated validation message. */
    @Test void changingAnIndividualPublicationDateIsStillRejected() {
        BranchEditor editor = mock(BranchEditor.class,CALLS_REAL_METHODS);
        DocDetails original = new DocDetails();
        DocDetails changed = new DocDetails();changed.setEditorFields(new DocEditorFields());changed.setPublishEndDate(new Date(1000));
        try (var properties = mockStatic(Prop.class)) {
            Prop prop = mock(Prop.class);
            properties.when(() -> Prop.getInstance((jakarta.servlet.http.HttpServletRequest)null)).thenReturn(prop);
            when(prop.getText("branches.editor.scheduling.readonly")).thenReturn("Plánovanie publikovania je iba na čítanie.");
            var error = assertThrows(ResponseStatusException.class,() -> ReflectionTestUtils.invokeMethod(editor,"rejectUnsupported",changed,original));
            assertEquals("Plánovanie publikovania je iba na čítanie.",error.getReason());
        }
    }
}
