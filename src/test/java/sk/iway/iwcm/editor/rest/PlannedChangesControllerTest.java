package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.PlannedChangesService;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.users.UsersDB;

/** Planned lifecycle actions use the editor's permissions for the stored master page. */
class PlannedChangesControllerTest {
    /** Denied operations must not change media, history or files. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checksEditorPermissionsBeforeChangingPlan(boolean allowed) {
        PlannedChangesService plans = mock(PlannedChangesService.class);
        EditorService editor = mock(EditorService.class);
        PlannedChangesController controller = new PlannedChangesController(plans, editor);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Identity user = new Identity();
        DocDetails page = new DocDetails();
        page.setDocId(42);
        when(editor.getDoc(42, -1)).thenReturn(page);
        if (!allowed) doThrow(new IllegalArgumentException("Denied")).when(editor).checkPermissions(user, page, false);

        try (var users = mockStatic(UsersDB.class); var mappings = mockStatic(MultigroupMappingDB.class);
             var documents = mockStatic(DocDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(43)).thenReturn(42);
            documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));

            if (allowed) {
                when(plans.initialize(42)).thenReturn(123L);
                assertEquals(123L, controller.initialize(43, request).get("historyId"));
                assertTrue(controller.cancel(43, request).get("success"));
                var order = inOrder(editor, plans);
                order.verify(editor).getDoc(42, -1);
                order.verify(editor).checkPermissions(user, page, false);
                order.verify(plans).initialize(42);
                order.verify(editor).getDoc(42, -1);
                order.verify(editor).checkPermissions(user, page, false);
                order.verify(plans).cancel(42);
            } else {
                assertThrows(IllegalArgumentException.class, () -> controller.initialize(43, request));
                assertThrows(IllegalArgumentException.class, () -> controller.cancel(43, request));
                verifyNoInteractions(plans);
            }
        }
    }
}
