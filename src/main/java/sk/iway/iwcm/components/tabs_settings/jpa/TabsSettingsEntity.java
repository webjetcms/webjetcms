package sk.iway.iwcm.components.tabs_settings.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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

/**
 * Stores editor tab settings for an entity class and domain.
 * Nullable Boolean settings distinguish an explicit value from an unchanged setting.
 */
@Entity
@Table(name = "tabs_settings")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CONF_UPDATE)
public class TabsSettingsEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_tabs_settings")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "class_name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "settings.custom-fields.class_name", tab = "basic", className = "ai-off",
        editor = @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/tabs-settings/autocomplete-class"),
            @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
            @DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
        })
    )
    @NotBlank
    @Size(max = 255)
    private String className;

    @Column(name = "tab_id")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.tabs-settings.tab_id", tab = "basic", className = "ai-off",
        editor = @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/tabs-settings/autocomplete-tab"),
            @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
            @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
            @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className"),
            @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
        })
    )
    @NotBlank
    @Size(max = 255)
    private String tabId;

    @Column(name = "title")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.tabs-settings.tab_title", tab = "basic", className = "ai-off")
    @Size(max = 255)
    private String title;

    @Column(name = "selected")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.tabs-settings.selected", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean selected;

    @Column(name = "hide_on_create")
    @DataTableColumn(inputType = DataTableColumnType.BUTTON_GROUP, title = "settings.tabs-settings.hide_on_create", tab = "basic", defaultValue = "null", alwaysCopyProperties = true,
        editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "button.yes", value = "true"),
            @DataTableColumnEditorAttr(key = "button.no", value = "false"),
            @DataTableColumnEditorAttr(key = "settings.fields-settings.unchanged", value = "null")
        })
    )
    private Boolean hideOnCreate;

    @Column(name = "perms")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.tabs-settings.perms", tab = "basic", className = "ai-off")
    @Size(max = 255)
    private String perms;

    @Column(name = "css_class")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.fields-settings.css_class", tab = "basic", className = "ai-off")
    @Size(max = 255)
    private String cssClass;

    @Lob
    @Column(name = "content")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "settings.tabs-settings.content", tab = "basic", className = "ai-off", visible = false, filter = false, orderable = false)
    private String content;

    @Column(name = "sort_after")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "settings.tabs-settings.sort_after", tab = "basic", className = "ai-off",
        editor = @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/tabs-settings/autocomplete-tab"),
            @DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
            @DataTableColumnEditorAttr(key = "data-ac-select", value = "true"),
            @DataTableColumnEditorAttr(key = "data-ac-params", value = "#DTE_Field_className")
        })
    )
    @Size(max = 255)
    private String sortAfter;

    @Column(name = "domain_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer domainId;
}
