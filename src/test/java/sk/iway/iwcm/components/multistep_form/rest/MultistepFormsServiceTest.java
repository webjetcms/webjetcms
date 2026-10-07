package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.forms.FormsController;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * Tests caching, identifiers, conditions, uploads, and saved selections in {@link MultistepFormsService}.
 */
class MultistepFormsServiceTest {

    /**
     * Verifies cached copies are reused and reloaded after administrative changes or cluster eviction.
     */
    @ParameterizedTest
    @ValueSource(strings = {"local", "cluster", "itemSave", "itemDelete", "stepSave", "stepDelete", "stepDuplicate", "formDuplicate", "formDelete"})
    void cachesValidationFieldsUntilInvalidated(String operation) {
        String formName = "contact-form";
        String otherDomainKey = "multistep_form.validationFields.10.other";
        FormItemsRepository repository = mock(FormItemsRepository.class);
        FormItemEntity field = validationField("name", "text");
        field.setFormName(formName);
        field.setDomainId(1);
        when(repository.findAllForValidation(formName, 1)).thenReturn(List.of(field));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<ClusterDB> clusters = mockStatic(ClusterDB.class);
                MockedStatic<Tools> tools = mockValidationFields(repository)) {
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            Cache cache = Cache.getInstance();
            cache.setObjectSeconds(otherDomainKey, List.of(field), 3600, false);

            MultistepFormsService.getFormItemsForValidation(formName).get(0).setLabel("Changed by caller");
            field.setLabel("Updated in database");
            assertEquals("name", MultistepFormsService.getFormItemsForValidation(formName).get(0).getLabel());
            verify(repository).findAllForValidation(formName, 1);

            invalidateValidationFields(operation, field, repository);

            assertEquals("Updated in database", MultistepFormsService.getFormItemsForValidation(formName).get(0).getLabel());
            verify(repository, times(2)).findAllForValidation(formName, 1);
            assertEquals(List.of(field), cache.getObject(otherDomainKey));
        }
    }

    /** Invokes the production operation responsible for invalidating a warmed validation cache. */
    private void invalidateValidationFields(String operation, FormItemEntity field, FormItemsRepository items) {
        String formName = field.getFormName();
        MultistepFormsService service = mock(MultistepFormsService.class);
        FormStepEntity step = new FormStepEntity();
        step.setId(field.getStepId());
        step.setFormName(formName);
        step.setDomainId(field.getDomainId());

        switch(operation) {
            case "local" -> MultistepFormsService.clearValidationFieldsCache(formName, field.getDomainId());
            case "cluster" -> MultistepFormsService.refresh(field.getDomainId());
            case "itemSave", "itemDelete" -> {
                FormItemsRestController controller = mock(FormItemsRestController.class, CALLS_REAL_METHODS);
                ReflectionTestUtils.setField(controller, "multistepFormsService", service);
                doReturn(new MockHttpServletRequest()).when(controller).getRequest();
                if("itemSave".equals(operation)) controller.afterSave(field, field);
                else controller.afterDelete(field, 1L);
            }
            case "stepSave", "stepDelete", "stepDuplicate" -> {
                FormStepsRestController controller = mock(FormStepsRestController.class, CALLS_REAL_METHODS);
                ReflectionTestUtils.setField(controller, "multistepFormsService", service);
                ReflectionTestUtils.setField(controller, "formItemsRepository", items);
                doReturn(false).when(controller).isDuplicate();
                doReturn(new Identity()).when(controller).getUser();
                if("stepSave".equals(operation)) controller.afterSave(step, step);
                else if("stepDelete".equals(operation)) controller.afterDelete(step, step.getId());
                else controller.afterDuplicate(step, 2L);
            }
            case "formDuplicate" -> {
                FormsController controller = mock(FormsController.class, CALLS_REAL_METHODS);
                doReturn(true).when(controller).isDuplicate();
                FormsEntity form = new FormsEntity();
                form.setFormName(formName);
                form.setDomainId(field.getDomainId());
                try {
                    controller.afterSave(form, form);
                } finally {
                    FormsController.setRedirect(null);
                }
            }
            case "formDelete" -> {
                FormsEntity form = new FormsEntity();
                form.setId(1L);
                form.setFormName(formName);
                form.setDomainId(field.getDomainId());
                FormsRepository forms = mock(FormsRepository.class);
                FormStepsRepository steps = mock(FormStepsRepository.class);
                when(forms.findFirstByIdAndDomainId(form.getId(), field.getDomainId())).thenReturn(Optional.of(form));
                FormsService<FormsRepository, FormsEntity> formsService = new FormsService<>(forms, null, steps, items);
                assertTrue(formsService.deleteItem(form, form.getId(), steps, items, null));
            }
            default -> throw new IllegalArgumentException("Unknown cache invalidation operation: " + operation);
        }
    }

    /**
     * Verifies that item form IDs remain unique across fields with different types.
     */
    @Test
    void generatesUniqueItemFormIdAcrossFieldTypes() {
        FormItemsRepository formItemsRepository = mock(FormItemsRepository.class);
        MultistepFormsService service = new MultistepFormsService(null, null, formItemsRepository, null, null);
        FormItemEntity emailField = new FormItemEntity();
        emailField.setFormName("contact-form");
        emailField.setFieldType("email");
        emailField.setLabel("Kontakt");

        when(formItemsRepository.getItemFormIds("contact-form", 1)).thenReturn(List.of("kontakt-1"));

        assertEquals("kontakt-2", service.getValidItemFormId(emailField));
        verify(formItemsRepository).getItemFormIds("contact-form", 1);
    }

    /**
     * Verifies that condition-field loading preserves step IDs larger than the integer range.
     */
    @Test
    void keepsLongStepIdsWhenLoadingConditionFields() {
        long stepId = (long) Integer.MAX_VALUE + 10L;
        FormItemsRepository formItemsRepository = mock(FormItemsRepository.class);
        FormStepsRepository formStepsRepository = mock(FormStepsRepository.class);
        Prop prop = mock(Prop.class);
        MultistepFormsService service = new MultistepFormsService(null, null, formItemsRepository, formStepsRepository, null);

        FormStepEntity step = new FormStepEntity();
        step.setId(stepId);
        step.setCurrentPosition(2);

        when(formStepsRepository.getStepsUpToPosition("contact-form", 2, 1)).thenReturn(List.of(step));
        when(formItemsRepository.findAllByFormNameAndStepIdInAndDomainId("contact-form", List.of(stepId), 1)).thenReturn(List.of());

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedConstruction<SimpleQuery> simpleQueries = mockConstruction(
                SimpleQuery.class,
                (query, context) -> when(query.forInt(
                    "SELECT current_position FROM form_steps WHERE id = ? AND domain_id = ?",
                    stepId,
                    1
                )).thenReturn(2)
            )
        ) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);

            assertEquals(List.of(), service.getAvailableConditionFields("contact-form", stepId, prop));
            verify(formItemsRepository).findAllByFormNameAndStepIdInAndDomainId("contact-form", List.of(stepId), 1);
            assertEquals(1, simpleQueries.constructed().size());
        }
    }

    /**
     * Verifies that array upload values cannot bypass the combined file size limit.
     */
    @Test
    void enforcesCombinedUploadLimitForArrayValues() {
        FormItemsRepository repository = mock(FormItemsRepository.class);
        MultistepFormsService service = new MultistepFormsService(null, null, repository, null, null);
        MockHttpServletRequest request = formRequest();
        FormSettingsEntity settings = new FormSettingsEntity();
        FormFileRestriction restriction = mock(FormFileRestriction.class);
        XhrFileUploadService uploads = mock(XhrFileUploadService.class);
        Prop prop = mock(Prop.class);
        FormItemEntity uploadItem = new FormItemEntity();
        uploadItem.setItemFormId("upload");
        uploadItem.setFieldType("multiupload");
        JSONObject received = new JSONObject()
            .put("Multiupload.formElementName", "upload")
            .put("upload", new JSONArray().put("fileA;fileB"));

        when(restriction.getMaxCombinedSizeInKilobytes()).thenReturn(1500L);
        when(uploads.getTempFilePath(anyString())).thenAnswer(invocation -> "/tmp/" + invocation.getArgument(0));
        when(repository.findAllForValidation("contact-form", 1)).thenReturn(List.of(validationField("upload", "multiupload")));

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<FormSettingsService> formSettings = mockStatic(FormSettingsService.class);
            MockedStatic<XhrFileUploadServlet> uploadServlet = mockStatic(XhrFileUploadServlet.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedConstruction<IwcmFile> files = mockConstruction(IwcmFile.class, (file, context) -> {
                when(file.exists()).thenReturn(true);
                when(file.length()).thenReturn(800L * 1024L);
            });
            MockedStatic<Cache> caches = mockStatic(Cache.class);
            MockedStatic<Tools> tools = mockValidationFields(repository)
        ) {
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            formSettings.when(() -> FormSettingsService.getFileRestriction("contact-form", settings)).thenReturn(restriction);
            uploadServlet.when(XhrFileUploadServlet::getService).thenReturn(uploads);
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);

            UndeclaredThrowableException exception = assertThrows(UndeclaredThrowableException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "validateFileFields", "contact-form", settings, List.of(uploadItem), received, new HashMap<String, String>(), request)
            );

            assertInstanceOf(SaveFormException.class, exception.getCause());
        }
    }

    /** Escapes user-controlled filenames in duplicate upload errors. */
    @Test
    void escapesFilenameInDuplicateUploadError() {
        FormItemsRepository repository = mock(FormItemsRepository.class);
        MultistepFormsService service = new MultistepFormsService(null, null, repository, null, null);
        MockHttpServletRequest request = formRequest();
        FormSettingsEntity settings = new FormSettingsEntity();
        XhrFileUploadService uploads = mock(XhrFileUploadService.class);
        Prop prop = mock(Prop.class, CALLS_REAL_METHODS);
        doReturn("The file {1} has been uploaded multiple times.").when(prop).getText("multistep_form.duplicate_file");
        FormItemEntity uploadItem = validationField("upload", "multiupload");
        JSONObject received = new JSONObject().put("upload", "fileA;fileB");
        HashMap<String, String> errors = new HashMap<>();

        when(uploads.getTempFilePath(anyString())).thenAnswer(invocation -> "/tmp/" + invocation.getArgument(0));
        when(uploads.getTempFileName(anyString())).thenReturn("temporary-upload.pdf");
        when(uploads.getOriginalFileName(anyString())).thenReturn("report<b> & \"draft\".pdf");
        when(repository.findAllForValidation("contact-form", 1)).thenReturn(List.of(uploadItem));

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<FormSettingsService> formSettings = mockStatic(FormSettingsService.class);
            MockedStatic<XhrFileUploadServlet> uploadServlet = mockStatic(XhrFileUploadServlet.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedStatic<Cache> caches = mockStatic(Cache.class);
            MockedStatic<Tools> tools = mockValidationFields(repository)
        ) {
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            uploadServlet.when(XhrFileUploadServlet::getService).thenReturn(uploads);
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);

            ReflectionTestUtils.invokeMethod(service, "validateFileFields", "contact-form", settings, List.of(uploadItem), received, errors, request);

            assertEquals("The file report&lt;b&gt; &amp; &quot;draft&quot;.pdf has been uploaded multiple times.", errors.get("upload"));
        }
    }

    /**
     * Verifies that a checkbox value containing a comma round-trips exactly while
     * retaining the legacy string session value.
     */
    @Test
    void restoresCommaContainingCheckboxValue() {
        FormItemsRepository repository = mock(FormItemsRepository.class);
        when(repository.findAllForValidation("contact-form", 1)).thenReturn(List.of(validationField("choices", "checkbox")));
        MultistepFormsService service = new MultistepFormsService(null, null, null, null, null);
        MockHttpServletRequest request = formRequest();
        MockHttpSession session = spy(new MockHttpSession());
        request.setSession(session);
        Prop prop = mock(Prop.class);
        when(prop.getText("components.formsimple.input.checkbox")).thenReturn("<input type=\"checkbox\">");
        JSONObject received = new JSONObject().put("choices", new JSONArray().put("Research, development"));

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedStatic<XhrFileUploadServlet> uploads = mockStatic(XhrFileUploadServlet.class);
            MockedStatic<Cache> caches = mockStatic(Cache.class);
            MockedStatic<Tools> tools = mockValidationFields(repository)
        ) {
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);

            ReflectionTestUtils.invokeMethod(service, "saveStepData", "contact-form", 1L, received, request);
            JSONObject restored = service.getSavedStepData("contact-form", 1L, request).getFirst();

            assertEquals(List.of("Research, development"), restored.getJSONArray("choices").toList());
            String sessionKey = MultistepFormsService.getSessionKey("contact-form", request);
            verify(session).getAttribute(sessionKey + "_choices");
            assertEquals("Research, development", request.getSession().getAttribute(sessionKey + "_choices"));

            session.setAttribute(sessionKey + "_choices", "Changed by processor");
            assertEquals("Changed by processor", service.getSavedStepData("contact-form", 1L, request).first.getString("choices"));
        }
    }

    /** Leaves headers without known field placeholders unchanged without accessing the session. */
    @Test
    void skipsSessionForHeadersWithoutFieldPlaceholders() {
        FormItemsRepository repository = mock(FormItemsRepository.class);
        when(repository.findAllForValidation("contact-form", 1)).thenReturn(List.of(validationField("name", "text")));
        HttpServletRequest request = mock(HttpServletRequest.class);
        try (
            MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
            MockedStatic<Cache> caches = mockStatic(Cache.class);
            MockedStatic<Tools> tools = mockValidationFields(repository)
        ) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
            for (String header : List.of("Thank you", "Thank you!", "Hello !unknown!")) {
                assertEquals(header, MultistepFormsService.updateFormValues("contact-form", request, new StringBuilder(header)).toString());
            }
            verifyNoInteractions(request);
        }
    }

    private static MockHttpServletRequest formRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-CSRF-Token", "test-token");
        request.setParameter("language", "en");
        return request;
    }

    private static MockedStatic<Tools> mockValidationFields(FormItemsRepository repository) {
        MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
        tools.when(() -> Tools.getSpringBean("formItemsRepository", FormItemsRepository.class)).thenReturn(repository);
        return tools;
    }

    private static FormItemEntity validationField(String itemFormId, String fieldType) {
        FormItemEntity field = new FormItemEntity();
        field.setStepId(1L);
        field.setItemFormId(itemFormId);
        field.setLabel(itemFormId);
        field.setFieldType(fieldType);
        return field;
    }
}
