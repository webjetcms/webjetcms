package sk.iway.iwcm.system.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.validation.BeanPropertyBindingResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.i18n.Prop;

/** Verifies JSON custom fields at the shared DataTable editor validation boundary. */
class JsonEditorDatatableTest {

    private MockedStatic<Constants> constants;
    private MockedStatic<InitServlet> initServlet;
    private MockedStatic<Prop> props;
    private MockedStatic<CustomFieldsService> fields;
    private TestController controller;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        initServlet = mockStatic(InitServlet.class);
        props = mockStatic(Prop.class);
        fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS);

        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(anyString(), anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
        props.when(() -> Prop.getInstance("sk")).thenReturn(prop);
        props.when(Prop::getInstance).thenReturn(prop);

        CustomFieldsEntity configuration = new CustomFieldsEntity();
        configuration.setAlphabet("A");
        configuration.setType("jsoneditor");
        configuration.setValue("jsoneditor");
        configuration.setRequired(true);
        fields.when(() -> CustomFieldsService.getCustomFieldsMap(any(CustomFieldsSearchDto.class))).thenReturn(Map.of('A', configuration));
        fields.when(() -> CustomFieldsService.getRequiredFieldsAlphabets(any(CustomFieldsSearchDto.class))).thenReturn(List.of('A'));
        controller = new TestController(prop);
    }

    @AfterEach
    void tearDown() {
        fields.close();
        props.close();
        initServlet.close();
        constants.close();
    }

    /** Verifies invalid JSON is mapped to the custom field and deletion is not blocked. */
    @Test
    void mapsEditorErrorsAndAllowsDelete() {
        TestEntity entity = new TestEntity();
        String invalid = "{\"html\":<invalid>}";
        entity.setFieldA(invalid);
        DatatableRequest<Long, TestEntity> request = request("edit", entity);

        BeanPropertyBindingResult errors = validate(request, entity);
        assertTrue(errors.hasFieldErrors("errorField.fieldA"));
        assertEquals(invalid, entity.getFieldA(), "Invalid input must not be canonicalized");

        request.setAction("remove");
        entity.setFieldA("");
        assertTrue(validate(request, entity).getAllErrors().isEmpty());
    }

    /** Verifies valid JSON passes without changing its source text. */
    @Test
    void acceptsValidEditorValue() {
        TestEntity entity = new TestEntity();
        String value = "{ \"id\": 9007199254740993 }";
        entity.setFieldA(value);

        assertTrue(validate(request("edit", entity), entity).getAllErrors().isEmpty());
        assertEquals(value, entity.getFieldA());
    }

    /** Verifies valid JSON is stored in a form that remains unchanged by the JPA XSS filter. */
    @Test
    void canonicalizesValidEditorValueForPersistence() throws Exception {
        TestEntity entity = new TestEntity();
        String source = "{\"html\":\"<img src=x onerror=alert(1)>\",\"entity\":\"&lt;\"}";
        String canonical = "{\"html\":\"\\u003Cimg src=x onerror=alert(1)\\u003E\",\"entity\":\"&lt;\"}";
        entity.setFieldA(source);

        assertTrue(validate(request("edit", entity), entity).getAllErrors().isEmpty());
        assertEquals(canonical, entity.getFieldA());
        assertEquals(canonical, DB.filterHtml(canonical), "The JPA filter must leave canonical JSON unchanged");
        assertEquals(new ObjectMapper().readTree(source), new ObjectMapper().readTree(canonical));
    }

    /** Verifies an imported JSON column is checked by the same editor validation method. */
    @Test
    void validatesImportedJsonColumn() {
        TestEntity entity = new TestEntity();
        entity.setFieldA("{invalid import}");
        DatatableRequest<Long, TestEntity> request = request("edit", entity);
        request.setDztotalchunkcount(1);
        request.setImportedColumns(Set.of("fieldA"));

        assertTrue(validate(request, entity).hasFieldErrors("errorField.fieldA"));
    }

    /** Verifies imported JSON columns use the same persistence canonicalization. */
    @Test
    void canonicalizesImportedJsonColumn() {
        TestEntity entity = new TestEntity();
        entity.setFieldA("{\"html\":\"<imported>\"}");
        DatatableRequest<Long, TestEntity> request = request("edit", entity);
        request.setDztotalchunkcount(1);
        request.setImportedColumns(Set.of("fieldA"));

        assertFalse(validate(request, entity).hasErrors());
        assertEquals("{\"html\":\"\\u003Cimported\\u003E\"}", entity.getFieldA());
    }

    /** Verifies an omitted imported JSON column retains its stored value and is not treated as blank. */
    @Test
    void skipsOmittedJsonColumnDuringImport() {
        TestEntity entity = new TestEntity();
        DatatableRequest<Long, TestEntity> request = request("edit", entity);
        request.setDztotalchunkcount(1);
        request.setImportedColumns(Set.of("fieldB"));

        assertFalse(validate(request, entity).hasErrors());
    }

    private BeanPropertyBindingResult validate(DatatableRequest<Long, TestEntity> request, TestEntity entity) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "request");
        controller.validateEditorForCustomFields(null, request, null, errors, 7L, entity);
        return errors;
    }

    private static DatatableRequest<Long, TestEntity> request(String action, TestEntity entity) {
        DatatableRequest<Long, TestEntity> request = new DatatableRequest<>();
        request.setAction(action);
        request.setErrorField(entity);
        return request;
    }

    private static class TestController extends DatatableRestControllerV2<TestEntity, Long> {
        private final Prop prop;
        TestController(Prop prop) { this.prop = prop; }
        @Override
        public Prop getProp() { return prop; }
        @Override
        protected CustomFieldsSearchDto getCustomFieldsSearchDto(TestEntity entity) {
            return new CustomFieldsSearchDto(TestEntity.class.getName(), 7L);
        }
    }

    public static class TestEntity {
        private String fieldA;
        private String fieldB;
        public String getFieldA() { return fieldA; }
        public void setFieldA(String value) { fieldA = value; }
        public String getFieldB() { return fieldB; }
        public void setFieldB(String value) { fieldB = value; }
    }
}
