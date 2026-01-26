package sk.iway.iwcm.components.basket.payment_methods.jpa;

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
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.components.basket.support.SupportMethodEntity;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "basket_payment_methods")
@Getter
@Setter
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_BASKET_UPDATE)
public class PaymentMethodEntity extends SupportMethodEntity {

    @Id //We do not use this ID, it's here just because it must be here
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_basket_payment_methods")
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

    @Column(name = "sort_priority")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "editor.sort_order",
		visible = false,
		editor = {
			@DataTableColumnEditor(
				message = "components.invoice_payment.sort_help",
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
				}
			)
		}
	)
	private Integer sortPriority = 0;

    @Transient //JUST to show status of method
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="apps.eshop.payments.status",
        hiddenEditor = true
    )
    private String status;

    @Transient
	@DataTableColumnNested
	private transient BaseEditorFields editorFields = null;

    //DOMAIN
    @Column(name = "domain_id")
    private Integer domainId;
}
