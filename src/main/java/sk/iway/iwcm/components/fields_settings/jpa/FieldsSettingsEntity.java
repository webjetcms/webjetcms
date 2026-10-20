package sk.iway.iwcm.components.fields_settings.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "fields_settings")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CONF_UPDATE)
public class FieldsSettingsEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_fields_settings")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "settings.custom-fields.class_name", tab = "basic", className = "ai-off",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/fields-settings/autocomplete-class"),
					@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
					@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
				}
			)
		}
    )
    @NotBlank
    @Size(max = 255)
    private String className;

    @Column(name = "field_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.fields-settings.field_name", tab = "basic", className = "ai-off",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/fields-settings/autocomplete-field"),
                    @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
                }
            )
        }
    )
    @NotBlank
    @Size(max = 255)
    private String fieldName;

    @Column(name = "tab")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.fields-settings.tab_name", tab = "basic", className = "ai-off",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/fields-settings/autocomplete-tab"),
                    @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
                }
            )
        }
    )
    @Size(max = 255)
    private String tab;

    @Column(name = "title")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.fields-settings.field_title", tab = "basic", className = "ai-off")
    @Size(max = 255)
    private String title;

    @Column(name = "sort_after")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.fields-settings.sort_after", tab = "basic", className = "ai-off",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/fields-settings/autocomplete-field"),
                    @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
                    @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
                    @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
                }
            )
        }
    )
    @Size(max = 255)
    private String sortAfter;

    @Column(name = "visible")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.visible", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean visible;

    @Column(name = "hidden")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.hidden", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean hidden;

    @Column(name = "hiddenEditor")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.hidden_editor", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean hiddenEditor;

    @Column(name = "filter")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.filter", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean filter;

    @Column(name = "orderable")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.orderable", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean orderable;

    @Column(name = "ai")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.ai", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean ai;

    @Column(name = "disabled")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.fields-settings.disabled", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean disabled;

    @Column(name="domain_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	private Integer domainId;
}
