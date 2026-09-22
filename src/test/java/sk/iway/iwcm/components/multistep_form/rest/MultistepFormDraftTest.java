package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.CSRF;

/** Verifies draft filtering, sanitization, isolation and request checks not covered by browser navigation tests. */
class MultistepFormDraftTest {
    private final String formName = "autotest-draft";
    private final FormItemsRepository items = mock(FormItemsRepository.class);
    private final FormStepsRepository steps = mock(FormStepsRepository.class);
    private final FormSettingsRepository settings = mock(FormSettingsRepository.class);
    private final SaveFormService save = mock(SaveFormService.class);
    private final Prop prop = mock(Prop.class);
    private final MultistepFormsService service = new MultistepFormsService(save, null, items, steps, settings);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private String sessionKey;
    private MockedStatic<Constants> constants;
    private MockedStatic<CloudToolsForCore> cloud;
    private MockedStatic<Prop> props;
    private MockedStatic<FormDB> databases;
    private MockedStatic<Cache> caches;
    private MockedStatic<Tools> tools;
    private MockedStatic<XhrFileUploadServlet> uploadServlet;
    private MockedStatic<CSRF> csrf;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        cloud = mockStatic(CloudToolsForCore.class);
        props = mockStatic(Prop.class);
        databases = mockStatic(FormDB.class);
        caches = mockStatic(Cache.class);
        uploadServlet = mockStatic(XhrFileUploadServlet.class);
        csrf = mockStatic(CSRF.class);
        tools = mockStatic(Tools.class, invocation -> {
            if ("getSpringBean".equals(invocation.getMethod().getName())) return items;
            return invocation.callRealMethod();
        });
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(() -> Constants.getString("spamProtectionJavascript")).thenReturn("formmailCsrf");
        cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
        props.when(() -> Prop.getInstance("en")).thenReturn(prop);
        caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
        csrf.when(() -> CSRF.verifyTokenAjax(any(HttpSession.class), eq("autotest-token"))).thenReturn(true);
        for (long id = 2; id <= 3; id++) {
            FormStepEntity step = new FormStepEntity();
            step.setId(id);
            step.setCurrentPosition((int) id);
            step.setMaxPosition(3);
            when(steps.getValidStep(formName, id, 1)).thenReturn(Optional.of(step));
        }
        when(items.findAllForValidation(formName, 1)).thenReturn(List.of(
            field(2, "note", "text"), field(2, "details", "wysiwyg"),
            field(2, "captcha", "captcha"), field(3, "email", "email")
        ));
        request.addHeader("X-CSRF-Token", "autotest-token");
        request.setParameter("language", "en");
        request.setCookies(new Cookie("JSESSIONID", "autotest-session"));
        sessionKey = MultistepFormsService.getSessionKey(formName, request);
        request.getSession().setAttribute(sessionKey + MultistepFormApp.PERMITTED, true);
        request.getSession().setAttribute(sessionKey + MultistepFormApp.COUNTER, 1);
    }

    @AfterEach
    void tearDown() {
        tools.close();
        csrf.close();
        uploadServlet.close();
        caches.close();
        databases.close();
        props.close();
        cloud.close();
        constants.close();
    }

    /** Filters out unrelated fields and sanitizes rich text without changing confirmed answers or submitting the form. */
    @Test
    void filtersAndSanitizesDraftValues() throws Exception {
        request.getSession().setAttribute(sessionKey + "_note", "Confirmed note");
        setValues(new JSONObject()
            .put("f1-note", "  untrimmed | ~  ")
            .put("details", "<p>Draft text</p><script>alert(1)</script>")
            .put("email", "belongs to another step").put("captcha", "must not be retained"));
        service.saveStepDraft(formName, 2L, request);
        JSONObject draft = service.getDraftStepData(formName, 2L, request).first;
        assertEquals("  untrimmed | ~  ", draft.getString("note"));
        assertEquals("<p>Draft text</p>", draft.getString("details"));
        assertFalse(draft.has("email"));
        assertFalse(draft.has("captcha"));
        assertEquals("Confirmed note", request.getSession().getAttribute(sessionKey + "_note"));
        verifyNoInteractions(settings, save);
        databases.verifyNoInteractions();
    }

    /** Reading and clearing drafts is scoped to the form instance and domain. */
    @Test
    void isolatesFormInstances() throws Exception {
        setValues(new JSONObject().put("note", "draft"));
        service.saveStepDraft(formName, 2L, request);
        setValues(new JSONObject().put("email", "invalid"));
        service.saveStepDraft(formName, 3L, request);
        MockHttpServletRequest other = new MockHttpServletRequest();
        other.setSession(request.getSession());
        other.addHeader("X-CSRF-Token", "other-token");
        assertTrue(service.getDraftStepData(formName, 2L, other).first.isEmpty());
        assertTrue(service.getDraftStepData("other-form", 2L, request).first.isEmpty());
        service.clearStepDrafts(formName, other);
        assertFalse(service.getDraftStepData(formName, 2L, request).first.isEmpty());
        cloud.when(CloudToolsForCore::getDomainId).thenReturn(2);
        assertTrue(service.getDraftStepData(formName, 2L, request).first.isEmpty());
        cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
        service.clearStepDrafts(formName, request);
        assertTrue(service.getDraftStepData(formName, 2L, request).first.isEmpty());
        assertTrue(service.getDraftStepData(formName, 3L, request).first.isEmpty());
    }

    /** Rejects an invalid CSRF token or unsupported field value before writing a draft. */
    @ParameterizedTest
    @ValueSource(strings = {"csrf", "payload"})
    void rejectsInvalidDraftRequest(String failure) throws Exception {
        setValues(new JSONObject().put("note", "draft"));
        if ("csrf".equals(failure)) csrf.when(() -> CSRF.verifyTokenAjax(any(HttpSession.class), anyString())).thenReturn(false);
        if ("payload".equals(failure)) request.setContent("{\"note\":{\"invalid\":true}}".getBytes(StandardCharsets.UTF_8));
        Class<? extends Exception> expected = "csrf".equals(failure) ? SaveFormException.class : IllegalArgumentException.class;
        assertThrows(expected, () -> service.saveStepDraft(formName, 2L, request));
        assertTrue(service.getDraftStepData(formName, 2L, request).first.isEmpty());
        verifyNoInteractions(settings, save);
    }

    private void setValues(JSONObject values) {
        request.setContent(values.toString().getBytes(StandardCharsets.UTF_8));
    }

    private FormItemEntity field(long step, String name, String type) {
        FormItemEntity field = new FormItemEntity();
        field.setId((long) name.hashCode());
        field.setStepId(step);
        field.setItemFormId(name);
        field.setLabel(name);
        field.setFieldType(type);
        return field;
    }
}
