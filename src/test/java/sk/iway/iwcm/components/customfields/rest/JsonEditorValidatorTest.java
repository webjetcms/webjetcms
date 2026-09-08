package sk.iway.iwcm.components.customfields.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import jakarta.persistence.Id;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Verifies supported object syntax, authoritative field rules and native validation errors.
 */
class JsonEditorValidatorTest {

    private Prop prop;

    @BeforeEach
    void prepareMessages() {
        prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(anyString(), anyString(), anyString())).thenAnswer(invocation ->
            invocation.getArgument(0) + " " + invocation.getArgument(1) + ":" + invocation.getArgument(2));
        when(prop.getText(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Verifies complete JSON objects, including values unsafe to round-trip through JavaScript numbers. */
    @ParameterizedTest
    @ValueSource(strings = {
        "{}", " \r\n {\"nested\":{\"values\":[1,true,false,null,\"text\"]}} \t",
        "{\"id\":9007199254740993,\"small\":1.234567890123456789,\"large\":1e999}",
        "{\"same\":1,\"same\":2}", "{\"text\":\"<script> & \\u20ac \\n \\\" \\\\\"}"
    })
    void acceptsCompleteObjects(String value) {
        assertNull(JsonEditorValidator.validateValue(value, true, prop), "A complete JSON object must be valid");
    }

    /** Verifies malformed syntax and trailing input cannot be accepted as a partial document. */
    @ParameterizedTest
    @ValueSource(strings = {
        "{", "{\"x\":}", "{\"x\":1,}",
        "{\"x\":NaN}", "{\"x\":Infinity}", "{\"x\":01}", "{\"x\":\"\\q\"}",
        "{\"x\":\"a\nb\"}", "{} {}", "{} null", "{} trailing"
    })
    void rejectsMalformedOrTrailingInput(String value) {
        assertNotNull(JsonEditorValidator.validateValue(value, false, prop), "Invalid syntax must block saving");
    }

    /** Verifies the exact same extended syntax fixtures as the browser validator. */
    @Test
    void supportsSharedExtendedSyntax() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/sk/iway/iwcm/components/customfields/jsoneditor-syntax.json")) {
            JsonNode fixtures = new ObjectMapper().readTree(stream);
            for (JsonNode value : fixtures.get("valid")) {
                assertNull(JsonEditorValidator.validateValue(value.asText(), true, prop), value.asText());
            }
            for (JsonNode value : fixtures.get("invalid")) {
                assertNotNull(JsonEditorValidator.validateValue(value.asText(), true, prop), value.asText());
            }
        }
    }

    /** Verifies other legal JSON root values are not accepted as MHUB objects. */
    @ParameterizedTest
    @ValueSource(strings = {"[]", "[{}]", "null", "true", "false", "42", "\"text\""})
    void rejectsNonObjectRoots(String value) {
        assertEquals("settings.custom-fields.jsoneditor.object.js", JsonEditorValidator.validateValue(value, false, prop));
    }

    /** Verifies empty input follows the custom field's required setting. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n"})
    void appliesRequiredRuleToBlankInput(String value) {
        assertNull(JsonEditorValidator.validateValue(value, false, prop));
        assertEquals("settings.custom-fields.required-err", JsonEditorValidator.validateValue(value, true, prop));
    }

    /** Verifies parser diagnostics identify the source line without echoing the JSON payload. */
    @Test
    void reportsSyntaxLocation() {
        String error = JsonEditorValidator.validateValue("{\n  \"secret\": ]\n}", false, prop);
        assertTrue(error.contains("position.js 2:"), "The error must identify the second source line");
        assertFalse(error.contains("secret"), "Validation messages must not echo field content");
    }

    /** Verifies validation preserves several kilobytes of whitespace, strings and exact numeric tokens. */
    @Test
    void leavesOriginalTextUntouched() {
        FieldBean bean = new FieldBean();
        String text = "{\n  \"id\": 9007199254740993,\n  \"description\": \"" + "content ".repeat(700) + "\"\n}";
        bean.setFieldA(text);
        assertTrue(JsonEditorValidator.validate(bean, Map.of("fieldA", true), prop).isEmpty());
        assertEquals(text, bean.getFieldA(), "Validation must not trim, format or reserialize the original text");
    }

    /** Verifies inherited annotation labels and database overrides agree with generated field definitions. */
    @Test
    void usesTrustedConfigurationAndIgnoresPostedMetadata() {
        FieldBean bean = new FieldBean();
        BaseEditorFields posted = new BaseEditorFields();
        posted.setFieldsDefinitionKeyPrefix("attacker");
        posted.setFieldsDefinition(List.of());
        bean.setEditorFields(posted);
        when(prop.getText("test.field_a.type")).thenReturn("jsoneditor");
        when(prop.getText("test.field_b.type")).thenReturn("json_doc");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<CustomFieldsService> fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            props.when(() -> Prop.getInstance("sk")).thenReturn(prop);
            props.when(Prop::getInstance).thenReturn(prop);
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(any(CustomFieldsSearchDto.class))).thenReturn(Map.of());

            assertEquals(Map.of("fieldA", false), JsonEditorValidator.getRules(bean, new CustomFieldsSearchDto(bean)));
            assertEquals("jsoneditor", new BaseEditorFields().getFields(bean, "test", 'A').get(0).getType());

            CustomFieldsEntity override = new CustomFieldsEntity();
            override.setType("text");
            override.setValue("text");
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(any(CustomFieldsSearchDto.class))).thenReturn(Map.of('A', override));
            assertTrue(JsonEditorValidator.getRules(bean, new CustomFieldsSearchDto(bean)).isEmpty(), "Database text overrides must disable legacy JSON validation");

            override.setType("jsoneditor");
            override.setValue("jsoneditor");
            override.setRequired(true);
            assertEquals(Map.of("fieldA", true), JsonEditorValidator.getRules(bean, new CustomFieldsSearchDto(bean)));
        }
    }

    /** Verifies hidden enumeration strings are excluded while named database JSON fields use fieldA errors. */
    @Test
    void respectsEnumerationVisibilityAndFieldNames() {
        EnumerationDataBean entity = new EnumerationDataBean();
        CustomFieldsSearchDto context = new CustomFieldsSearchDto(EnumerationDataBean.class.getName(), 5L);
        CustomFieldsEntity configuration = new CustomFieldsEntity();
        configuration.setType("jsoneditor");
        configuration.setValue("jsoneditor");
        configuration.setLabel("Ignored configuration label");
        EnumerationTypeBean parent = new EnumerationTypeBean();
        parent.setString1Name("");
        EnumerationTypeRepository repository = mock(EnumerationTypeRepository.class);
        when(repository.getNonHiddenByEnumId(5, false)).thenReturn(parent);
        when(prop.getText("enumeration.field_a.type")).thenReturn("jsoneditor");
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
                MockedStatic<CustomFieldsService> fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("enumerationTypeRepository", EnumerationTypeRepository.class)).thenReturn(repository);
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            props.when(() -> Prop.getInstance("sk")).thenReturn(prop);
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(context)).thenReturn(Map.of());
            assertTrue(JsonEditorValidator.getRules(entity, context).isEmpty());
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(context)).thenReturn(Map.of('A', configuration));
            assertTrue(JsonEditorValidator.getRules(entity, context).isEmpty());
            configuration.setLabel("");
            parent.setString1Name("JSON data");
            entity.setFieldA("invalid");
            Map<String, Boolean> rules = JsonEditorValidator.getRules(entity, context);
            assertEquals(Map.of("fieldA", false), rules);
            assertEquals("fieldA", JsonEditorValidator.validate(entity, rules, prop).get(0).getName());
        }
    }

    /** Verifies document template contexts are restored on both success and failure. */
    @Test
    void restoresTemplatePrefixesBetweenDocuments() {
        RequestBean requestBean = new RequestBean();
        TemplatesDB templates = mock(TemplatesDB.class);
        when(templates.getTemplate(7)).thenReturn(mock(TemplateDetails.class));
        when(templates.getTemplate(8)).thenReturn(mock(TemplateDetails.class));
        try (MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class);
                MockedStatic<TemplatesDB> templateDb = mockStatic(TemplatesDB.class)) {
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);
            templateDb.when(TemplatesDB::getInstance).thenReturn(templates);
            RequestBean.addTextKeyPrefix("shared", true);
            assertEquals(List.of("temp-7", "shared"), DocEditorFields.withCustomFieldTextPrefixes(7, () -> List.copyOf(RequestBean.getTextKeyPrefixes())));
            assertEquals(List.of("shared"), RequestBean.getTextKeyPrefixes());
            assertThrows(IllegalArgumentException.class, () -> DocEditorFields.withCustomFieldTextPrefixes(8, () -> {
                assertFalse(RequestBean.getTextKeyPrefixes().contains("temp-7"));
                throw new IllegalArgumentException("Expected test failure");
            }));
            assertEquals(List.of("shared"), RequestBean.getTextKeyPrefixes());
        }
    }

    /** Verifies the JSON editor remains registered as a supported custom-field type. */
    @Test
    void registersJsonEditorFieldType() {
        assertEquals(FieldType.JSONEDITOR, FieldType.asFieldType("jsoneditor"));
        assertEquals(FieldType.JSON_DOC, FieldType.asFieldType("json_doc"));
        assertTrue(CustomFieldsService.isSupportedFieldType("jsoneditor"));
    }

    /** Metadata inherited by the test entity mirrors DocBasic and other custom-field base classes. */
    public static class FieldBase {
        @DataTableColumn(title = "[[#{test.field_a}]]")
        private String fieldA;
        @DataTableColumn(title = "test.field_b")
        private String fieldB;
        public String getFieldA() { return fieldA; }
        public void setFieldA(String value) { fieldA = value; }
        public String getFieldB() { return fieldB; }
        public void setFieldB(String value) { fieldB = value; }
    }

    /** Minimal entity used to test configuration and controller boundaries without persistence. */
    public static class FieldBean extends FieldBase {
        @Id
        private Long id;
        private BaseEditorFields editorFields;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public BaseEditorFields getEditorFields() { return editorFields; }
        public void setEditorFields(BaseEditorFields value) { editorFields = value; }
    }
}
