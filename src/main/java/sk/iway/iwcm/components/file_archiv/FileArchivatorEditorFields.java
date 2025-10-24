package sk.iway.iwcm.components.file_archiv;

import java.util.Date;
import java.util.List;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;

@Getter
@Setter
public class FileArchivatorEditorFields extends BaseEditorFields {

    //Column for status icons
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, className = "allow-html", orderable = false, sortAfter = "virtualFileName"
    )
    private String statusIcons;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.file_archiv.uploadType",
        tab = "basic",
        hidden = true,
        className = "hide-on-create",
        editor = {
			@DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.file_archiv.upload_file")
                }
			)
		}
    )
    private String uploadType;

    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", className = "dt-tree-dir-simple", title="components.file_archiv.target_directory", hidden = true, editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "constant:fileArchivDefaultDirPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-skipFolders", value = "fileArchivInsertLaterDirPath"),
            }
        )
    })
    private String dir;

    @DataTableColumn(
        inputType = DataTableColumnType.UPLOAD,
        tab = "basic",
        title = "components.file_archiv.file"
    )
    private String file = "";

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title="components.file_archiv.save_later",
        hidden = true
    )
    private Boolean saveLater;

    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        tab = "basic",
        title="components.file_archiv.file_list_cakajuce.nahrat_po",
        hidden = true,
		sortAfter = "editorFields.saveLater"
    )
    private Date dateUploadLater;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        tab = "basic",
        title="components.file_archiv.emails",
        hidden = true,
        sortAfter = "editorFields.saveFileLaterDate"
    )
    private String emails;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "listOfVersions",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/file-archive?id={id}&listOfVersions=true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.file_archiv.FileArchivatorBean"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns-customize", value = "removeEditorFields"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,duplicate,import,export,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,virtualFileName,fileName,filePath,showFile,priority,orderId"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "0,asc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'advanced', 'title': '[[#{datatable.tab.advanced}]]' },{ 'id': 'customFields', 'title': '[[#{editor.tab.fields}]]' }]")
            }
        )
    })
    private List<FileArchivatorBean> listOfVersions;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title="components.file_archiv.rename_file",
        hidden = true,
        className = "hide-on-create",
        sortAfter = "fileName"
    )
    private Boolean renameFile;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.file_archiv.new_name",
        hidden = true,
        className = "hide-on-create",
        sortAfter = "editorFields.renameFile",
        editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
            })
        }
    )
    private String newFileName;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.file_archiv.upload_redundant_file",
        tab = "advanced",
        sortAfter = "note",
        hidden = true
    )
    private Boolean uploadRedundantFile = false;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "waitingFiles",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/file-archive?id={id}&waitingFiles=true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.file_archiv.FileArchivatorBean"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns-customize", value = "removeEditorFields"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,duplicate,import,export,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,virtualFileName,fileName,filePath,dateUploadLater"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "1,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'advanced', 'title': '[[#{datatable.tab.advanced}]]' },{ 'id': 'customFields', 'title': '[[#{editor.tab.fields}]]' }]")
            }
        )
    })
    private List<FileArchivatorBean> waitingFiles;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.file_archiv.save_after",
        tab = "basic",
        hidden = true,
        className = "hide-on-create"
    )
    private String saveAfterSelect;

    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "components.file_archiv.save_after",
        tab = "basic",
        className = "hide-on-create hide-on-edit",
        hidden = true
    )
    private Integer saveAfterId;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "listOfPattern",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/file-archive?id={id}&listOfPattern=true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.file_archiv.FileArchivatorBean"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns-customize", value = "removeEditorFields"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,duplicate,import,export,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,virtualFileName,fileName,filePath,dateUploadLater"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "1,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'advanced', 'title': '[[#{datatable.tab.advanced}]]' },{ 'id': 'customFields', 'title': '[[#{editor.tab.fields}]]' }]")
            }
        )
    })
    private List<FileArchivatorBean> patterns;

    public void fromFileArchivatorBean(FileArchivatorBean fileArchivatorOriginal, ProcessItemAction action, UserDetails currentUser, int rowCount, FileArchiveRepository repository) {

        statusIcons = "";

        if(action == ProcessItemAction.CREATE) {
            dir = Constants.getString("fileArchivDefaultDirPath");
            if(dir.startsWith(FileArchivSupportMethodsService.SEPARATOR) == false) dir = FileArchivSupportMethodsService.SEPARATOR + dir;
            fileArchivatorOriginal.setShowFile(true);
            emails = currentUser.getEmail();
        } else if(action == ProcessItemAction.EDIT) {
            dir = fileArchivatorOriginal.getFilePath();
            emails = currentUser.getEmail();
            prepareIconsAndClasses(fileArchivatorOriginal, repository);
        } else if(action == ProcessItemAction.FIND) {
            prepareIconsAndClasses(fileArchivatorOriginal, repository);
        }

        if(rowCount == 1) {
            setFieldsDefinition(this.getFields(fileArchivatorOriginal, "components.file_archiv", 'E'));
        }

        fileArchivatorOriginal.setEditorFields(this);
    }

    private void prepareIconsAndClasses(FileArchivatorBean entity, FileArchiveRepository repository) {
        //Is mainFile ?
        if (entity.getReferenceId() == -1)
            addStatusIcon("ti ti-star");
        else {
            addStatusIcon("ti ti-star-off");
            addRowClass("notMainFile");
        }

        //Is show
        if (Tools.isTrue(entity.getShowFile()))
            addStatusIcon("ti ti-map-pin");
        else
            addStatusIcon("ti ti-map-pin-off");

        //IS pattern file ?
        if(Tools.isNotEmpty(entity.getReferenceToMain())) addStatusIcon("ti ti-texture");

        //IS waiting for upload
        if(entity.getUploaded() == 0) {
            addRowClass("waitingForUpload");
            addStatusIcon("ti ti-calendar-time");
        } else {
            //Check if have waiting files
            List<FileArchivatorBean> waitingFilesList = FileArchiveService.getWaitingFileList(entity.getId(), repository);
            if(waitingFilesList != null && waitingFilesList.isEmpty() == false) {
                addStatusIcon("ti ti-calendar-plus");
            }
        }

        statusIcons = getStatusIconsHtml();
    }
}