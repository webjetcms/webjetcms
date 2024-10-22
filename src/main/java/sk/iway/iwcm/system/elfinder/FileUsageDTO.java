package sk.iway.iwcm.system.elfinder;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class FileUsageDTO {

    public FileUsageDTO() {}

    public FileUsageDTO(String fileName, String fileUrl, Integer docId) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileUrlHidden = fileUrl;
        this.docId = docId;
    }

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "sync.title",
        renderFormatLinkTemplate = "javascript:openDocDetails({{docId}});"
    )
    private String fileName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "editor.virtual_path",
        renderFormatLinkTemplate = "javascript:openFile('{{fileUrl}}');"
    )
    private String fileUrl;

    //Need as param for renderFormatLinkTemplate
    private String fileUrlHidden;
    private Integer docId;
}