package sk.iway.iwcm.components.customfields.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@Entity
@Table(name = "custom_fields")
@Getter
@Setter
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
    @DataTableTab(id = "bonus", title = "settings.custom-fields.tabs.bonus")
})
public class CustomFieldsEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_custom_fields")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "settings.custom-fields.class_name", tab = "basic", className = "ai-off",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/custom-fields/autocomplete-class"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @NotBlank
    private String className;

    @Column(name="alphabet")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "settings.custom-fields.alphabet", tab = "basic", className = "ai-off")
    @NotNull
    private String alphabet;

    @Column(name = "entity_id")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.entity_id", tab = "basic", className = "ai-off")
    private Long entityId;

    @Column(name="type")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "settings.custom-fields.type", tab = "basic", className = "ai-off")
    private String type;

    @Column(name="label")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.label", tab = "basic")
    private String label;

    @Column(name="tooltip")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.tooltip", tab = "basic")
    private String tooltip;

    @Column(name="required")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "settings.custom-fields.required", tab = "basic")
    private Boolean required;

    @Column(name = "bonus_class_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.bonus_class_name", tab = "bonus", className = "ai-off",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/custom-fields/autocomplete-class"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "false")
				}
			)
		}
    )
    private String bonusClassName;

    @Column(name = "bonus_entity_id")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.bonus_entity_id", tab = "bonus", className = "ai-off")
    private Long bonusEntityId;

    @Column(name="domain_id")
	private Integer domainId;

    @Lob
    @Column(name="value")
	private String value;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.textMaxLength", tab = "basic", hidden = true, className = "ai-off specific-field")
    private Integer textMaxLength;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.textWarningLength", tab = "basic", hidden = true, className = "ai-off specific-field")
    private Integer textWarningLength;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "settings.custom-fields.textWarningText", tab = "basic", hidden = true, className = "ai-off specific-field")
    private String textWarningText;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.OPTIONS, title = "settings.custom-fields.selectOptions", tab = "basic", hidden = true, className = "specific-field")
    private String selectOptions;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.BASIC_OPTIONS, title = "", tab = "basic", hidden = true, className = "specific-field")
    private String autocompleteOptions;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "settings.custom-fields.docInGroup", tab = "basic", hidden = true, className = "dt-tree-group specific-field")
    private GroupDetails docInGroup;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.ENUMERATION, title = "settings.custom-fields.enumeration", tab = "basic", hidden = true, className = "specific-field")
    private String enumeration;

    public void setAlphabet(String alphabet) {
        if(Tools.isNotEmpty(alphabet))
            this.alphabet = alphabet.toUpperCase();
        else this.alphabet = null; //NOSONAR
    }

    public Character getCharacterAlphabet() {
        if(Tools.isEmpty(alphabet)) return null;
        return alphabet.charAt(0);
    }

    @PrePersist
    @PreUpdate
    private void normalizeBonusFields() {
        if(Tools.isEmpty(bonusClassName)) {
            bonusClassName = "";
        }
        if(bonusEntityId == null || bonusEntityId < 1) {
            bonusEntityId = 0L;
        }
        if(entityId == null || entityId < 1) {
            entityId = 0L;
        }
    }
}