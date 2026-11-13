package sk.iway.iwcm.branch;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.Column;
import org.springframework.beans.BeanUtils;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.spirit.model.Media;

/** Reads only classified WORK fields; operational and scheduling values stay in the server row. */
final class BranchEditorRows {
    private static final Map<String,Method> DOCUMENT_READERS = readers(DocDetails.class,BranchFields.DOCUMENT.supported(),
        Map.of("perex_group","perexGroupIdsString"));
    private static final Map<String,Method> GROUP_READERS = readers(GroupDetails.class,BranchFields.GROUP.supported(),
        Map.of("navbar","navbarName", "new_page_docid_template","newPageDocIdTemplate", "force_group_template","forceTheUseOfGroupTemplate"));
    private static final Map<String,Method> MEDIA_READERS = readers(Media.class,BranchFields.MEDIA.supported(),Map.of());

    private BranchEditorRows() { }

    static Map<String,Object> document(DocDetails bean, Map<String,?> stored) {
        Map<String,Object> row = read(bean,stored,DOCUMENT_READERS);
        // The legacy getter exposes milliseconds instead of the Date stored in the JPA field.
        row.put("date_created",new java.sql.Timestamp(bean.getDateCreated()));
        return row;
    }

    static Map<String,Object> group(GroupDetails bean, Map<String,?> stored) {
        return read(bean,stored,GROUP_READERS);
    }

    static Map<String,Object> media(Media bean, Map<String,?> stored) {
        return read(bean,stored,MEDIA_READERS);
    }

    private static Map<String,Object> read(Object bean, Map<String,?> stored, Map<String,Method> readers) {
        Map<String,Object> row = new LinkedHashMap<>(stored);
        for (var entry:readers.entrySet()) {
            try { row.put(entry.getKey(),entry.getValue().invoke(bean)); }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot read WORK column "+entry.getKey()+" from "+bean.getClass().getSimpleName(),ex);
            }
        }
        return row;
    }

    /** Resolve every supported column once; missing or ambiguous mappings must never be silently skipped. */
    static Map<String,Method> readers(Class<?> type, Set<String> columns, Map<String,String> aliases) {
        Map<String,String> properties = new LinkedHashMap<>();
        for (Class<?> current=type;current!=null;current=current.getSuperclass()) {
            for (Field field:current.getDeclaredFields()) {
                Column annotation=field.getAnnotation(Column.class);
                if (annotation==null) continue;
                String column=annotation.name().isBlank() ? field.getName() : annotation.name().trim();
                if (properties.putIfAbsent(column,field.getName())!=null)
                    throw new IllegalStateException("Ambiguous column "+column+" on "+type.getSimpleName());
            }
        }
        Map<String,Method> result = new LinkedHashMap<>();
        for (String column:columns) {
            String property=aliases.getOrDefault(column,properties.getOrDefault(column,propertyName(column)));
            PropertyDescriptor descriptor=BeanUtils.getPropertyDescriptor(type,property);
            if (descriptor==null || descriptor.getReadMethod()==null)
                throw new IllegalStateException("Missing getter for WORK column "+column+" ("+type.getSimpleName()+"."+property+")");
            result.put(column,descriptor.getReadMethod());
        }
        return Map.copyOf(result);
    }

    private static String propertyName(String column) {
        StringBuilder name=new StringBuilder();
        boolean uppercase=false;
        for (char character:column.toCharArray()) {
            if (character=='_') uppercase=true;
            else { name.append(uppercase ? Character.toUpperCase(character) : character); uppercase=false; }
        }
        return name.toString();
    }
}
