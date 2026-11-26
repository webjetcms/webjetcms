package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.layout.LayoutBean;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;

/** Checks the form editor view and its administration endpoints with the same form permission. */
class FormEditorAccessTest {
    private final FormsServiceImpl forms = mock(FormsServiceImpl.class);
    private final FormStepsRepository steps = mock(FormStepsRepository.class);
    private final Identity editor = new Identity();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    /** An allowed form shows its tables. A denied form omits the editor and has a translated notification. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void editorRespectsFormAccess(boolean allowed) {
        request.addParameter("formName", "contact");
        request.getSession().setAttribute(Constants.USER_KEY, editor);
        when(forms.isFormAccessible("contact", editor)).thenReturn(allowed);

        try (StaticApplicationContext spring = new StaticApplicationContext()) {
            spring.getBeanFactory().registerSingleton("formsServiceImpl", forms);
            spring.refresh();
            spring.addMessage("admin.operationPermissionDenied", Locale.ENGLISH, "Permission denied");
            spring.addMessage("components.forms.permsDeniedNote", Locale.ENGLISH, "Check your page permissions.");
            var web = JakartaServletWebApplication.buildApplication(request.getServletContext());
            WebContext context = new WebContext(web.buildExchange(request, new MockHttpServletResponse()), Locale.ENGLISH);
            context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                    new ThymeleafEvaluationContext(spring, null));
            var layout = mock(LayoutBean.class, RETURNS_DEEP_STUBS);
            when(layout.getUser()).thenReturn(editor);
            context.setVariable("layout", layout);
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateEngineMessageSource(spring);
            engine.setTemplateResolver(new FileTemplateResolver());
            String html = engine.process("src/main/webapp/apps/form/admin/form-steps.html", context);
            var document = Jsoup.parse(html);
            assertEquals(allowed, document.getElementById("formStepsDataTable") != null);
            assertEquals(allowed, document.getElementById("formItemsDataTable") != null);
            assertEquals(allowed, document.selectFirst(".stepPreviewWrapper") != null);
            assertTrue(html.contains("WJ.notifyError(\"Permission denied\", \"Check your page permissions.\""));

            String statsHtml = engine.process("src/main/webapp/apps/form/admin/form-stats.html", context);
            var stats = Jsoup.parse(statsHtml);
            for (String id : List.of("responsesCount", "chartContainer", "bonusChartContainer", "filter")) {
                assertEquals(allowed, stats.getElementById(id) != null, id);
            }
            assertTrue(statsHtml.contains("WJ.notifyError(\"Permission denied\", \"Check your page permissions.\""));

            String chartEditor = engine.process("src/main/webapp/apps/form/admin/form-stats-table.html", context);
            assertEquals(allowed, Jsoup.parse(chartEditor).getElementById("formItemsDataTable") != null);
            assertTrue(chartEditor.contains("WJ.notifyError(\"Permission denied\", \"Check your page permissions.\""));
        }
    }

    /** Both statistics endpoints deny access before loading the form or calculating any metrics. */
    @Test
    void deniesStatisticsBeforeLoadingData() {
        request.getSession().setAttribute(Constants.USER_KEY, editor);
        doCallRealMethod().when(forms).requireFormAccess("contact", editor);
        var multistep = mock(MultistepFormsService.class);
        var controller = new FormStatRestController(new FormStatService(null, null, forms, null, multistep));
        for (var response : List.of(controller.getFormStepHtml("contact", request),
                controller.getFormStepHtml("contact", "rating", request))) {
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertFalse(response.hasBody());
        }
        verifyNoInteractions(multistep);
    }

    /** Neither loading nor filtering any of the editor tables may read rows of a denied form. */
    @Test
    void deniesTableReadsBeforeLoadingRowsOrOptions() {
        request.addParameter("formName", "contact");
        request.getSession().setAttribute(Constants.USER_KEY, editor);
        doCallRealMethod().when(forms).requireFormAccess("contact", editor);
        FormItemsRepository items = mock(FormItemsRepository.class);
        FormItemsConditionsRepository conditions = mock(FormItemsConditionsRepository.class);
        var pageable = PageRequest.of(0, 10);
        for (var table : List.of(new FormStepsRestController(steps, items, null, forms),
                new FormItemsRestController(items, null, null, null, conditions, steps, forms),
                new FormItemsConditionsRestController(conditions, null, items, forms))) {
            table.setRequest(request);
            assertThrows(AccessDeniedException.class, () -> table.getAllItems(pageable));
            assertThrows(AccessDeniedException.class, () -> table.findByColumns(new HashMap<>(Map.of("formName", "contact")), pageable, null));
        }
        verifyNoInteractions(steps, items, conditions);
    }

    /** Preview requires access to the form and a step belonging to that form in the current domain. */
    @Test
    void previewChecksBothFormAndStep() {
        var controller = new FormStepsRestController(steps, null, null, forms);
        request.getSession().setAttribute(Constants.USER_KEY, editor);
        controller.setRequest(request);
        assertEquals(HttpStatus.FORBIDDEN, controller.getFormStepHtml("contact", 7L, "sk", request).getStatusCode());

        when(forms.isFormAccessible("contact", editor)).thenReturn(true);
        when(steps.getValidStep("contact", 7L, CloudToolsForCore.getDomainId())).thenReturn(Optional.empty());
        var response = controller.getFormStepHtml("contact", 7L, "sk", request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.hasBody());

        when(steps.getValidStep("contact", 7L, CloudToolsForCore.getDomainId())).thenReturn(Optional.of(new FormStepEntity()));
        try (var renderer = mockConstruction(FormHtmlHandler.class, (handler, context) ->
                when(handler.getFormStepHtml(7L, request)).thenReturn("<form></form>"))) {
            assertEquals("<form></form>", controller.getFormStepHtml("contact", 7L, "sk", request).getBody());
        }
    }

    /** The condition being opened must belong to the selected form and field. */
    @Test
    void conditionDetailUsesStoredOwner() {
        var conditions = mock(FormItemsConditionsRepository.class);
        var controller = new FormItemsConditionsRestController(conditions, null, null, forms);
        request.addParameter("formName", "contact");
        request.addParameter("itemId", "3");
        request.getSession().setAttribute(Constants.USER_KEY, editor);
        when(forms.isFormAccessible("contact", editor)).thenReturn(true);
        controller.setRequest(request);
        FormItemsConditionEntity stored = new FormItemsConditionEntity();
        stored.setFormItemId(3L);
        stored.setFormName("other");
        when(conditions.findFirstByIdAndDomainId(8L, CloudToolsForCore.getDomainId())).thenReturn(Optional.of(stored));
        assertFalse(controller.checkItemPerms(stored, 8L));
        stored.setFormName("contact");
        assertTrue(controller.checkItemPerms(stored, 8L));
    }
}
