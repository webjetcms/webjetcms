package sk.iway.iwcm.components.multistep_form.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "form_items_conditions")
@Getter
@Setter
public class FormItemsConditionEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_form_items_conditions")
    @DataTableColumn(inputType = DataTableColumnType.ID, hidden = true)
    private Long id;

    // Used as fast reference to get all form items conditions
    @Column(name = "form_name")
    private String formName;

    @Column(name = "sort_priority")
    @DataTableColumn(inputType = DataTableColumnType.ROW_REORDER, title = "editor.sort_order", filter = false)
	private Integer sortPriority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type")
    private ConditionType conditionType;

    // item they are bound to
    @Column(name = "form_item_id")
    private Long formItemId;

    @Column(name = "item_form_id")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.form_items_condition.item_id")
    private String itemFormId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.form_items_condition.operator")
    private OperatorType operator;

    @Column(name = "value")
    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.form_items_condition.value")
    private String value;

    @Column(name = "case_insensitive")
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT)
    private Boolean caseInsensitive;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_operator_type")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.form_items_condition.join_operator")
    private JoinOperatorType joinOperatorType;

    @Column(name = "domain_id")
    private Integer domainId;
}
