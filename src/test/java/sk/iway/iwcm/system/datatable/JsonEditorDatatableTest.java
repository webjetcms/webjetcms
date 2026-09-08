package sk.iway.iwcm.system.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.Validator;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsValidationException;
import sk.iway.iwcm.components.customfields.rest.JsonEditorValidator;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/**
 * Verifies JSON validation at editor and direct REST boundaries before custom save hooks run.
 */
class JsonEditorDatatableTest {

    private MockedStatic<Constants> constants;
    private MockedStatic<InitServlet> initServlet;
    private MockedStatic<Prop> props;
    private MockedStatic<CustomFieldsService> fields;
    private MockedStatic<WebjetEventPublisher> events;
    private TestController controller;
    private Prop prop;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        initServlet = mockStatic(InitServlet.class);
        props = mockStatic(Prop.class);
        fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS);
        events = mockStatic(WebjetEventPublisher.class);
        prop = mock(Prop.class);
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
        Validator validator = mock(Validator.class);
        when(validator.validate(any(TestEntity.class))).thenReturn(Set.of());
        ReflectionTestUtils.setField(controller, "validator", validator);
        ReflectionTestUtils.invokeMethod(controller, "clearThreadData");
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.invokeMethod(controller, "clearThreadData");
        events.close();
        fields.close();
        props.close();
        initServlet.close();
        constants.close();
    }

    /** Verifies direct REST creation cannot bypass JSON validation by omitting editor metadata. */
    @Test
    void blocksDirectCreateBeforePersistence() {
        TestEntity entity = new TestEntity();
        entity.setFieldA("{broken}");
        CustomFieldsValidationException failure = assertThrows(CustomFieldsValidationException.class, () -> controller.add(entity));
        assertEquals("fieldA", failure.getFieldErrors().get(0).getName());
        assertEquals(0, controller.saves, "No custom persistence hook may run after failed JSON validation");
    }

    /** Verifies direct REST updates reject tampered metadata and leave original values intact. */
    @Test
    void blocksDirectUpdateWithoutMutatingOriginal() {
        TestEntity entity = new TestEntity();
        entity.setId(7L);
        entity.setFieldA("{} {}");
        BaseEditorFields posted = new BaseEditorFields();
        posted.setFieldsDefinition(List.of());
        posted.setFieldsDefinitionKeyPrefix("untrusted");
        entity.setEditorFields(posted);
        assertThrows(CustomFieldsValidationException.class, () -> controller.edit(7, entity));
        assertEquals("{\"stored\":true}", controller.original.getFieldA());
        assertEquals(0, controller.saves);
    }

    /** Verifies partial edits validate retained values instead of treating omitted required fields as empty. */
    @Test
    void validatesMergedPartialUpdates() {
        TestEntity entity = new TestEntity();
        entity.setId(7L);
        entity.setFieldB("Changed text");
        assertTrue(controller.errors(entity, 7).isEmpty());
        assertEquals("{\"stored\":true}", controller.original.getFieldA());
        controller.original.setFieldA("invalid stored JSON");
        assertEquals("fieldA", controller.errors(entity, 7).get(0).getName());
        assertEquals("invalid stored JSON", controller.original.getFieldA());
    }

    /** Verifies retained JPA text is restored on the candidate only and posted encoded syntax still fails. */
    @Test
    void restoresOnlyRetainedJsonInPartialUpdates() throws Exception {
        String raw = " \n{\"html\":\"</textarea>&quot;\",\"decimal\":1.00}\t ";
        String filtered = DB.filterHtml(raw);
        controller.original.setFieldA(filtered);
        TestEntity partial = new TestEntity();
        partial.setId(7L);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet row = mock(ResultSet.class);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(row);
            when(row.next()).thenReturn(true);
            when(row.getString(1)).thenReturn(raw);
            assertTrue(controller.errors(partial, 7).isEmpty());
            assertEquals(filtered, controller.original.getFieldA(), "Validation must leave the persisted entity unchanged");
            partial.setFieldA(filtered);
            assertEquals("fieldA", controller.errors(partial, 7).get(0).getName());
            assertEquals(filtered, partial.getFieldA(), "Submitted encoded syntax must not be decoded or replaced");
            assertEquals(filtered, controller.original.getFieldA());
        }
    }

    /** Verifies switching to a JSON template validates retained raw text using the new field configuration. */
    @Test
    void restoresRetainedJsonAfterChangingTemplate() throws Exception {
        String raw = "{\"html\":\"<tag>&quot;\"}";
        DocDetails original = new DocDetails();
        original.setId(7L);
        original.setTempId(2);
        original.setFieldA(DB.filterHtml(raw));
        DocDetails partial = new DocDetails();
        partial.setId(7L);
        partial.setTempId(9);
        DocController documents = new DocController(prop, original);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet row = mock(ResultSet.class);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
                MockedStatic<JsonEditorValidator> json = mockStatic(JsonEditorValidator.class, CALLS_REAL_METHODS)) {
            pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(row);
            when(row.next()).thenReturn(true);
            when(row.getString(1)).thenReturn(raw);
            json.when(() -> JsonEditorValidator.getRules(any(), any(CustomFieldsSearchDto.class), any())).thenAnswer(invocation ->
                ((DocDetails)invocation.getArgument(0)).getTempId() == 9 ? Map.of("fieldA", false) : Map.of());
            assertTrue(documents.getJsonEditorFieldErrors(partial, 7).isEmpty());
            assertEquals(DB.filterHtml(raw), original.getFieldA());
            partial.setFieldA(DB.filterHtml(raw));
            assertEquals("fieldA", documents.getJsonEditorFieldErrors(partial, 7).get(0).getName());
        }
    }

    /** Verifies the base persistence merge saves retained raw JSON under the newly selected configuration. */
    @Test
    void persistsRetainedRawJsonAfterConfigurationChange() throws Exception {
        String raw = " \n{\"html\":\"<tag>&quot;\"}\t ";
        TestEntity original = new TestEntity();
        original.setId(7L);
        original.setFieldA(DB.filterHtml(raw));
        original.setFieldB("old configuration");
        TestEntity partial = new TestEntity();
        partial.setId(7L);
        partial.setFieldB("JSON configuration");
        @SuppressWarnings("unchecked")
        JpaRepository<TestEntity, Long> repository = mock(JpaRepository.class);
        when(repository.save(any(TestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DatatableRestControllerV2<TestEntity, Long> persistence = new DatatableRestControllerV2<>(repository) {
            @Override
            public Prop getProp() { return prop; }
            @Override
            public TestEntity getOne(long id) { return original; }
        };
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet row = mock(ResultSet.class);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
                MockedStatic<JsonEditorValidator> json = mockStatic(JsonEditorValidator.class, CALLS_REAL_METHODS)) {
            pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(row);
            when(row.next()).thenReturn(true);
            when(row.getString(1)).thenReturn(raw);
            json.when(() -> JsonEditorValidator.getRules(any(), any(CustomFieldsSearchDto.class), any())).thenAnswer(invocation ->
                "JSON configuration".equals(((TestEntity)invocation.getArgument(0)).getFieldB()) ? Map.of("fieldA", false) : Map.of());
            assertEquals(raw, persistence.editItem(partial, 7L).getFieldA());
            org.mockito.Mockito.verify(repository).save(original);
        }
    }

    /** Verifies a retained template's optional JSON override supersedes a global required text field. */
    @Test
    void appliesEffectiveTemplateRulesBeforeLegacyRequiredValidation() {
        DocDetails original = mock(DocDetails.class);
        when(original.getId()).thenReturn(7L);
        when(original.getDocId()).thenReturn(7);
        when(original.getTempId()).thenReturn(9);
        when(original.getFieldA()).thenReturn("{}");
        DocDetails partial = mock(DocDetails.class);
        when(partial.getId()).thenReturn(7L);
        DocController documents = new DocController(prop, original);
        DatatableRequest<Long, DocDetails> request = new DatatableRequest<>();
        request.setAction("edit");
        request.setErrorField(partial);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "request");
        try (MockedStatic<JsonEditorValidator> json = mockStatic(JsonEditorValidator.class, CALLS_REAL_METHODS)) {
            json.when(() -> JsonEditorValidator.getRules(any(), any(CustomFieldsSearchDto.class), any())).thenAnswer(invocation ->
                ((DocDetails)invocation.getArgument(0)).getTempId() == 9 ? Map.of("fieldA", false) : Map.of());
            documents.validateEditorForCustomFields(null, request, null, errors, 7L, partial);
            assertTrue(errors.getAllErrors().isEmpty(), "The effective optional JSON rule must replace the global required text rule");
        }
    }

    /** Verifies the editor reports field errors and does not block deleting legacy invalid values. */
    @Test
    void mapsEditorErrorsAndAllowsDelete() {
        TestEntity entity = new TestEntity();
        entity.setId(7L);
        entity.setFieldA("invalid");
        DatatableRequest<Long, TestEntity> request = new DatatableRequest<>();
        request.setAction("edit");
        request.setErrorField(entity);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "request");
        controller.validateEditorForCustomFields(null, request, null, errors, 7L, entity);
        assertTrue(errors.hasFieldErrors("errorField.fieldA"));

        request.setAction("remove");
        entity.setFieldA("");
        errors = new BeanPropertyBindingResult(request, "request");
        controller.validateEditorForCustomFields(null, request, null, errors, 7L, entity);
        assertTrue(errors.getAllErrors().isEmpty(), "Removing a row must not validate JSON contents");
    }

    /** Verifies import rows defer JSON checks until their target IDs and retained values are known. */
    @Test
    void defersImportChecksToPersistenceBoundary() {
        TestEntity entity = new TestEntity();
        entity.setId(7L);
        DatatableRequest<Long, TestEntity> request = new DatatableRequest<>();
        request.setAction("edit");
        request.setDztotalchunkcount(1);
        request.setErrorField(entity);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "request");
        controller.validateEditorForCustomFields(null, request, null, errors, 7L, entity);
        assertTrue(errors.getAllErrors().isEmpty(), "An omitted imported JSON column must retain its stored value");
        entity.setFieldA("invalid imported JSON");
        assertThrows(CustomFieldsValidationException.class, () -> controller.edit(7, entity));
        assertEquals(0, controller.saves);
    }

    /** Verifies an alternate-column import rejects invalid text after resolving an existing row ID. */
    @Test
    void blocksAlternateColumnImportUpdate() {
        TestEntity entity = new TestEntity();
        entity.setFieldA("{invalid import}");
        entity.setFieldB("matching value");
        assertThrows(CustomFieldsValidationException.class,
            () -> ReflectionTestUtils.invokeMethod(controller, "editItemByColumn", entity, "fieldB"));
        assertEquals(7L, entity.getId(), "Validation must use the ID found by the alternate column");
        assertEquals(0, controller.saves);
        assertEquals("{\"stored\":true}", controller.original.getFieldA());
    }

    /** Verifies the insert fallback of an alternate-column import also validates before persistence. */
    @Test
    void blocksAlternateColumnImportInsert() {
        controller.findExisting = false;
        TestEntity entity = new TestEntity();
        entity.setFieldA("{invalid new import}");
        entity.setFieldB("new value");
        assertThrows(CustomFieldsValidationException.class,
            () -> ReflectionTestUtils.invokeMethod(controller, "editItemByColumn", entity, "fieldB"));
        assertEquals(0, controller.saves);
    }

    /** Verifies valid text reaches the save hook without a formatting round-trip. */
    @Test
    void preservesValidSubmittedText() {
        TestEntity entity = new TestEntity();
        String text = "{ \"large\": 9007199254740993, \"items\": [1, 2] }";
        entity.setFieldA(text);
        assertEquals(text, controller.add(entity).getBody().getFieldA());
        assertEquals(1, controller.saves);
    }

    /** Controller with explicit save counters instead of database side effects. */
    private static class TestController extends DatatableRestControllerV2<TestEntity, Long> {
        private final Prop prop;
        private final TestEntity original = new TestEntity();
        private int saves;
        private boolean findExisting = true;
        TestController(Prop prop) {
            this.prop = prop;
            original.setId(7L);
            original.setFieldA("{\"stored\":true}");
        }
        @Override
        public Prop getProp() { return prop; }
        @Override
        public TestEntity getOneItem(long id) { return original; }
        @Override
        public TestEntity insertItem(TestEntity entity) { saves++; return entity; }
        @Override
        public TestEntity editItem(TestEntity entity, long id) { saves++; return entity; }
        @Override
        public List<TestEntity> findItemBy(String propertyName, TestEntity entity) {
            return findExisting ? List.of(original) : List.of();
        }
        List<DatatableFieldError> errors(TestEntity entity, long id) { return getJsonEditorFieldErrors(entity, id); }
    }

    /** Isolates template configuration fallback from unrelated document editor services. */
    private static class DocController extends DatatableRestControllerV2<DocDetails, Long> {
        private final Prop prop;
        private final DocDetails original;
        DocController(Prop prop, DocDetails original) { this.prop = prop; this.original = original; }
        @Override
        public Prop getProp() { return prop; }
        @Override
        public DocDetails getOneItem(long id) { return original; }
        @Override
        protected void copyEntityIntoOriginal(DocDetails entity, DocDetails destination) {
            if (entity.getFieldA() != null) destination.setFieldA(entity.getFieldA());
            if (entity.getTempId() > 0) destination.setTempId(entity.getTempId());
        }
    }

    /** Entity fixture with independently configurable JSON and ordinary text fields. */
    @Entity
    @Table(name = "jsoneditor_fixture")
    public static class TestEntity {
        @Id
        @Column(name = "record_id")
        private Long id;
        @DataTableColumn(title = "test.field_a")
        @Column(name = "field_a")
        private String fieldA;
        @DataTableColumn(title = "test.field_b")
        private String fieldB;
        private BaseEditorFields editorFields;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getFieldA() { return fieldA; }
        public void setFieldA(String value) { fieldA = value; }
        public String getFieldB() { return fieldB; }
        public void setFieldB(String value) { fieldB = value; }
        public BaseEditorFields getEditorFields() { return editorFields; }
        public void setEditorFields(BaseEditorFields value) { editorFields = value; }
    }
}
