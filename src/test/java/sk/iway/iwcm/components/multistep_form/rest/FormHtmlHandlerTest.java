package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.utils.Pair;

/**
 * Tests context-specific rendering in {@link FormHtmlHandler}.
 */
class FormHtmlHandlerTest {

    /** Keeps UTF-8 form CSS usable in PDF rendering without embedding a file BOM. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preparesCssForEmailAndPdf(boolean useTemplate) {
        String css = "\uFEFF/* Unicode: \u017D */\n.answer { color: blue; }";
        DocDB docDB = mock(DocDB.class);
        GroupsDB groupsDB = mock(GroupsDB.class);
        TemplatesDB templatesDB = mock(TemplatesDB.class);
        DocDetails doc = new DocDetails();
        doc.setTempId(1);
        doc.setGroupId(2);
        TemplateDetails template = new TemplateDetails();
        template.setBaseCssPath("/css/form.css");
        ServletContext context = mock(ServletContext.class);
        when(docDB.getDoc(10, -1, false)).thenReturn(doc);
        when(groupsDB.getGroup(2)).thenReturn(new GroupDetails());
        when(templatesDB.getTemplate(1)).thenReturn(useTemplate ? template : null);
        when(context.getResourceAsStream("/css/email.css"))
            .thenReturn(new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8)));

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<DocDB> docs = mockStatic(DocDB.class);
             MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
             MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
             MockedStatic<FileTools> files = mockStatic(FileTools.class)) {
            constants.when(Constants::getServletContext).thenReturn(context);
            docs.when(DocDB::getInstance).thenReturn(docDB);
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            templates.when(TemplatesDB::getInstance).thenReturn(templatesDB);
            files.when(() -> FileTools.readFileContent("/css/form.css"))
                .thenReturn(new String(css.getBytes(StandardCharsets.UTF_8), Charset.forName(Constants.FILE_ENCODING)));
            files.when(() -> FileTools.readFileContent("/css/form.css", SetCharacterEncodingFilter.getEncoding())).thenReturn(css);

            Pair<String, String> result = FormHtmlHandler.getCssDataLink(10, false);

            assertEquals("all", Jsoup.parse(result.first).selectFirst("style").attr("media"));
            assertTrue(result.first.contains("/* Unicode: \u017D */"));
            assertTrue(result.first.contains(".answer { color: blue; }"));
            assertFalse(result.first.contains("\uFEFF"));
            assertTrue(result.second.contains(useTemplate ? "/css/form.css" : "/css/email.css"));
        }
    }

    /**
     * Verifies that the localized loader stays inside the form element on a web page.
     */
    @Test
    void appendsLocalizedLoaderInsidePageForm() {
        Prop prop = mock(Prop.class);
        StringBuilder formEndHtml = new StringBuilder("<button type=\"submit\">Send</button></form><script></script>");
        String loaderHtml = "<div class=\"loader\">Loading...</div>";
        when(prop.getText("components.mustistep.form.loader")).thenReturn(loaderHtml);

        FormHtmlHandler.appendFormLoader(formEndHtml, false, prop);

        assertEquals(
            "<button type=\"submit\">Send</button>" + loaderHtml + "</form><script></script>",
            formEndHtml.toString()
        );
        verify(prop).getText("components.mustistep.form.loader");
    }

    /**
     * Verifies that email rendering neither resolves nor appends the loader property.
     */
    @Test
    void omitsLoaderFromEmailForm() {
        Prop prop = mock(Prop.class);
        String originalHtml = "<button type=\"submit\">Send</button></form>";
        StringBuilder formEndHtml = new StringBuilder(originalHtml);

        FormHtmlHandler.appendFormLoader(formEndHtml, true, prop);

        assertEquals(originalHtml, formEndHtml.toString());
        verifyNoInteractions(prop);
    }

    /** Exercises source-template replacement across a complete step and its email representation. */
    @Test
    void rendersSettingsInAllStepWrappersAndEmail() {
        FormStepsRepository steps = mock(FormStepsRepository.class);
        FormItemsRepository items = mock(FormItemsRepository.class);
        FormItemsConditionsRepository conditions = mock(FormItemsConditionsRepository.class);
        FormSettingsRepository settingsRepository = mock(FormSettingsRepository.class);
        FormSettingsEntity settings = new FormSettingsEntity();
        settings.setFormName("contact");
        settings.setSubject("A & B");
        settings.setFieldA("<b>${formActionSrc}|${submitButtonText}|${settings.subject}</b>");
        settings.setEmailTextBefore("&lt;em&gt;Before&lt;/em&gt;");
        settings.setEmailTextAfter("<em>After</em>");
        settings.setAddTechInfo(true);
        settings.setRowView(true);
        when(settingsRepository.findByFormNameAndDomainId("contact", 7)).thenReturn(settings);
        FormStepEntity step = new FormStepEntity();
        step.setId(2L);
        step.setCurrentPosition(2);
        step.setMaxPosition(2);
        step.setHeader("Heading");
        step.setBackStepBtnLabel("Back & more");
        step.setNextStepBtnLabel("Send");
        FormStepEntity previous = new FormStepEntity();
        previous.setId(1L);
        when(steps.getReferenceById(2L)).thenReturn(step);
        when(steps.findById(2L)).thenReturn(Optional.of(step));
        when(steps.getStepByPosition("contact", 1, 7)).thenReturn(Optional.of(previous));
        when(steps.findAllByFormNameAndDomainIdOrderBySortPriorityAsc("contact", 7)).thenReturn(List.of(step));
        when(items.getAllStepItems(2L, 7)).thenReturn(List.of());
        Map<String, String> templates = Map.ofEntries(
            Map.entry("components.mustistep.form.start", "<form action=\"${formActionSrc}\" data-title=\"${settings.subject}\">${settings.fieldA}"),
            Map.entry("components.mustistep.step.start", "<section data-title=\"${settings.subject}\">"),
            Map.entry("components.mustistep.step.header", "<h2>${step-header} ${settings.fieldA}</h2>"),
            Map.entry("components.mustistep.rowView.start", "<div class=\"row\" data-title=\"${settings.subject}\">"),
            Map.entry("components.mustistep.rowView.end", "</div><i>${settings.subject}</i>"),
            Map.entry("components.mustistep.step.end", "<footer>${settings.fieldA}</footer></section>"),
            Map.entry("components.mustistep.form.back_button", "<button type=\"button\" data-multistep-back-step=\"${previousStepId}\" title=\"${settings.subject}\">${backButtonText}${settings.fieldA}</button>"),
            Map.entry("components.mustistep.form.end", "<button type=\"submit\" title=\"${settings.subject}\">${submitButtonText}</button>{tech-info}</form>"),
            Map.entry("components.mustistep.form.loader", "<div class=\"loader\">${settings.fieldA}</div>"),
            Map.entry("components.formsimple.techinfo", "<div class=\"techinfo\">${settings.subject} ${settings.fieldA}</div>")
        );
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> templates.getOrDefault(invocation.getArgument(0), ""));
        Map<String, Object> beans = Map.of("formStepsRepository", steps, "formItemsRepository", items,
            "formItemsConditionsRepository", conditions, "formSettingsRepository", settingsRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("language", "en");
        try (MockedStatic<Tools> tools = mockStatic(Tools.class, invocation -> {
                if ("getSpringBean".equals(invocation.getMethod().getName())) return beans.get(invocation.getArgument(0));
                return invocation.callRealMethod();
            });
             MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
             MockedStatic<Prop> props = mockStatic(Prop.class);
             MockedStatic<ShowDoc> showDoc = mockStatic(ShowDoc.class);
             MockedStatic<FormHtmlHandler> handlers = mockStatic(FormHtmlHandler.class, CALLS_REAL_METHODS)) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(7);
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);
            handlers.when(() -> FormHtmlHandler.getCssDataLink(10, false, null)).thenReturn(new Pair<>("", ""));
            showDoc.when(() -> ShowDoc.updateCodes(org.mockito.ArgumentMatchers.isNull(), anyString(),
                org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.same(request), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

            FormHtmlHandler handler = new FormHtmlHandler("contact", 1, request);
            org.jsoup.nodes.Document page = Jsoup.parseBodyFragment(handler.getFormStepHtml(2L, request));
            assertEquals("A & B", page.selectFirst("form").attr("data-title"));
            assertEquals("/rest/multistep-form/save-form?form-name=contact&step-id=2", page.selectFirst("form").attr("action"));
            assertEquals("A & B", page.selectFirst("section").attr("data-title"));
            assertEquals("A & B", page.selectFirst(".row").attr("data-title"));
            assertEquals("A & B", page.selectFirst("i").text());
            assertEquals("1", page.selectFirst("[data-multistep-back-step]").attr("data-multistep-back-step"));
            assertEquals("Back & more", page.selectFirst("[data-multistep-back-step]").ownText());
            assertEquals("Send", page.selectFirst("[type=submit]").text());
            assertEquals(5, page.select("b").size());
            page.select("b").forEach(element -> assertEquals("${formActionSrc}|${submitButtonText}|${settings.subject}", element.text()));
            assertFalse(page.select("form .loader").isEmpty());

            FormsEntity form = new FormsEntity();
            form.setFormName("contact");
            handler.setFormHtml(form, Map.of(), request, 10);
            org.jsoup.nodes.Document email = Jsoup.parseBodyFragment(handler.getFormHtmlBeforeCss());
            assertEquals("Before", email.select("em").first().text());
            assertEquals("After", email.select("em").last().text());
            assertEquals("A & B", email.selectFirst(".techinfo").ownText());
            assertTrue(email.select(".loader, [data-multistep-back-step]").isEmpty());
            email.select("b").forEach(element -> assertEquals("${formActionSrc}|${submitButtonText}|${settings.subject}", element.text()));
            verify(settingsRepository).findByFormNameAndDomainId("contact", 7);
        }
    }

    /** Resolves selected and unselected options from exact or legacy values containing commas. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void resolvesSelectedOptionsWithCommaContainingValues(boolean exactSelections) {
        FormHtmlHandler handler = mock(FormHtmlHandler.class, CALLS_REAL_METHODS);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(handler, "formName", "contact-form");
        ReflectionTestUtils.setField(handler, "formData", Map.of("choices", "Research, development"));
        try (MockedStatic<MultistepFormsService> forms = mockStatic(MultistepFormsService.class)) {
            forms.when(() -> MultistepFormsService.getSavedSelectedValues("contact-form", "choices", request))
                .thenReturn(exactSelections ? new String[] {"Research, development"} : null);

            assertTrue((Boolean) ReflectionTestUtils.invokeMethod(handler, "isCheckboxOrRadioSelected", "Research, development", "choices", request));
            assertFalse((Boolean) ReflectionTestUtils.invokeMethod(handler, "isCheckboxOrRadioSelected", "Other", "choices", request));
        }
    }
}
