package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

/**
 * Tests identifiers, condition-field loading, upload limits and saved selections in
 * {@link MultistepFormsService}.
 */
class MultistepFormsServiceTest {

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

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<FormSettingsService> formSettings = mockStatic(FormSettingsService.class);
            MockedStatic<XhrFileUploadServlet> uploadServlet = mockStatic(XhrFileUploadServlet.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedConstruction<IwcmFile> files = mockConstruction(IwcmFile.class, (file, context) -> {
                when(file.exists()).thenReturn(true);
                when(file.length()).thenReturn(800L * 1024L);
            });
            MockedConstruction<ComplexQuery> queries = mockFormItemsQuery("upload", "multiupload")
        ) {
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

    /**
     * Verifies that a checkbox value containing a comma round-trips exactly while
     * retaining the legacy string session value.
     */
    @Test
    void restoresCommaContainingCheckboxValue() {
        MultistepFormsService service = new MultistepFormsService(null, null, null, null, null);
        MockHttpServletRequest request = formRequest();
        Prop prop = mock(Prop.class);
        when(prop.getText("components.formsimple.input.checkbox")).thenReturn("<input type=\"checkbox\">");
        JSONObject received = new JSONObject().put("choices", new JSONArray().put("Research, development"));

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedStatic<XhrFileUploadServlet> uploads = mockStatic(XhrFileUploadServlet.class);
            MockedConstruction<ComplexQuery> queries = mockFormItemsQuery("choices", "checkbox")
        ) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);

            ReflectionTestUtils.invokeMethod(service, "saveStepData", "contact-form", 1L, received, request);
            JSONObject restored = service.getSavedStepData("contact-form", 1L, request).getFirst();

            assertEquals(List.of("Research, development"), restored.getJSONArray("choices").toList());
            String sessionKey = MultistepFormsService.getSessionKey("contact-form", request);
            assertEquals("Research, development", request.getSession().getAttribute(sessionKey + "_choices"));
        }
    }

    private static MockHttpServletRequest formRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-CSRF-Token", "test-token");
        request.setParameter("language", "en");
        return request;
    }

    private static MockedConstruction<ComplexQuery> mockFormItemsQuery(String itemFormId, String fieldType) {
        return mockConstruction(ComplexQuery.class, withSettings().defaultAnswer(RETURNS_SELF), (query, context) ->
            when(query.list(any())).thenAnswer(invocation -> {
                Mapper<?> mapper = invocation.getArgument(0);
                ResultSet row = mock(ResultSet.class);
                when(row.getString("item_form_id")).thenReturn(itemFormId);
                when(row.getString("field_type")).thenReturn(fieldType);
                mapper.map(row);
                return List.of();
            })
        );
    }
}
