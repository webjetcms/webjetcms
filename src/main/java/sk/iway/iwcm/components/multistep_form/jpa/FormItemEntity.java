package sk.iway.iwcm.components.multistep_form.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_items")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_INQUIRY)
public class FormItemEntity extends BaseEditorFields {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_items")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    @Accessors(chain = false)
    private Long id;

    @Column(name = "form_name")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String formName;

    @Column(name = "step_id")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.form_items.step")
    private Integer stepId;

    @Column(name = "field_type")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.formsimple.fieldType", tab = "main")
    private String fieldType;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "", tab = "main")
    private Boolean required;

    @Column(name = "label")
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.label", className="dt-row-edit", hidden = true, tab = "main")
    private String label;

    @Column(name = "value")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.value", className="dt-row-edit", hidden = true, tab = "main")
    private String value;

    @Column(name = "placeholder")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.placeholder", hidden = true, tab = "main")
    private String placeholder;

    @Column(name = "tooltip")
    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.tooltip", hidden = true, tab = "main")
    private String tooltip;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER, title = "", tab = "main")
    private Integer sortPriority;

    @Column(name = "regex_validation")
    private String regexValidation;

    @Column(name = "item_form_id")
    private String itemFormId;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title = "", hidden = true, tab = "main")
    private transient Integer[] regexValidationArr;

    @Column(name = "domain_id")
    private Integer domainId;
}
