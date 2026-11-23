package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupEditorField;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.approve.ApproveService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Verifies that direct folder edits and the DataTable Editor apply the same parent validation. */
class GroupsRestControllerHierarchyTest {
    private static final int FOLDER_ID = 10;
    private static final int ORIGINAL_PARENT = 1;
    private static final String PARENT_FIELD = "errorField.editorFields.parentGroupDetails";
    @AutoClose private static final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();

    private final GroupsDB groups = mock(GroupsDB.class);
    private final Identity user = mock(Identity.class);
    private final Map<Integer, GroupDetails> tree = new HashMap<>();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final GroupsRestController controller = new GroupsRestController(null, null, null, mock(ApproveService.class), null);

    @AutoClose private final MockedStatic<GroupsDB> groupAccess = mockStatic(GroupsDB.class);
    @AutoClose private final MockedStatic<Prop> translations = mockStatic(Prop.class);
    @AutoClose private final MockedStatic<InitServlet> deployment = mockStatic(InitServlet.class);
    @AutoClose private final MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class);

    @BeforeEach
    void setUp() {
        groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
        groupAccess.when(() -> GroupsDB.isGroupEditable(eq(user), anyInt())).thenReturn(true);
        groupAccess.when(() -> GroupsDB.sanitizeGroupName(anyString(), anyBoolean())).thenCallRealMethod();
        when(groups.getGroupNamePath(FOLDER_ID)).thenReturn("/Folder");
        when(groups.getGroup(anyInt())).thenAnswer(call -> tree.get(call.<Integer>getArgument(0)));
        tree.put(ORIGINAL_PARENT, folder(ORIGINAL_PARENT, 0));
        tree.put(FOLDER_ID, folder(FOLDER_ID, ORIGINAL_PARENT));
        // The deepest descendant is beyond the former 50-parent validation limit.
        for (int id = FOLDER_ID + 1; id <= 65; id++) tree.put(id, folder(id, id - 1));
        // This separate branch already contains a cycle.
        tree.put(80, folder(80, 81));
        tree.put(81, folder(81, 80));
        tree.put(90, folder(90, 0));

        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        translations.when(() -> Prop.getInstance(request)).thenReturn(prop);
        request.getSession().setAttribute(Constants.USER_KEY, user);
        controller.setRequest(request);
        controller.setValidator(validators.getValidator());
    }

    /** Both entry points reject a cyclic move before the cached folder or its database row changes. */
    @ParameterizedTest
    @CsvSource({
        "10, editor.parent_group_error_same_folder",
        "11, editor.parent_group_error_child_folder",
        "65, editor.parent_group_error_child_folder",
        "80, editor.parent_group_error_cycle"
    })
    void rejectsCyclicMove(int newParent, String errorKey) {
        GroupDetails submitted = submittedMove(newParent);
        var fieldError = validateInEditor(submitted).getFieldError(PARENT_FIELD);
        assertNotNull(fieldError);
        assertEquals(errorKey, fieldError.getDefaultMessage());

        var failure = assertThrows(ConstraintViolationException.class, () -> controller.edit(FOLDER_ID, submitted));
        assertEquals(errorKey, failure.getMessage());
        assertEquals(ORIGINAL_PARENT, tree.get(FOLDER_ID).getParentGroupId());
        verify(groups, never()).save(any());
    }

    /** An unrelated folder and the root are both valid destinations for a permitted move. */
    @ParameterizedTest
    @ValueSource(ints = {90, 0})
    void savesValidMove(int newParent) {
        GroupDetails submitted = submittedMove(newParent);
        assertFalse(validateInEditor(submitted).hasErrors());

        GroupDetails cached = tree.get(FOLDER_ID);
        when(groups.save(cached)).thenReturn(true);
        // Copying editor fields also reads the folder's document and URL getters.
        try (var documents = mockStatic(DocDB.class)) {
            DocDB docDb = mock(DocDB.class);
            documents.when(DocDB::getInstance).thenReturn(docDb);
            assertEquals(200, controller.edit(FOLDER_ID, submitted).getStatusCode().value());
        }
        verify(groups).save(cached);
        assertEquals(newParent, cached.getParentGroupId());
    }

    private BeanPropertyBindingResult validateInEditor(GroupDetails submitted) {
        var edit = new DatatableRequest<Long, GroupDetails>();
        edit.setAction("edit");
        edit.setErrorField(submitted);
        var errors = new BeanPropertyBindingResult(edit, "editor");
        controller.validateEditor(request, edit, user, errors, (long) FOLDER_ID, submitted);
        return errors;
    }

    private GroupDetails submittedMove(int parentId) {
        GroupDetails submitted = folder(FOLDER_ID, ORIGINAL_PARENT);
        GroupEditorField fields = new GroupEditorField();
        fields.setParentGroupDetails(folder(parentId, 0));
        fields.setDefaultDocDetails(new DocDetails());
        fields.setUseDefaultLogonPage(true);
        submitted.setEditorFields(fields);
        return submitted;
    }

    private static GroupDetails folder(int id, int parentId) {
        var group = new GroupDetails();
        group.setGroupId(id);
        group.setParentGroupId(parentId);
        group.setGroupName("Folder " + id);
        group.setNavbarName(group.getGroupName());
        return group;
    }
}
