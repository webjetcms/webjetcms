package sk.iway.iwcm.components.customfields.rest;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.database.DataSource;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

/**
 * Restores configured JSON text from its persisted source after the generic JPA HTML converter.
 * Other custom fields retain their existing HTML filtering.
 */
public final class JsonEditorValueReader {

    private JsonEditorValueReader() {}

    /**
     * Restores JSON properties of an entity that has already been loaded and authorized.
     * @param entity persisted entity
     * @param fieldNames JSON property names taken from generated field definitions
     */
    public static void restore(Object entity, Collection<String> fieldNames) {
        restore(entity, entity, fieldNames);
    }

    private static void restore(Object target, Object source, Collection<String> fieldNames) {
        if (target == null || source == null || fieldNames.isEmpty()) return;
        if (source instanceof DocDetails doc && doc.getHistoryId() > 0) {
            DocHistory history = new DocHistory();
            history.setId((long)doc.getHistoryId());
            source = history;
        }
        Class<?> sourceClass = source.getClass();
        while (sourceClass != null && sourceClass.isAnnotationPresent(Entity.class) == false) {
            sourceClass = sourceClass.getSuperclass();
        }
        if (sourceClass == null) return;
        BeanWrapperImpl bean = new BeanWrapperImpl(target);
        List<String> names = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        for (String name : fieldNames) {
            Object value = bean.getPropertyValue(name);
            if (value instanceof String text && text.indexOf('<') < 0 && text.indexOf('>') < 0
                    && (text.contains("&lt;") || text.contains("&gt;"))) {
                Field field = ReflectionUtils.findField(sourceClass, name);
                if (field == null || field.getType() != String.class) continue;
                names.add(name);
                columns.add(columnName(field));
            }
        }
        if (names.isEmpty()) return;
        Field idField = null;
        for (Class<?> type = sourceClass; type != null && idField == null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    break;
                }
            }
        }
        if (idField == null) return;
        Object id = new BeanWrapperImpl(source).getPropertyValue(idField.getName());
        if (id == null || (id instanceof Number number && number.longValue() < 1)) return;
        Table table = sourceClass.getAnnotation(Table.class);
        String tableName = table == null || table.name().isEmpty() ? sourceClass.getSimpleName() : table.name();
        if (table != null && table.schema().isEmpty() == false) tableName = table.schema() + "." + tableName;
        DataSource dataSource = sourceClass.getAnnotation(DataSource.class);
        String database = dataSource == null ? "iwcm" : dataSource.name();
        String sql = "SELECT " + String.join(",", columns) + " FROM " + tableName + " WHERE " + columnName(idField) + "=?";
        try (Connection connection = DBPool.getConnection(database);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    for (int index = 0; index < names.size(); index++) {
                        String raw = result.getString(index + 1);
                        Object current = bean.getPropertyValue(names.get(index));
                        if (raw != null && raw.equals(current) == false && DB.filterHtml(raw).equals(current)) {
                            bean.setPropertyValue(names.get(index), raw);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot read persisted JSON custom fields for " + sourceClass.getName(), ex);
        }
    }

    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return column == null || column.name().isEmpty() ? field.getName() : column.name();
    }
}
