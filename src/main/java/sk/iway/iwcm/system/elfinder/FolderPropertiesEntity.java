package sk.iway.iwcm.system.elfinder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
    @DataTableColumn(inputType = DataTableColumnType.ID)
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