package sk.iway.iwcm.components.translation_keys.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
@Setter
@Getter
public class MissingKeysDto {

    public MissingKeysDto() {}

    public MissingKeysDto(String key, Date lastMissing, String language, String urlAddress) {
        this.key = key;
        this.lastMissing = lastMissing;
        this.language = language;
        this.urlAddress = urlAddress;
    }

    @DataTableColumn(inputType = DataTableColumnType.ID, filter = false)
    Long id;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "user.admin.key", className = "show-html")
    String key;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "missing-keys.last_missing", editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    Date lastMissing;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "admin.temps_list.jazyk")
    String language;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "searchall.url", editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
    String urlAddress;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "missing-keys.translation", hidden = true)
    String translation;
}
