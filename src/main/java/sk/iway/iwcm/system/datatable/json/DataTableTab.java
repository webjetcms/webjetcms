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
    private String content;

    public DataTableTab(DataTableColumn annotation, boolean selected) {
        setId(annotation.tab());
        String key = "editor.tab." + annotation.tab();
        String translated = DataTableColumnsFactory.translate(key);
        if (key.equals(translated)) translated = annotation.tab();
        setTitle(translated);
        setSelected(selected);
    }
}