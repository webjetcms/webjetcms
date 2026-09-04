package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;

class FormsServiceTest {

    @Test
    void replaceFieldsResolvesTooltipPlaceholdersAndAriaRelation() {
        Prop prop = mock(Prop.class);
        when(prop.getText("components.formsimple.tooltipCode")).thenReturn(
            "<i class=\"custom-tooltip-trigger ${classes}\" aria-describedby=\"${tooltipId}\" " +
                "aria-owns=\"${tooltipId}\" data-field-id=\"${id}\" data-item-id=\"${itemId}\" " +
                "data-label-sanitized=\"${labelSanitized}\" data-value=\"${value}\" " +
                "data-value-sanitized=\"${valueSanitized}\" data-placeholder=\"${placeholder}\"></i>" +
                "<span id=\"${tooltipId}\" role=\"tooltip\">${label} (${stepId})</span>"
        );

        JSONObject item = new JSONObject()
            .put("fieldType", "text")
            .put("id", 42)
            .put("stepId", 7)
            .put("itemFormId", "f2-customer.name=1")
            .put("label", "Customer <b>name</b>")
            .put("labelOriginal", "Customer <b>name</b>")
            .put("required", true)
            .put("value", "Some value")
            .put("placeholder", "Enter a value")
            .put("tooltip", "Enter ${id}, ${itemId} and ${stepId} literally");

        String result = FormsService.replaceFields(
            "<label for=\"${id}\">${label}${tooltip}</label>",
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
        assertEquals(tooltipId, trigger.attr("aria-describedby"));
        assertEquals(tooltipId, trigger.attr("aria-owns"));
        assertEquals("f2-customer.name=1", trigger.attr("data-field-id"));
        assertEquals("42", trigger.attr("data-item-id"));
        assertEquals("Customer name", trigger.attr("data-label-sanitized"));
        assertEquals("Some value", trigger.attr("data-value"));
        assertEquals("some-value", trigger.attr("data-value-sanitized"));
        assertEquals("Enter a value", trigger.attr("data-placeholder"));
        assertEquals("custom-tooltip-trigger required", trigger.className());

        Element tooltip = document.getElementById(tooltipId);
        assertNotNull(tooltip);
        assertEquals(1, document.select("#" + tooltipId).size());
        assertEquals("tooltip", tooltip.attr("role"));
        assertEquals("Enter ${id}, ${itemId} and ${stepId} literally (7)", tooltip.text());
        assertFalse(trigger.outerHtml().contains("${"));
        assertFalse(tooltip.attr("id").contains("${"));
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
