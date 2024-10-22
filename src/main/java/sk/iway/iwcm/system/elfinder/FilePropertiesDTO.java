package sk.iway.iwcm.system.elfinder;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;


@Getter
@Setter
public class FilePropertiesDTO {
    
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="fbrowse.file_name",
        tab = "basic"
    )
    private String fileName;

    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
    private String originalFileName;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="fbrowse.change_file_name",
        tab = "basic"
    )
    private boolean changeAllFileNames;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="elfinder.file_prop.dirPath",
        tab = "basic",
        editor = {
            @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") } )
        }
    )
    private String dirPath;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "usage",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/file-usage?filePath={fullPath}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.elfinder.FileUsageDTO"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "fbrowse.usage"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<DocDetails> docDetailsList;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "history",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/file-history?filePath={fullPath}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.elfinder.FileHistoryEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "elfinder.file_prop.history.headline"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<FileHistoryEntity> fileHistoryList;

    public String getFullPath() {
        return dirPath + fileName;
    }
}