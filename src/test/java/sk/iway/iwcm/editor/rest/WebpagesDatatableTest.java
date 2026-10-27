package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.datatable.settings.fields.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.system.datatable.settings.fields.rest.FieldsSettingsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import tools.jackson.databind.json.JsonMapper;

/** Verifies that page edits merge field settings before entering the editor save workflow. */
class WebpagesDatatableTest {
    private WebpagesDatatable controller;
    private EditorFacade facade;
    private DocDetailsRepository repository;
    private FieldsSettingsService settings;
    private DocDetails published;
    private MockedStatic<Constants> constants;
    private MockedStatic<InitServlet> initServlet;
    private MockedStatic<DocDB> documents;
    private MockedStatic<Tools> tools;
    private MockedStatic<CloudToolsForCore> cloud;

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
        controller = spy(new WebpagesDatatable(repository, facade, mock(DocAtrDefRepository.class)));
        controller.setRequest(new MockHttpServletRequest());
        doNothing().when(controller).addInsertEditNotify(anyBoolean(), anyList());
        doReturn(false).when(controller).isRefreshMenuRequired(any(), any());
        when(facade.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        cloud.close();
        tools.close();
        documents.close();
        initServlet.close();
        constants.close();
    }

    /** Omitted optional values come from the selected version, for work saves and publication alike. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesHiddenFieldFromSelectedHistory(boolean publish) {
        FieldsSettingsEntity hidden = new FieldsSettingsEntity();
        hidden.setFieldName("fieldA");
        hidden.setHiddenEditor(true);
        when(settings.getFieldsSettings(DocDetails.class.getName(), 9)).thenReturn(new FieldsSettingsEntity[] {hidden});
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
