package sk.iway.iwcm.system.elfinder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "dirprop")
@Getter
@Setter
public class FolderPropertiesEntity {
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_dirprop")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
    private Long id;

    @Column(name = "dir_url")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "editor.virtual_path",
        tab = "basic",
        sortAfter = "editorFields.dirName",
        editor = {
            @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") } )
        }
    )
    @NotBlank
    private String dirUrl;

    @Column(name = "index_fulltext")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "fbrowse.dirprop.index_fulltext",
        tab = "basic"
    )
    private boolean indexFullText;

    @Column(name = "password_protected")
    private String passwordProtected;

    @Column(name = "logon_doc_id")
    Integer logonDocId;

    @DataTableColumnNested
	@Transient
	private FolderPropertiesEditorFields editorFields = null;
}