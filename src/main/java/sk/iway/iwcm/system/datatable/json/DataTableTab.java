package sk.iway.iwcm.system.datatable.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Trieda pre generovanie JSONu pre DataTable {@see https://datatables.net/} z
 * anotacie {@link sk.iway.iwcm.system.datatable.annotations.DataTableColumn}
 * nad poliami objektu. Trieda je priamo mapovatelna pomocou
 * {@link com.fasterxml.jackson.databind.ObjectMapper}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableTab {
    private String id;
    private String title;
    private boolean selected;
    private boolean hideOnCreate;
    private String content;

    public DataTableTab(String id, String title, boolean selected) {
        setId(id);
        String key;
        if (title.contains(".")==false) key = "editor.tab." + title;
        else key = title;
        String translated = DataTableColumnsFactory.translate(key);
        if (key.equals(translated)) translated = title;
        setTitle(translated);
        setSelected(selected);
        hideOnCreate = false;
        content = null;
    }

    public DataTableTab(DataTableColumn annotation, boolean selected) {
        this(annotation.tab(), annotation.tab(), selected);
    }

    public DataTableTab(sk.iway.iwcm.system.datatable.annotations.DataTableTab annotation) {
        this(annotation.id(), annotation.title(), annotation.selected());
        setHideOnCreate(annotation.hideOnCreate());
        String content = annotation.content();
        if ("null".equals(content)) content = null;
        setContent(content);
    }
}