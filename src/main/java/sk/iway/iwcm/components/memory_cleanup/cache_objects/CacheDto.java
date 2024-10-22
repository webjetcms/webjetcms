package sk.iway.iwcm.components.memory_cleanup.cache_objects;

import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

import javax.validation.constraints.Size;
import java.util.Date;

public class CacheDto {

    @DataTableColumn(
        inputType = {DataTableColumnType.ID},
        title = "[[#{components.memory_cleanup.cache_objects.id}]]",
        filter = false
    )
    private Long id;

    @DataTableColumn(
            inputType = {DataTableColumnType.OPEN_EDITOR},
            renderFormat = "dt-format-text-wrap",
            title = "[[#{components.data_deleting.name}]]",
            tab = "basicTab",
            editor = @DataTableColumnEditor(
                    type = "text",
                    attr = @DataTableColumnEditorAttr(
                            key = "disabled",
                            value = "disabled"
                    )
            )
    )
    private String name;

    @DataTableColumn(
            renderFormat = "dt-format-date-time",
            title = "[[#{components.data_deleting.expiry}]]",
            tab = "basicTab",
            editor = @DataTableColumnEditor(
                    type = "datetime",
                    attr = @DataTableColumnEditorAttr(
                            key = "disabled",
                            value = "disabled"
                    )
            )
    )
    private Date expirationDate;

    @DataTableColumn(
            renderFormat = "dt-format-number",
            title = "[[#{components.memory_cleanup.cache_objects.size}]]",
            tab = "basicTab",
            editor = @DataTableColumnEditor(
                    type = "text",
                    attr = @DataTableColumnEditorAttr(
                            key = "disabled",
                            value = "disabled"
                    )
            )
    )
    private long size;

    @Size(max = 1000)
    @DataTableColumn(
            visible = false,
            tab = "descriptionTab",
            editor = @DataTableColumnEditor(
                    type = "textarea",
                    attr = @DataTableColumnEditorAttr(
                            key = "class",
                            value = "textarea-code"
                    )
            )
    )
    private String toStringObjectValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getToStringObjectValue() {
        return toStringObjectValue;
    }

    public void setToStringObjectValue(String toStringObjectValue) {
        this.toStringObjectValue = toStringObjectValue;
    }
}
