package sk.iway.iwcm.components.blog.jpa;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/** Fields used by the create-only Blog section editor. */
@Getter
@Setter
public class BlogSectionBean {
    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @NotNull
    @Positive
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer parentGroupId;

    @DataTableColumn(inputType = DataTableColumnType.DISABLED, title = "editor.superior_directory")
    private String parentPath;

    @NotBlank
    @Size(max = 255)
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "editor.directory_name", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-validation", value = "true"),
            @DataTableColumnEditorAttr(key = "data-dt-escape-slash", value = "true")
        })
    })
    private String groupName;
}
