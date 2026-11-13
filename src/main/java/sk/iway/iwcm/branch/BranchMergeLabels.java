package sk.iway.iwcm.branch;

import java.lang.reflect.Field;
import java.util.*;
import jakarta.persistence.Column;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaEditorFields;

/** Comparison labels use the editor's original keys, including inherited and nested fields. */
final class BranchMergeLabels {
    private static final Map<BranchType,Map<String,String>> KEYS=keys();

    private BranchMergeLabels() { }

    static Map<String,Map<String,String>> translate(Prop prop) {
        Map<String,Map<String,String>> result=new LinkedHashMap<>();
        KEYS.forEach((type,keys)->{
            Map<String,String> labels=new LinkedHashMap<>();
            keys.forEach((field,key)->{
                String label=prop.getText(key);
                if(field.startsWith("media_title_")||field.startsWith("media_info_"))label+=" ("+field.substring(field.length()-2)+")";
                labels.put(field,label);
            });
            result.put(type.name(),labels);
        });
        return result;
    }

    private static Map<BranchType,Map<String,String>> keys() {
        Map<BranchType,Map<String,String>> result=new EnumMap<>(BranchType.class);
        Map<String,String> documents=new LinkedHashMap<>(Map.of("logon_page_doc_id","groupedit.log_on_page"));
        titles(documents,DocDetails.class,Map.of("authorName","author_id"));
        titles(documents,DocEditorFields.class,Map.of("groupDetails","group_id","permisions","password_protected"));
        result.put(BranchType.DOCUMENT,documents);

        Map<String,String> groups=new LinkedHashMap<>(Map.of(
            "force_group_template","groupedit.force_the_use_of_group_template",
            "link_group_id","groupedit.linked_group_id","hidden_in_admin","groupedit.hidden_in_admin"));
        titles(groups,GroupDetails.class,Map.of("navbarName","navbar","newPageDocIdTemplate","new_page_docid_template"));
        titles(groups,GroupEditorField.class,Map.of("parentGroupDetails","parent_group_id","defaultDocDetails","default_doc_id",
            "permisions","password_protected","logonPage","logon_page_doc_id"));
        result.put(BranchType.GROUP,groups);

        Map<String,String> media=new LinkedHashMap<>(Map.of("media_fk_id","components.media.docId",
            "media_fk_table_name","branches.merge.field.media_fk_table_name.js","media_group","editor.media.group"));
        titles(media,Media.class,Map.of());
        titles(media,MediaEditorFields.class,Map.of("groups","group_ids_snapshot"));
        for(String language:List.of("sk","cz","en","de")) {
            media.put("media_title_"+language,media.get("media_title_sk"));
            media.put("media_info_"+language,"branches.merge.field.media_info.js");
        }
        result.put(BranchType.MEDIA,media);
        result.put(BranchType.FILE,new LinkedHashMap<>(Map.of("entry_kind","branches.merge.field.entry_kind.js",
            "logical_path","sync.path","content_hash","branches.merge.field.content_hash.js")));
        for(BranchType type:BranchType.values())result.get(type).keySet().removeIf(field->
            !BranchFields.forType(type).supported().contains(field)&&!(type==BranchType.MEDIA&&"group_ids_snapshot".equals(field)));
        return result;
    }

    private static void titles(Map<String,String> result,Class<?> type,Map<String,String> aliases) {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())for(Field field:current.getDeclaredFields()) {
            DataTableColumn annotation=field.getAnnotation(DataTableColumn.class);
            if(annotation==null)continue;
            String key=annotation.title().replace("[[#{","").replace("}]]","");
            if(key.isBlank()||"&nbsp;".equals(key))continue;
            Column column=field.getAnnotation(Column.class);
            String name=column!=null&&!column.name().isBlank()?column.name().trim():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            result.put(aliases.getOrDefault(field.getName(),name),key);
        }
    }
}
