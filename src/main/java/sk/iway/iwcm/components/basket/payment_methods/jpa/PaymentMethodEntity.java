package sk.iway.iwcm.components.basket.payment_methods.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
public class PaymentMethodEntity {

    @Id //We do not use this ID, it's here just because it must be here
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    //For us, this is REAL ID, in DB set as UNIQUE
    @Column(name = "payment_method_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.basket.invoice.payment_method",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                }
            )
        }
    )
    @Size(max = 255)
    private String paymentMethodName;

    @Transient //JUST to show status of method
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="apps.eshop.payments.status",
        hiddenEditor = true
    )
    private String status;

    @Column(name = "field_a")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldA;

    @Column(name = "field_b")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldB;

    @Column(name = "field_c")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldC;

    @Column(name = "field_d")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldD;

    @Column(name = "field_e")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldE;

    @Column(name = "field_f")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldF;

    @Column(name = "field_g")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldG;

    @Column(name = "field_h")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldH;

    @Column(name = "field_i")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldI;

    @Column(name = "field_j")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldJ;

    @Column(name = "field_k")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldK;

    @Column(name = "field_l")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    private String fieldL;

    @Transient
	@DataTableColumnNested
	private transient BaseEditorFields editorFields = null;

    //DOMAIN
    @Column(name = "domain_id")
    private Integer domainId;
}
