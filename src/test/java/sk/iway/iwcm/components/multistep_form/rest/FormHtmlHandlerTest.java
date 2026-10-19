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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
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
