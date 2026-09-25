package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.ConditionType;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.OperatorType;
import sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.i18n.Prop;

/**
 * Verifies condition evaluation and session updates when a previously saved step is resubmitted.
 */
class MultistepFormStepValidationTest {

    /**
     * Uses cleared current-step values for visibility and requirement conditions while retaining
     * submitted values and session-backed conditions from previous steps.
     */
    @ParameterizedTest
    @CsvSource({
        "VISIBILITY, true, false, true",
        "REQUIREMENT, true, false, true",
        "VISIBILITY, true, true, false",
        "REQUIREMENT, true, true, false",
        "VISIBILITY, false, false, false",
        "REQUIREMENT, false, false, false"
    })
    void validatesResubmittedStep(ConditionType conditionType, boolean checkboxInCurrentStep,
            boolean checkboxSubmitted, boolean shouldProceed) throws Exception {
        FormItemsRepository items = mock(FormItemsRepository.class);
        FormStepsRepository steps = mock(FormStepsRepository.class);
        FormSettingsRepository settings = mock(FormSettingsRepository.class);
        FormItemsConditionsRepository conditions = mock(FormItemsConditionsRepository.class);
        MultistepFormsService service = new MultistepFormsService(null, null, items, steps, settings);
        FormStepEntity currentStep = new FormStepEntity();
        currentStep.setId(2L);
        currentStep.setCurrentPosition(2);
        currentStep.setMaxPosition(3);
        FormStepEntity nextStep = new FormStepEntity();
        nextStep.setId(3L);
        when(steps.getValidStep("contact-form", 2L, 1)).thenReturn(Optional.of(currentStep));
        when(steps.getStepByPosition("contact-form", 3, 1)).thenReturn(Optional.of(nextStep));
        when(settings.findByFormNameAndDomainId("contact-form", 1)).thenReturn(new FormSettingsEntity());

        FormItemsConditionEntity condition = new FormItemsConditionEntity();
        condition.setItemFormId("invoice");
        condition.setOperator(OperatorType.EQUALS);
        condition.setValue("yes");
        when(conditions.findAllByFormItemIdAndConditionTypeAndDomainIdOrderBySortPriorityAsc(10L, conditionType, 1))
            .thenReturn(List.of(condition));

        ResultSet company = fieldRow(10L, "company", "text", true);
        ResultSet invoice = fieldRow(11L, "invoice", "checkbox", false);
        Prop prop = mock(Prop.class);
        when(prop.getText("checkform.title.required")).thenReturn("Required field");
        when(prop.getText("components.formsimple.input.checkbox")).thenReturn("<input type=\"checkbox\">");
        FormDB formDB = mock(FormDB.class);
        when(formDB.getAllRegularExpressionAsEntity()).thenReturn(List.of());
        Cache cache = mock(Cache.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-CSRF-Token", "test-token");
        request.setParameter("language", "en");
        request.setCookies(new Cookie("JSESSIONID", "test-session"));
        JSONObject payload = new JSONObject().put("f1-note", "Updated note");
        if (checkboxSubmitted) payload.put("f1-invoice", "yes");
        request.setContent(payload.toString().getBytes(StandardCharsets.UTF_8));

        try (
            MockedStatic<Constants> constants = mockStatic(Constants.class);
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<Prop> props = mockStatic(Prop.class);
            MockedStatic<FormDB> formDatabases = mockStatic(FormDB.class);
            MockedStatic<Cache> caches = mockStatic(Cache.class);
            MockedStatic<Tools> tools = mockStatic(Tools.class, invocation -> {
                if ("getSpringBean".equals(invocation.getMethod().getName())) {
                    return "formItemsConditionsRepository".equals(invocation.getArgument(0)) ? conditions : items;
                }
                return invocation.callRealMethod();
            });
            MockedConstruction<ComplexQuery> queries = mockConstruction(ComplexQuery.class,
                withSettings().defaultAnswer(RETURNS_SELF), (query, context) ->
                    when(query.list(any())).thenAnswer(invocation -> {
                        Mapper<?> mapper = invocation.getArgument(0);
                        // Evaluate the dependent field first to catch order-dependent normalization.
                        mapper.map(company);
                        if (checkboxInCurrentStep) mapper.map(invoice);
                        mapper.map(fieldRow(12L, "note", "text", false));
                        return List.of();
                    })
            )
        ) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);
            formDatabases.when(FormDB::getInstance).thenReturn(formDB);
            caches.when(Cache::getInstance).thenReturn(cache);

            String sessionKey = MultistepFormsService.getSessionKey("contact-form", request);
            request.getSession().setAttribute(sessionKey + MultistepFormApp.PERMITTED, true);
            request.getSession().setAttribute(sessionKey + MultistepFormApp.COUNTER, 1);
            request.getSession().setAttribute(sessionKey + "_invoice", "yes");
            request.getSession().setAttribute(sessionKey + "_company", "Previous company");
            request.getSession().setAttribute(sessionKey + "_note", "Previous note");

            JSONObject response = new JSONObject();
            service.saveFormStep("contact-form", 2L, request, response);

            if (shouldProceed) {
                assertFalse(response.has("fieldErrors"));
                assertEquals(3L, response.getLong("step-id"));
                assertEquals("", request.getSession().getAttribute(sessionKey + "_invoice"));
                assertEquals("", request.getSession().getAttribute(sessionKey + "_company"));
                assertEquals("Updated note", request.getSession().getAttribute(sessionKey + "_note"));
            } else {
                assertTrue(response.getJSONObject("fieldErrors").has("company"));
                assertFalse(response.has("step-id"));
                assertEquals("yes", request.getSession().getAttribute(sessionKey + "_invoice"));
                assertEquals("Previous company", request.getSession().getAttribute(sessionKey + "_company"));
            }
        }
    }

    private ResultSet fieldRow(long id, String fieldId, String fieldType, boolean required) throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getLong("id")).thenReturn(id);
        when(row.getString("item_form_id")).thenReturn(fieldId);
        when(row.getString("label")).thenReturn(fieldId);
        when(row.getString("field_type")).thenReturn(fieldType);
        when(row.getBoolean("required")).thenReturn(required);
        return row;
    }
}
