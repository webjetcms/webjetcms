package sk.iway.iwcm.components.gallery;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class GalleryDimensionEditorFields implements Serializable{

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "&nbsp;",
        visible = false,
        sortAfter = "resizeMode",
        tab = "sizes",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.gallery.regenerate.all", value = "true")
                },
                message = "components.gallery.regenerate.all.title"
            )
        }
    )
    private boolean regenerateImages = false;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "&nbsp;",
        visible = false,
        sortAfter = "editorFields.regenerateImages",
        tab = "sizes",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true")
                }
            )
        }
    )
    private boolean forceResizeModeToSubgroups = false;


    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "&nbsp;",
        visible = false,
        sortAfter = "watermarkSaturation",
        tab = "watermark",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.gallery.regenerate.all", value = "true")
                },
                message = "components.gallery.regenerate.all.title"
            )
        }
    )
    private boolean regenerateWatermark = false;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "&nbsp;",
        visible = false,
        sortAfter = "editorFields.regenerateWatermark",
        tab = "watermark",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "editor.apply_for_all_sub_folders", value = "true")
                }
            )
        }
    )
    private boolean forceWatermarkToSubgroups = false;

}
