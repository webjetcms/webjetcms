package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;

class FormsServiceTest {

    @Test
    void replaceFieldsPreservesReplacementValuesAndUnknownExpressions() {
        Map<String, String> fields = Map.of("id", "field-${stepId}");

        assertEquals(
            "field-${stepId}|${unknown}|${unknown:-fallback}|$field-${stepId}",
            FormsService.replaceFields("${id}|${unknown}|${unknown:-fallback}|$${id}", fields)
        );
    }

    @Test
    void replaceFieldsResolvesTooltipPlaceholdersAndAriaRelation() {
        Prop prop = mock(Prop.class);
        when(prop.getText("components.formsimple.tooltipCode")).thenReturn(
            "<button type=\"button\" class=\"custom-tooltip-trigger ${classes}\" " +
                "aria-label=\"Help for ${labelSanitized}\" aria-describedby=\"${tooltipId}\" " +
                "data-description-id=\"${tooltipId}\" data-field-id=\"${id}\" data-item-id=\"${itemId}\" " +
                "data-label-sanitized=\"${labelSanitized}\" data-value=\"${value}\" " +
                "data-value-sanitized=\"${valueSanitized}\" data-placeholder=\"${placeholder}\"></button>" +
                "<span id=\"${tooltipId}\" role=\"tooltip\">${label} (${stepId})</span>"
        );

        JSONObject item = new JSONObject()
            .put("fieldType", "text")
            .put("id", 42)
            .put("stepId", 7)
            .put("itemFormId", "f2-customer.name=1")
            .put("label", "Customer <b>\"name\"</b>")
            .put("labelOriginal", "Customer <b>\"name\"</b>")
            .put("required", true)
            .put("value", "Some value")
            .put("placeholder", "Enter a value")
            .put("tooltip", "Enter ${id}, ${itemId} and ${stepId} literally");

        String result = FormsService.replaceFields(
            "<label for=\"${id}\">${label}</label>${tooltip}",
            "contact-form",
            "",
            item,
            "",
            false,
            false,
            new HashSet<>(),
            prop,
            null
        );

        Document document = Jsoup.parseBodyFragment(result);
        Element trigger = document.selectFirst(".custom-tooltip-trigger");
        assertNotNull(trigger);

        String tooltipId = "info-tooltip-f2-customer-name-1-42";
        assertEquals("button", trigger.tagName());
        assertEquals("button", trigger.attr("type"));
        assertEquals(tooltipId, trigger.attr("aria-describedby"));
        assertEquals(tooltipId, trigger.attr("data-description-id"));
        assertEquals("Help for Customer \"name\"", trigger.attr("aria-label"));
        assertEquals("f2-customer.name=1", trigger.attr("data-field-id"));
        assertEquals("42", trigger.attr("data-item-id"));
        assertEquals("Customer \"name\"", trigger.attr("data-label-sanitized"));
        assertEquals("Some value", trigger.attr("data-value"));
        assertEquals("some-value", trigger.attr("data-value-sanitized"));
        assertEquals("Enter a value", trigger.attr("data-placeholder"));
        assertEquals("custom-tooltip-trigger required", trigger.className());
        assertFalse("label".equals(trigger.parent().tagName()));

        Element tooltip = document.getElementById(tooltipId);
        assertNotNull(tooltip);
        assertEquals(1, document.select("#" + tooltipId).size());
        assertEquals("tooltip", tooltip.attr("role"));
        assertEquals("Enter ${id}, ${itemId} and ${stepId} literally (7)", tooltip.text());
        assertFalse(trigger.outerHtml().contains("${"));
        assertFalse(tooltip.attr("id").contains("${"));
    }

    /** Exposes only the documented settings and preserves raw HTML in the eight HTML fields. */
    @Test
    void exposesSettingsWithContextAppropriateEscaping() {
        FormSettingsEntity settings = new FormSettingsEntity();
        String html = "<strong title=\"A & B\">${id} ${settings.subject}</strong>";
        String storedHtml = ResponseUtils.filter(html);
        settings.setFieldA(storedHtml);
        settings.setFieldB(html);
        settings.setFieldC("C");
        settings.setFieldD("D");
        settings.setFieldE("E");
        settings.setFieldF("F");
        settings.setEmailTextBefore(storedHtml);
        settings.setEmailTextAfter(storedHtml);
        settings.setFormName("Contact Form");
        settings.setRecipients("a@example.org");
        settings.setCcEmails("cc@example.org");
        settings.setBccEmails("bcc@example.org");
        settings.setReplyTo("reply@example.org");
        settings.setSubject(ResponseUtils.filter("<subject> \" & '"));
        settings.setForward("/ok?a=1&b=2");
        settings.setForwardFail("/error");
        settings.setMaxSizeInKilobytes(1024);
        settings.setMaxCombinedSizeInKilobytes(4096L);
        settings.setAllowedExtensions("pdf,docx");
        settings.setPictureHeight(600);
        settings.setPictureWidth(800);
        settings.setFormAddClasses("form-wide");
        settings.setRowView(false);
        settings.setEncryptKey("must-not-be-exposed");
        settings.setAfterSendInterceptor("internal.Processor");

        Map<String, String> fields = FormsService.getSettingsReplacementMap(settings);
        assertEquals(Map.ofEntries(
            Map.entry("settings.fieldA", html), Map.entry("settings.fieldB", html),
            Map.entry("settings.fieldC", "C"), Map.entry("settings.fieldD", "D"),
            Map.entry("settings.fieldE", "E"), Map.entry("settings.fieldF", "F"),
            Map.entry("settings.emailTextBefore", html), Map.entry("settings.emailTextAfter", html),
            Map.entry("settings.formName", "Contact Form"), Map.entry("settings.recipients", "a@example.org"),
            Map.entry("settings.ccEmails", "cc@example.org"), Map.entry("settings.bccEmails", "bcc@example.org"),
            Map.entry("settings.replyTo", "reply@example.org"),
            Map.entry("settings.subject", "&lt;subject&gt; &quot; &amp; &#39;"),
            Map.entry("settings.forward", "/ok?a=1&amp;b=2"), Map.entry("settings.forwardFail", "/error"),
            Map.entry("settings.maxSizeInKilobytes", "1024"), Map.entry("settings.maxCombinedSizeInKilobytes", "4096"),
            Map.entry("settings.allowedExtensions", "pdf,docx"), Map.entry("settings.pictureHeight", "600"),
            Map.entry("settings.pictureWidth", "800"), Map.entry("settings.formAddClasses", "form-wide"),
            Map.entry("settings.rowView", "false")
        ), fields);
        settings.setRowView(true);
        assertEquals("true", FormsService.getSettingsReplacementMap(settings).get("settings.rowView"));
        assertEquals(storedHtml, settings.getFieldA(), "Rendering must not modify the entity");
    }

    /** Missing settings are empty, while unsupported expressions remain literal. */
    @Test
    void handlesMissingSettingsAndUnknownExpressions() {
        Map<String, String> fields = FormsService.getSettingsReplacementMap(null);
        assertEquals(23, fields.size());
        assertTrue(fields.values().stream().allMatch(String::isEmpty));
        assertEquals(fields, FormsService.getSettingsReplacementMap(new FormSettingsEntity()));
        assertEquals("||${settings.encryptKey}|${settings.fieldZ}", FormsService.replaceFields(
            "${settings.fieldA}|${settings.subject}|${settings.encryptKey}|${settings.fieldZ}", fields));
    }

    /** The loader never reuses settings from another domain or another form. */
    @Test
    void loadsSettingsForTheCurrentFormAndDomain() {
        FormSettingsRepository repository = mock(FormSettingsRepository.class);
        FormSettingsEntity first = new FormSettingsEntity();
        first.setFieldA("first domain");
        FormSettingsEntity second = new FormSettingsEntity();
        second.setFieldA("second domain");
        when(repository.findByFormNameAndDomainId("contact", 1)).thenReturn(first);
        when(repository.findByFormNameAndDomainId("contact", 2)).thenReturn(second);
        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
             MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class)) {
            tools.when(() -> Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class)).thenReturn(repository);
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            org.junit.jupiter.api.Assertions.assertSame(first, FormSettingsService.loadEntity("contact"));
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(2);
            org.junit.jupiter.api.Assertions.assertSame(second, FormSettingsService.loadEntity("contact"));
            org.junit.jupiter.api.Assertions.assertNull(FormSettingsService.loadEntity("other"));
            verify(repository).findByFormNameAndDomainId("contact", 1);
            verify(repository).findByFormNameAndDomainId("contact", 2);
            verify(repository).findByFormNameAndDomainId("other", 2);
        }
    }

    /** Nested templates expand settings once and never interpret expressions in inserted values. */
    @Test
    void rendersSettingsInFieldsHeadingsTooltipsAndIterableOptions() {
        FormSettingsEntity settings = new FormSettingsEntity();
        settings.setFieldA("<b>${id}|${settings.subject}|${cs-error}|${counter}</b>");
        settings.setSubject("A & B");
        Prop prop = mock(Prop.class);
        when(prop.getText("components.formsimple.tooltipCode")).thenReturn("<aside>${settings.fieldA} ${label}${cs-error}</aside>");
        when(prop.getText("components.formsimple.firstTimeHeading.select", false)).thenReturn("<h2>${settings.subject}</h2>");
        when(prop.getText("components.formsimple.iterable.select")).thenReturn(
            "<option value=\"${value}\" data-counter=\"${counter}\" data-title=\"${settings.subject}\">${value-label} ${settings.fieldA}</option>${cs-error}");
        JSONObject item = new JSONObject().put("fieldType", "select").put("label", "Choice")
            .put("labelOriginal", "Choice").put("itemFormId", "f1-choice").put("id", 1)
            .put("value", "One:${settings.subject}|Two:2").put("tooltip", "Literal ${settings.subject}");

        String result = FormsService.replaceFields(
            "<label>${label}</label>${tooltip}<select>${iterable}</select><footer>${settings.fieldA}</footer>${cs-error}",
            "form", settings, "", item, "", false, false, new HashSet<>(), prop, null);
        Document document = Jsoup.parseBodyFragment(result);
        assertEquals("A & B", document.selectFirst("h2").text());
        assertEquals("Literal ${settings.subject}", document.selectFirst("aside").ownText());
        assertEquals(4, result.split(java.util.regex.Pattern.quote(settings.getFieldA()), -1).length - 1);
        assertEquals(2, document.select("b").size());
        for (Element element : document.select("b")) {
            assertEquals("${id}|${settings.subject}|${cs-error}|${counter}", element.text());
        }
        assertEquals("${settings.subject}", document.selectFirst("option").attr("value"));
        assertEquals("A & B", document.selectFirst("option").attr("data-title"));
        assertEquals("0", document.selectFirst("option").attr("data-counter"));
        assertNotNull(document.selectFirst(".cs-error-f1-choice-1"));
        assertEquals(4, result.split(java.util.regex.Pattern.quote(
            "<div class=\"help-block cs-error cs-error-f1-choice cs-error-f1-choice-0 cs-error-f1-choice-1\"></div>"), -1).length - 1);

        String email = FormsService.replaceFields("${tooltip}${settings.emailTextBefore}${settings.fieldA}",
            "form", settings, "", item, "", true, false, new HashSet<>(), prop, null);
        assertFalse(email.contains("<aside>"));
        assertTrue(email.contains(settings.getFieldA()));
        assertEquals("<b>${id}|${settings.subject}|${cs-error}|${counter}</b>|form", FormsService.replaceFields(
            "${settings.fieldA}|${formname}", "form", settings, "", null, "", false, false, null, prop, null));
    }

    @Test
    void isFormAccessibleReturnsFalseForRestrictedUserWhenSubmittedFormDoesNotExist() {
        int domainId = 42;
        String formName = "new-form";

        FormsRepository formsRepository = mock(FormsRepository.class);
        FormsService<FormsRepository, FormsEntity> formsService = new FormsService<>(
            formsRepository,
            mock(FormSettingsRepository.class),
            mock(FormStepsRepository.class),
            mock(FormItemsRepository.class)
        );

        UserDetails user = mock(UserDetails.class);
        when(user.getEditableGroups()).thenReturn("");
        when(user.getEditablePages()).thenReturn("123");

        GroupsDB groupsDB = mock(GroupsDB.class);
        when(groupsDB.expandGroupIdsToChilds(any(int[].class), eq(true))).thenReturn(new int[0]);
        DocDB docDB = mock(DocDB.class);
        when(formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId))
            .thenReturn(null);

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(domainId);
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            docs.when(DocDB::getInstance).thenReturn(docDB);

            assertFalse(formsService.isFormAccessible(formName, user));
        }

        verify(formsRepository).findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId);
        verifyNoInteractions(docDB);
    }
}
