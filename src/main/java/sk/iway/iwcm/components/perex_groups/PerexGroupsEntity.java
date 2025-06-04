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
import sk.iway.iwcm.Tools;
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
public class PerexGroupsEntity implements Serializable {

    @Id
    @Column(name = "perex_group_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_perex_groups")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "perex_group_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{editor.perex_group_name}]]",
        tab = "basic",
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

    @Column(name = "perex_group_name_sk")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.slovak}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameSk;

    @Column(name = "perex_group_name_cz")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.czech}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameCz;

    @Column(name = "perex_group_name_en")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.english}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameEn;

    @Column(name = "perex_group_name_de")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.deutsch}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameDe;

    @Column(name = "perex_group_name_pl")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.polish}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNamePl;

    @Column(name = "perex_group_name_ru")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.ru}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameRu;

    @Column(name = "perex_group_name_hu")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.hungary}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameHu;

    @Column(name = "perex_group_name_cho")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.chorvatsky}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameCho;

    @Column(name = "perex_group_name_esp")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{temp.esp}]]",
        tab = "translates"
    )
    @Size(max = 255)
    private String perexGroupNameEsp;

    /**
     * Get perex group name by language
     * @param lng
     * @return
     */
    public String getPerexGroupName(String lng) {
        String name = getPerexGroupNameInternal(lng);
        //Get value by REQUIRED perexGroupName
        if(Tools.isEmpty(name)) name = perexGroupName;

        return name;
    }

    private String getPerexGroupNameInternal(String lng) {
        if (Tools.isEmpty(lng)) return "";
        switch(lng) {
            case "sk": return perexGroupNameSk;
            case "cz": return perexGroupNameCz;
            case "en": return perexGroupNameEn;
            case "de": return perexGroupNameDe;
            case "pl": return perexGroupNamePl;
            case "ru": return perexGroupNameRu;
            case "hu": return perexGroupNameHu;
            case "cho": return perexGroupNameCho;
            case "esp": return perexGroupNameEsp;
            default: return "";
        }
    }

    @Column(name = "field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_a",
		visible = false,
		tab = "fields"
    )
	private String fieldA;

    @Column(name = "field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_b",
		visible = false,
		tab = "fields"
    )
	private String fieldB;

    @Column(name = "field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_c",
		visible = false,
		tab = "fields"
    )
	private String fieldC;

    @Column(name = "field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_d",
		visible = false,
		tab = "fields"
    )
	private String fieldD;

    @Column(name = "field_e")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_e",
		visible = false,
		tab = "fields"
    )
	private String fieldE;

    @Column(name = "field_f")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.perex.field_f",
		visible = false,
		tab = "fields"
    )
	private String fieldF;

    public void addAvailableGroup(int availableGroupId) {
        if(Tools.isEmpty(availableGroups)) availableGroups = "" + availableGroupId;
        else {
            if(availableGroups.endsWith(",")) availableGroups += availableGroupId;
            else availableGroups += "," + availableGroupId;
        }
    }
}
