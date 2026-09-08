package sk.iway.iwcm.components.customfields.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;

/** Verifies exact JSON recovery from JDBC without decoding submitted or unrelated values. */
class JsonEditorValueReaderTest {

    /** Verifies generated definitions restore only JSON fields and retain their exact persisted text. */
    @Test
    void restoresExactTextAndLeavesOrdinaryFieldsFiltered() throws Exception {
        String raw = " \n{\"id\":9007199254740993,\"html\":\"</textarea>&quot;&amp;&#39;\",\"decimal\":1.00}\t ";
        TestEntity entity = new TestEntity();
        entity.setFieldA(DB.filterHtml(raw));
        entity.setFieldB(DB.filterHtml("<b>ordinary text</b>"));
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        CustomFieldsEntity configuration = new CustomFieldsEntity();
        configuration.setType("jsoneditor");
        configuration.setValue("jsoneditor");
        try (StoredRow row = new StoredRow(raw);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<CustomFieldsService> fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            props.when(Prop::getInstance).thenReturn(prop);
            props.when(() -> Prop.getInstance("sk")).thenReturn(prop);
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(any(CustomFieldsSearchDto.class))).thenReturn(java.util.Map.of('A', configuration));

            CustomFieldsSearchDto context = new CustomFieldsSearchDto(TestEntity.class.getName(), 7L);
            List<Field> definitions = new BaseEditorFields().getFields(entity, "test", 'B', context);
            assertEquals(raw, entity.getFieldA());
            assertEquals(raw, definitions.get(0).getValue());
            assertEquals("&lt;b&gt;ordinary text&lt;/b&gt;", entity.getFieldB());
            assertEquals("&lt;b&gt;ordinary text&lt;/b&gt;", definitions.get(1).getValue());
            verify(row.connection).prepareStatement("SELECT field_a FROM jsoneditor_fixture WHERE record_id=?");
            verify(row.statement).setObject(1, 7L);
        }
    }

    /** Verifies an invalid stored value remains invalid after its actual original text is restored. */
    @Test
    void preservesInvalidStoredText() throws Exception {
        String raw = " \n{\"broken\":<tag>} ";
        TestEntity entity = new TestEntity();
        entity.setFieldA(DB.filterHtml(raw));
        try (StoredRow row = new StoredRow(raw)) {
            JsonEditorValueReader.restore(entity, List.of("fieldA"));
            assertEquals(raw, entity.getFieldA());
            Prop prop = mock(Prop.class);
            when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            assertNotNull(JsonEditorValidator.validateValue(entity.getFieldA(), false, prop));
        }
    }

    /** Verifies encoded syntax that was already stored literally is never HTML-decoded. */
    @Test
    void doesNotGuessEncodedJsonSyntax() throws Exception {
        String literal = "{&quot;html&quot;:&quot;&lt;tag&gt;&quot;}";
        TestEntity entity = new TestEntity();
        entity.setFieldA(literal);
        try (StoredRow row = new StoredRow(literal)) {
            JsonEditorValueReader.restore(entity, List.of("fieldA"));
            assertEquals(literal, entity.getFieldA());
        }
    }

    /** Verifies an in-memory value differing from the persisted converter output is never overwritten. */
    @Test
    void leavesChangedValuesUntouched() throws Exception {
        TestEntity entity = new TestEntity();
        String changed = "{&quot;changed&quot;:&quot;&lt;tag&gt;&quot;}";
        entity.setFieldA(changed);
        try (StoredRow row = new StoredRow("{\"original\":\"<tag>\"}")) {
            JsonEditorValueReader.restore(entity, List.of("fieldA"));
            assertEquals(changed, entity.getFieldA());
        }
    }

    /** Verifies mapped history metadata also selects the right source during partial-update copying. */
    @Test
    void recognizesHistoryMetadataOnMappedPage() throws Exception {
        String raw = "{\"history\":\"<tag>\"}";
        DocDetails doc = new DocDetails();
        doc.setDocId(163800);
        doc.setHistoryId(1234);
        doc.setFieldA(DB.filterHtml(raw));
        try (StoredRow row = new StoredRow(raw)) {
            JsonEditorValueReader.restore(doc, List.of("fieldA"));
            assertEquals(raw, doc.getFieldA());
            verify(row.statement).setObject(1, 1234L);
        }
    }

    /** Verifies inherited entity metadata is used for a runtime proxy subclass. */
    @Test
    void supportsEntitySubclasses() throws Exception {
        String raw = "{\"html\":\"<tag>\"}";
        TestEntity entity = new TestEntity() {};
        entity.setFieldA(DB.filterHtml(raw));
        try (StoredRow row = new StoredRow(raw)) {
            JsonEditorValueReader.restore(entity, List.of("fieldA"));
            assertEquals(raw, entity.getFieldA());
        }
    }

    /** Verifies ordinary values and new entities avoid any additional database read. */
    @Test
    void skipsUnconfiguredOrNewEntities() {
        TestEntity entity = new TestEntity();
        entity.setFieldA(DB.filterHtml("{\"html\":\"<tag>\"}"));
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            JsonEditorValueReader.restore(entity, List.of());
            entity.setId(-1L);
            JsonEditorValueReader.restore(entity, List.of("fieldA"));
            pool.verifyNoInteractions();
        }
    }

    private static class StoredRow implements AutoCloseable {
        private final MockedStatic<DBPool> pool = mockStatic(DBPool.class);
        private final Connection connection = mock(Connection.class);
        private final PreparedStatement statement = mock(PreparedStatement.class);
        private final ResultSet result = mock(ResultSet.class);
        StoredRow(String raw) throws Exception {
            pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(result);
            when(result.next()).thenReturn(true);
            when(result.getString(1)).thenReturn(raw);
        }
        @Override
        public void close() { pool.close(); }
    }

    /** Fixture with an inherited primary key and ordinary text property. */
    public static class TestBase {
        @Id
        @Column(name = "record_id")
        private Long id = 7L;
        @Column(name = "field_a")
        private String fieldA;
        private String fieldB;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getFieldA() { return fieldA; }
        public void setFieldA(String value) { fieldA = value; }
        public String getFieldB() { return fieldB; }
        public void setFieldB(String value) { fieldB = value; }
    }

    /** Named persistent fixture for the raw-column lookup. */
    @Entity
    @Table(name = "jsoneditor_fixture")
    public static class TestEntity extends TestBase {}
}
