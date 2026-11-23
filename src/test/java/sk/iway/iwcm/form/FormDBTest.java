package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.forms.archive.FormsArchiveEntity;

/**
 * Tests that active and archived form entities share the columns copied by {@link FormDB}.
 */
class FormDBTest {

    /**
     * Verifies that persistent entity fields and both sides of the archive insert stay aligned.
     */
    @Test
    void keepsArchiveEntityAndInsertColumnsAligned() {
        Set<String> activeColumns = getPersistentColumns(FormsEntity.class);
        Set<String> archiveColumns = getPersistentColumns(FormsArchiveEntity.class);
        Set<String> insertColumns = getArchiveColumnsWithId();

        assertEquals(activeColumns, archiveColumns);
        assertEquals(activeColumns, insertColumns);

        String sql = FormDB.getArchiveInsertSql("f.id = ?", true);
        assertInsertColumnsMatch(sql, insertColumns);
        assertTrue(sql.endsWith("WHERE f.id = ?"));
    }

    /**
     * Verifies that SQL Server archive inserts omit the identity column while preserving all other fields.
     */
    @Test
    void omitsIdentityColumnFromMssqlArchiveInsert() {
        Set<String> expectedColumns = getArchiveColumnsWithId();
        expectedColumns.remove("id");

        String sql = FormDB.getArchiveInsertSql("f.id = ?", false);
        assertInsertColumnsMatch(sql, expectedColumns);
        String targetColumns = sql.substring(sql.indexOf('(') + 1, sql.indexOf(')'));
        assertFalse(Arrays.asList(targetColumns.split(", ")).contains("id"));
    }

    /**
     * Verifies that dirty data with duplicate active headers is resolved deterministically.
     */
    @Test
    void selectsLowestActiveHeaderForArchiveInsertAndUpdate() {
        String insertSql = FormDB.getArchiveHeaderInsertSql();
        String updateSql = FormDB.getArchiveHeaderUpdateSql();

        assertTrue(insertSql.contains("f.id = (SELECT MIN(f2.id) FROM forms f2"));
        assertEquals(2, countOccurrences(updateSql, "SELECT MIN(f2.id) FROM forms f2"));
        assertEquals(2, countOccurrences(updateSql, "f2.form_name = ? AND f2.domain_id = ?"));
        assertTrue(updateSql.endsWith("WHERE form_name = ? AND domain_id = ? AND create_date IS NULL"));
        assertFalse(updateSql.contains("UPDATE forms_archive f1, forms f2"));
    }

    private static Set<String> getPersistentColumns(Class<?> entityClass) {
        Set<String> columns = new LinkedHashSet<>();
        Class<?> currentClass = entityClass;

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isAnnotationPresent(Transient.class)) {
                    continue;
                }

                Column column = field.getAnnotation(Column.class);
                columns.add(column != null && column.name().isEmpty() == false ? column.name() : field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }

        return columns;
    }

    private static Set<String> getArchiveColumnsWithId() {
        return new LinkedHashSet<>(Arrays.asList(("id, " + FormDB.FORM_ARCHIVE_COLUMNS_WITHOUT_ID).split(", ")));
    }

    private static void assertInsertColumnsMatch(String sql, Set<String> expectedColumns) {
        assertFalse(sql.contains("*"));

        int targetStart = sql.indexOf('(') + 1;
        int targetEnd = sql.indexOf(')');
        String targetColumns = sql.substring(targetStart, targetEnd);

        int sourceStart = sql.indexOf("SELECT ") + "SELECT ".length();
        int sourceEnd = sql.indexOf(" FROM forms");
        String sourceColumns = sql.substring(sourceStart, sourceEnd).replace("f.", "");

        assertEquals(targetColumns, sourceColumns);
        assertEquals(expectedColumns, new LinkedHashSet<>(Arrays.asList(targetColumns.split(", "))));
    }

    private static int countOccurrences(String value, String substring) {
        return (value.length() - value.replace(substring, "").length()) / substring.length();
    }
}
