package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import jakarta.validation.ValidatorFactory;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
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
    @AutoClose private static final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
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
        controller.setRequest(request);
        controller.setValidator(validators.getValidator());
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

    @Nested
    class ToolPageCreation {
        @AutoClose final MockedStatic<AiToolService> dispatch = mockStatic(AiToolService.class);
        @AutoClose final MockedStatic<WebpagesService> pages = mockStatic(WebpagesService.class);
        final GroupDetails folder = new GroupDetails();
        final DocDetails draft = page("Inherited value");

        @BeforeEach
        void selectFolder() {
            var user = new Identity();
            request.getSession().setAttribute(Constants.USER_KEY, user);
            request.getSession().setAttribute(Constants.SESSION_GROUP_ID, "10");
            request.setParameter("groupId", "25");
            folder.setGroupId(25);
            draft.setDocId(-1);
            draft.setGroupId(25);
            draft.getEditorFields().setGroupDetails(folder);
            draft.getEditorFields().setRequestPublish(false);
            dispatch.when(() -> AiToolService.isToolRequest(request, WebpagesRestController.class)).thenReturn(true);
            pages.when(() -> WebpagesService.requireCreateFolder(user, 25)).thenReturn(folder);
            when(settings.getFieldsSettings(DocEditorFields.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[0]);
            doReturn(page("Folder default")).when(facade).getDocForEditor(-1, -1, 25);
        }

        @Test
        void fullBodyKeepsProtectedFolderDefaultAndCurrentEditorFolder() {
            hideFieldA();
            assertEquals(25, controller.getDefaultOptions(Pageable.unpaged(), false).getGroupId());
            assertEquals("10", request.getSession().getAttribute(Constants.SESSION_GROUP_ID));
            assertSame(draft, controller.insertItem(draft));
            assertEquals("Folder default", draft.getFieldA());
            assertFalse(draft.getEditorFields().isRequestPublish());
            verify(facade).save(draft);
        }

        @ParameterizedTest
        @CsvSource({
            ", , , <p>Folder template</p>, About",
            "'', Menu label, false, <p>&nbsp;</p>, Menu label",
            "<p>New content</p>, Menu label, true, <p>New content</p>, Menu label"
        })
        void compactBodyOverridesOnlyRequestedDefaults(String html, String navbar, Boolean publish, String expectedHtml, String expectedNavbar) {
            draft.setTempId(17);
            draft.setSortPriority(80);
            draft.setData("<p>Folder template</p>");
            pages.when(() -> WebpagesService.getOneItem(eq(-1L), eq(25), eq(-1), eq(facade), any(), anyList(), eq(request)))
                    .thenReturn(draft);
            DatatableRestControllerV2.addNotify(new NotifyBean("Previous request", "Old message", NotifyBean.NotifyType.WARNING));
            when(facade.save(draft)).thenReturn(published);
            var response = controller.createFromFolder(new WebpagesRestController.NewPage("About", html, navbar, publish));
            assertEquals(201, response.getStatusCode().value());
            assertEquals(published.getDocId(), response.getBody().id());
            assertFalse(controller.hasNotify());
            assertEquals("About", draft.getTitle());
            assertEquals(expectedNavbar, draft.getNavbar());
            assertEquals(expectedHtml, draft.getData());
            assertEquals(Boolean.TRUE.equals(publish), draft.isAvailable());
            assertEquals(Boolean.TRUE.equals(publish), draft.getEditorFields().isRequestPublish());
            assertEquals(25, draft.getGroupId());
            assertSame(folder, draft.getEditorFields().getGroupDetails());
            assertEquals(17, draft.getTempId());
            assertEquals(80, draft.getSortPriority());
            assertEquals("Inherited value", draft.getFieldA());
            verify(facade).save(draft);
        }

        @Test
        void invalidFolderCannotFallBackToCurrentEditorFolder() {
            request.setParameter("groupId", "0");
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
        assertThrows(ConstraintViolationException.class, () -> controller.edit(123, submitted));
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
