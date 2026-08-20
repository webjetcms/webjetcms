package sk.iway.iwcm.system.elfinder;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.filebrowser.UnusedFile;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class UnusedFileDTO {

    public UnusedFileDTO() {
        // Default constructor for JSON deserialization.
    }

    public static UnusedFileDTO fromUnusedFile(Long id, UnusedFile unusedFile) {
        UnusedFileDTO dto = new UnusedFileDTO();
        dto.id = id;
        dto.fileName = unusedFile.getName();
        String separator = unusedFile.getVirtualParent().endsWith("/") ? "" : "/";
        dto.fullPath = unusedFile.getVirtualParent() + separator + unusedFile.getName();
        dto.lastModified = new Date(unusedFile.getLastModified());
        dto.size = unusedFile.getLength();
        return dto;
    }

    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "elfinder.folder_prop.unused_files.file_name",
        renderFormatLinkTemplate = "javascript:openFile('{{fullPath}}');"
    )
    private String fileName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "editor.virtual_path"
    )
    private String fullPath;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "fbrowse.date"
    )
    private Date lastModified;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "fbrowse.size",
        renderFormat = "dt-format-filesize"
    )
    private Long size;
}
