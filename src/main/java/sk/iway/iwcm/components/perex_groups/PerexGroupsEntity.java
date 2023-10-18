package sk.iway.iwcm.components.perex_groups;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "perex_groups")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_PEREX_GROUP_UPDATE)
public class PerexGroupsEntity implements Serializable{

    @Id
    @Column(name = "perex_group_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_perex_groups")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", renderFormat = "dt-format-selector")
    private Long id;

    @Column(name = "perex_group_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{editor.perex_group_name}]]",
        renderFormat = "dt-format-text",
        editor = {
            @DataTableColumnEditor(type = "text")
        }
    )
    @Size(max = 255)
    @NotBlank
    private String perexGroupName;

    @Column(name = "related_pages")
    private String relatedPages;


    @Size(max = 255)
    @Column(name = "available_groups")
    private String availableGroups;

    @Transient
    @DataTableColumnNested
	private PerexGroupsEditorFields editorFields = null;
}
