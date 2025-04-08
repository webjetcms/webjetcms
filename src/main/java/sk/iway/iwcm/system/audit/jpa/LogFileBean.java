package sk.iway.iwcm.system.audit.jpa;

import java.io.File;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Bean to hold log files for DataTable
 */
@Getter
@Setter
public class LogFileBean {

    public LogFileBean(File file, Long id) {
        this.file = file;
        this.id = id;
    }

    //Source file
    private File file;

	@DataTableColumn(inputType = DataTableColumnType.ID, hidden = true)
	private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "fbrowse.title",
        renderFormatLinkTemplate = "javascript:getAbsolutePath('{{absolutePath}}');"
    )
	private String fileName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "fbrowse.size",
        renderFormat = "dt-format-filesize"
    )
	private long size;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "fbrowse.date"
    )
	private Date lastChange;

    public String getFileName() {
        return this.file.getName();
    }

    public String getAbsolutePath() {
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            //Need to replace all backslashes with double backslashes
            return this.file.getAbsolutePath().replace("\\", "\\\\" );
        }

        return this.file.getAbsolutePath();
    }

    public long getSize() {
        return this.file.length();
    }

    public long getLastChange() {
        return this.file.lastModified();
    }
}
