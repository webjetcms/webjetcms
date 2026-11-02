package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/** Verifies preview content and planned media after session serialization removes editor fields. */
class PreviewControllerTest {
    private MockedStatic<DocDB> documents;
    private MockedStatic<GroupsDB> groups;
    private MockedStatic<UserGroupsDB> userGroups;
    private MockedStatic<Prop> properties;
    private MockedStatic<TemplatesDB> templates;
    private MockedStatic<UsersDB> users;
    private MockedStatic<EditorDB> editor;
    private MockedStatic<PlannedAccess> planned;
    private MockedStatic<SetCharacterEncodingFilter> requests;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final PreviewController controller = new PreviewController();

    @BeforeEach
    void mockApplicationServices() {
        documents = mockStatic(DocDB.class);
        groups = mockStatic(GroupsDB.class);
        userGroups = mockStatic(UserGroupsDB.class);
        properties = mockStatic(Prop.class);
        templates = mockStatic(TemplatesDB.class);
        users = mockStatic(UsersDB.class);
        editor = mockStatic(EditorDB.class);
        planned = mockStatic(PlannedAccess.class);
        requests = mockStatic(SetCharacterEncodingFilter.class);

        documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
        groups.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
        userGroups.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
        properties.when(Prop::getInstance).thenReturn(mock(Prop.class));
        templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
        editor.when(() -> EditorDB.isPageEditable(null, 42)).thenReturn(true);
        planned.when(() -> PlannedAccess.masterId(42)).thenReturn(41);
    }

    @AfterEach
    void closeApplicationServices() {
        for (MockedStatic<?> service : List.of(requests, planned, editor, users, templates, properties, userGroups, groups, documents)) {
            service.close();
        }
    }

    /** Restored previews retain unsaved content and select the correct live or planned media. */
    @ParameterizedTest
    @CsvSource({
        "false, false, false, false",
        "true,  false, false, false",
        "false, true,  false, false",
        "true,  true,  false, true",
        "true,  false, true,  true"
    })
    void restoresSerializedPreview(boolean enabled, boolean publicable, boolean scheduled, boolean expectedPlanned) throws Exception {
        planned.when(PlannedAccess::enabled).thenReturn(enabled);
        DocDetails doc = previewDocument();
        doc.setPublicable(publicable);
        doc.getEditorFields().setPublishAfterStart(scheduled);

        DocDetails restored = submitAndSerialize(doc);

        assertEquals("forward:/showdoc.do", controller.preview(request, response));
        assertNotNull(restored.getEditorFields());
        assertEquals(scheduled, restored.getEditorFields().isPublishAfterStart());
        assertEquals(expectedPlanned ? Integer.valueOf(41) : null, request.getAttribute(PlannedAccess.PREVIEW_DOC));
        assertSame(restored, request.getAttribute("ShowdocAction.showDocData"));
        assertEquals("<p>Unsaved preview content</p>", restored.getData());
        assertEquals("/preview-test.html", request.getAttribute("path_filter_orig_path"));
        assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
        assertEquals(200, response.getStatus());
    }

    /** New pages also receive reconstructed fields without selecting another page's planned media. */
    @Test
    void restoresSerializedNewPage() throws Exception {
        DocDetails doc = previewDocument();
        doc.setDocId(-1);
        DocDetails restored = submitAndSerialize(doc);

        assertEquals("forward:/showdoc.do", controller.preview(request, response));
        assertNotNull(restored.getEditorFields());
        assertEquals(-1, restored.getDocId());
        assertNull(request.getAttribute(PlannedAccess.PREVIEW_DOC));
    }

    /** Existing editor fields keep their current media selection instead of being reconstructed. */
    @Test
    void preservesExistingEditorFields() {
        DocDetails doc = previewDocument();
        DocEditorFields fields = doc.getEditorFields();
        fields.setPlannedMedia(true);
        request.getSession().setAttribute("ShowdocAction.showDocData", doc);

        assertEquals("forward:/showdoc.do", controller.preview(request, response));
        assertSame(fields, doc.getEditorFields());
        assertEquals(41, request.getAttribute(PlannedAccess.PREVIEW_DOC));
    }

    /** Reconstructing planned media does not bypass the page-specific permission check. */
    @Test
    void deniesUnauthorizedSerializedPlannedPreview() throws Exception {
        planned.when(PlannedAccess::enabled).thenReturn(true);
        editor.when(() -> EditorDB.isPageEditable(null, 42)).thenReturn(false);
        DocDetails doc = previewDocument();
        doc.setPublicable(true);
        submitAndSerialize(doc);

        assertNull(controller.preview(request, response));
        assertEquals(404, response.getStatus());
        assertNull(request.getAttribute(PlannedAccess.PREVIEW_DOC));
        assertNull(request.getAttribute("ShowdocAction.showDocData"));
    }

    /** A session without a preview document still forwards to the existing not-found page. */
    @Test
    void handlesMissingPreview() {
        assertEquals("forward:/404.jsp", controller.preview(request, response));
    }

    private DocDetails previewDocument() {
        DocDetails doc = new DocDetails();
        doc.setDocId(42);
        doc.setData("<p>Unsaved preview content</p>");
        doc.setVirtualPath("/preview-test.html");
        doc.setGenerateUrlFromTitle(false);
        DocEditorFields fields = new DocEditorFields();
        fields.setGroupDetails(new GroupDetails());
        doc.setEditorFields(fields);
        return doc;
    }

    private DocDetails submitAndSerialize(DocDetails doc) throws Exception {
        assertEquals("{\"ok\": true}", new WebpagesDatatable(null, null, null).preview(doc, request));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(request.getSession().getAttribute("ShowdocAction.showDocData"));
        }
        DocDetails restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (DocDetails) in.readObject();
        }
        assertNull(restored.getEditorFields(), "Session serialization must reproduce the missing transient field");
        request.getSession().setAttribute("ShowdocAction.showDocData", restored);
        return restored;
    }
}
