package sk.iway.iwcm.components.gallery;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class GalleryDimensionEditorFields implements Serializable{

    @DataTableColumn(
        renderFormat = "dt-format-checkbox",
        title = "[[#{}]]",
        visible = false,
        sortAfter = "resizeMode",
        editor = {
            @DataTableColumnEditor(
                type = "checkbox",
                tab = "sizes",
                options = {
                    @DataTableColumnEditorAttr(key = "components.gallery.regenerate.all", value = "true")
                },
                message = "components.gallery.regenerate.all.title"
            )
        }
    )
    private boolean regenerateImages = false;

    @DataTableColumn(
        renderFormat = "dt-format-checkbox",
        title = "[[#{}]]",
        visible = false,
        sortAfter = "editorFields.regenerateImages",
        editor = {
            @DataTableColumnEditor(
                type = "checkbox",
                tab = "sizes",
                options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true")
                }
            )
        }
    )
    private boolean forceResizeModeToSubgroups = false;

}
