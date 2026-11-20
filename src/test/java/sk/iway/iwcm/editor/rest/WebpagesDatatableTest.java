package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.system.datatable.settings.fields.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.system.datatable.settings.fields.rest.FieldsSettingsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.WebpagesService;
import tools.jackson.databind.json.JsonMapper;

/** Verifies that page edits merge field settings before entering the editor save workflow. */
class WebpagesDatatableTest {
    private WebpagesRestController controller;
    private EditorFacade facade;
    private DocDetailsRepository repository;
    private FieldsSettingsService settings;
    private DocDetails published;
    @AutoClose private MockedStatic<Constants> constants;
    @AutoClose private MockedStatic<InitServlet> initServlet;
    @AutoClose private MockedStatic<DocDB> documents;
    @AutoClose private MockedStatic<Tools> tools;
    @AutoClose private MockedStatic<CloudToolsForCore> cloud;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        MockServletContext servletContext = new MockServletContext();
        constants.when(Constants::getServletContext).thenReturn(servletContext);
        initServlet = mockStatic(InitServlet.class);
        documents = mockStatic(DocDB.class);
        tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
        cloud = mockStatic(CloudToolsForCore.class);
        cloud.when(CloudToolsForCore::getDomainId).thenReturn(9);
        settings = mock(FieldsSettingsService.class);
        tools.when(() -> Tools.getSpringBean("fieldsSettingsService", FieldsSettingsService.class)).thenReturn(settings);
        when(settings.getFieldsSettings(DocDetails.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[0]);

        DocDB database = mock(DocDB.class);
        documents.when(DocDB::getInstance).thenReturn(database);
        published = page("Published value");
        when(database.getDoc(123, -1, false)).thenReturn(published);
        facade = mock(EditorFacade.class);
        repository = mock(DocDetailsRepository.class);
        controller = spy(new WebpagesRestController(repository, facade, mock(DocAtrDefRepository.class), null, null));
        controller.setRequest(new MockHttpServletRequest());
        doNothing().when(controller).addInsertEditNotify(anyBoolean(), anyList());
        doReturn(false).when(controller).isRefreshMenuRequired(any(), any());
        when(facade.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    /** Omitted optional values come from the selected version, for work saves and publication alike. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesHiddenFieldFromSelectedHistory(boolean publish) {
        hideFieldA();
        DocDetails history = page("Work version value");
        when(facade.getExistingDocForEditor(123, 456)).thenReturn(history);
        DocDetails submitted = JsonMapper.builder().build().readValue(
            "{\"docId\":123,\"historyId\":456,\"title\":\"Changed title\",\"navbar\":\"Menu\"}", DocDetails.class);
        submitted.setEditorFields(new DocEditorFields());
        submitted.getEditorFields().setRequestPublish(publish);
        assertEquals("", submitted.getFieldA());

        DocDetails saved = controller.editItem(submitted, 123);

        assertEquals("Work version value", saved.getFieldA());
        assertEquals("Changed title", saved.getTitle());
        assertEquals(publish, saved.getEditorFields().isRequestPublish());
        assertEquals("Published value", published.getFieldA());
        verify(facade).save(submitted);
        verify(settings).getFieldsSettings(DocDetails.class.getName(), 9);
        verifyNoInteractions(repository);
    }

    /** Disabled values are retained even if a client explicitly submits a replacement. */
    @ParameterizedTest
    @ValueSource(strings = {"Stored value", ""})
    void preservesDisabledField(String storedValue) {
        FieldsSettingsEntity disabled = new FieldsSettingsEntity();
        disabled.setFieldName("fieldA");
        disabled.setDisabled(true);
        when(settings.getFieldsSettings(DocDetails.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[] {disabled});
        DocDetails stored = page(storedValue);
        when(facade.getExistingDocForEditor(123, 0)).thenReturn(stored);

        DocDetails saved = controller.editItem(page("Submitted replacement"), 123);

        assertEquals(storedValue, saved.getFieldA());
    }

    /** A full page from the tool uses the stored destination and preserves protected folder defaults on insert. */
    @Test
    void insertsFullToolBodyUsingFolderDefaults() {
        var folder = new GroupDetails();
        folder.setGroupId(25);
        var submitted = page("Submitted value");
        submitted.setDocId(-1);
        submitted.getEditorFields().setGroupDetails(folder);
        submitted.getEditorFields().setRequestPublish(false);
        hideFieldA();
        when(settings.getFieldsSettings(DocEditorFields.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[0]);
        doReturn(page("Folder default")).when(facade).getDocForEditor(-1, -1, 25);
        var request = (MockHttpServletRequest) controller.getRequest();
        request.getSession().setAttribute(Constants.USER_KEY, new Identity());
        request.getSession().setAttribute(Constants.SESSION_GROUP_ID, "10");
        request.setParameter("groupId", "25");
        var user = controller.getUser();
        try (var dispatch = mockStatic(AiToolService.class); var pages = mockStatic(WebpagesService.class)) {
            dispatch.when(() -> AiToolService.isToolRequest(request, WebpagesRestController.class)).thenReturn(true);
            assertEquals(25, controller.getDefaultOptions(Pageable.unpaged(), false).getGroupId());
            assertEquals("10", request.getSession().getAttribute(Constants.SESSION_GROUP_ID));
            pages.when(() -> WebpagesService.requireCreateFolder(user, 25)).thenReturn(folder);
            assertSame(submitted, controller.insertItem(submitted));
            assertEquals("Folder default", submitted.getFieldA());
            assertFalse(submitted.getEditorFields().isRequestPublish());
            verify(facade).save(submitted);
        }
    }

    /** The compact call saves a draft from folder defaults or requests publication through the same editor. */
    @ParameterizedTest
    @CsvSource({
        ", , , <p>Folder template</p>, About",
        "'', Menu label, false, <p>&nbsp;</p>, Menu label",
        "<p>New content</p>, Menu label, true, <p>New content</p>, Menu label"
    })
    void createsPageFromFolderWithOnlyRequestedChanges(String html, String navbar, Boolean publish, String expectedHtml, String expectedNavbar) {
        var folder = new GroupDetails();
        folder.setGroupId(25);
        folder.setGroupName("Site");
        folder.setNavbarName("Site");
        var defaults = page("Inherited value");
        defaults.setDocId(-1);
        defaults.setGroupId(25);
        defaults.setTempId(17);
        defaults.setSortPriority(80);
        defaults.setData("<p>Folder template</p>");
        defaults.setAvailable(false);
        defaults.getEditorFields().setGroupDetails(folder);
        defaults.getEditorFields().setRequestPublish(false);
        var request = (MockHttpServletRequest) controller.getRequest();
        request.setParameter("groupId", "25");
        request.getSession().setAttribute(Constants.USER_KEY, new Identity());
        var user = controller.getUser();
        doReturn(page("Inherited value")).when(facade).getDocForEditor(-1, -1, 25);
        when(settings.getFieldsSettings(DocEditorFields.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[0]);
        try (var dispatch = mockStatic(AiToolService.class); var pages = mockStatic(WebpagesService.class);
                var factory = Validation.buildDefaultValidatorFactory()) {
            controller.setValidator(factory.getValidator());
            dispatch.when(() -> AiToolService.isToolRequest(request, WebpagesRestController.class)).thenReturn(true);
            pages.when(() -> WebpagesService.requireCreateFolder(user, 25)).thenReturn(folder);
            pages.when(() -> WebpagesService.getOneItem(eq(-1L), eq(25), eq(-1), eq(facade), any(), anyList(), eq(request)))
                    .thenReturn(defaults);
            var changes = new WebpagesRestController.NewPage("About", html, navbar, publish);
            var response = controller.createFromFolder(changes);

            assertEquals(201, response.getStatusCode().value());
            var saved = response.getBody();
            assertEquals("About", saved.getTitle());
            assertEquals(expectedNavbar, saved.getNavbar());
            assertEquals(expectedHtml, saved.getData());
            assertEquals(Boolean.TRUE.equals(publish), saved.isAvailable());
            assertEquals(Boolean.TRUE.equals(publish), saved.getEditorFields().isRequestPublish());
            assertEquals(25, saved.getGroupId());
            assertSame(folder, saved.getEditorFields().getGroupDetails());
            assertEquals(17, saved.getTempId());
            assertEquals(80, saved.getSortPriority());
            assertEquals("Inherited value", saved.getFieldA());
            verify(facade).save(saved);
        }
    }

    /** An invalid tool destination cannot fall back to the current editor folder. */
    @Test
    void compactCreateRequiresExplicitPositiveFolder() {
        var request = (MockHttpServletRequest) controller.getRequest();
        request.setParameter("groupId", "0");
        request.getSession().setAttribute(Constants.USER_KEY, new Identity());
        request.getSession().setAttribute(Constants.SESSION_GROUP_ID, "25");
        try (var dispatch = mockStatic(AiToolService.class); var pages = mockStatic(WebpagesService.class)) {
            dispatch.when(() -> AiToolService.isToolRequest(request, WebpagesRestController.class)).thenReturn(true);
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.createFromFolder(
                    new WebpagesRestController.NewPage("About", null, null, null))).getStatusCode().value());
            pages.verifyNoInteractions();
            verifyNoInteractions(facade);
        }
    }

    private void hideFieldA() {
        var hidden = new FieldsSettingsEntity();
        hidden.setFieldName("fieldA");
        hidden.setHiddenEditor(true);
        when(settings.getFieldsSettings(DocDetails.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[] {hidden});
    }

    /** Empty strings still clear editable optional fields. */
    @Test
    void allowsClearingEditableField() {
        DocDetails stored = page("Stored value");
        when(facade.getExistingDocForEditor(123, 0)).thenReturn(stored);

        DocDetails saved = controller.editItem(page(""), 123);

        assertEquals("", saved.getFieldA());
    }

    /** Required fields remain subject to validation before any stored values are merged. */
    @Test
    void rejectsMissingRequiredTitleBeforeMerge() {
        DocDetails submitted = JsonMapper.builder().build().readValue(
            "{\"docId\":123,\"navbar\":\"Menu\"}", DocDetails.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            controller.setValidator(factory.getValidator());
            assertThrows(ConstraintViolationException.class, () -> controller.edit(123, submitted));
        }
        verifyNoInteractions(facade, repository);
    }

    private DocDetails page(String fieldA) {
        DocDetails page = new DocDetails();
        page.setDocId(123);
        page.setTitle("Original title");
        page.setNavbar("Menu");
        page.setFieldA(fieldA);
        page.setEditorFields(new DocEditorFields());
        return page;
    }
}
