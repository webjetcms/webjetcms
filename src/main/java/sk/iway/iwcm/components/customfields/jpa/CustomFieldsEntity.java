package sk.iway.iwcm.components.customfields.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "custom_fields")
@Getter
@Setter
public class CustomFieldsEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_custom_fields")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.class_name", tab = "basic", className = "ai-off",
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
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.alphabet", tab = "basic", className = "ai-off")
    @NotNull
    @Size(min = 1, max = 1)
    private String alphabet;

    @Column(name = "entity_id")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.entity_id", tab = "basic", className = "ai-off")
    @Min(1)
    private Long entityId;

    @Column(name="type")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.custom-fields.type", tab = "basic", className = "ai-off")
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
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    private String bonusClassName;

    @Column(name = "bonus_entity_id")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "settings.custom-fields.bonus_entity_id", tab = "bonus", className = "ai-off")
    @Min(1)
    private Long bonusEntityId;

    public void setAlphabet(String alphabet) {
        if(Tools.isNotEmpty(alphabet))
            this.alphabet = alphabet.toUpperCase();
        else this.alphabet = null; //NOSONAR
    }

    @PrePersist
    @PreUpdate
    private void normalizeBonusFields() {
        if(Tools.isEmpty(bonusClassName)) {
            bonusClassName = null;
        }
        if(bonusEntityId != null && bonusEntityId < 1) {
            bonusEntityId = null;
        }
        if(entityId != null && entityId < 1) {
            entityId = null;
        }
    }
}