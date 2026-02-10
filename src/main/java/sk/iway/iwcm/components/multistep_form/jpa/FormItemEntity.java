package sk.iway.iwcm.components.multistep_form.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "form_items")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
    @DataTableTab(id = "advanced", title = "datatable.tab.advanced")
})
public class FormItemEntity extends BaseEditorFields {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_items")
    @DataTableColumn(inputType = DataTableColumnType.ID, hidden = true, tab = "basic")
    @Accessors(chain = false)
    private Long id;

    @Column(name = "step_id")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.form_items.step", hidden = true, tab = "advanced")
    private Integer stepId;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER, title = "", className = "icon-only", filter = false, tab = "advanced")
    private Integer sortPriority;

    @Column(name = "form_name")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, tab = "basic")
    private String formName;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.label", hiddenEditor = true)
    private transient String generatedTitle;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.form_items.item_preview", hidden = true, hiddenEditor = true, className = "allow-html")
    private transient String generatedItem;

    @Column(name = "field_type")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.formsimple.fieldType", hidden = true, tab = "basic")
    private String fieldType;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.formsimple.required", hidden = true, tab = "basic")
    private Boolean required;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title = "components.form_items.regex_validation", hidden = true, tab = "basic")
    private transient Integer[] regexValidationArr;

    @Column(name = "label")
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.label", className="quill-oneline", hidden = true)
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String label;

    @Column(name = "value")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.value", hidden = true, tab = "advanced")
    private String value;

    @Column(name = "placeholder")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.placeholder", hidden = true, tab = "advanced")
    private String placeholder;

    @Column(name = "tooltip")
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.tooltip", className="quill-oneline", hidden = true, tab = "advanced")
    @jakarta.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String tooltip;

    @Column(name = "regex_validation")
    private String regexValidation;

    @Column(name = "item_form_id")
    private String itemFormId;

    @Column(name = "domain_id")
    private Integer domainId;
}
