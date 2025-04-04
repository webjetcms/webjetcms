package sk.iway.iwcm.components.basket.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name="basket_invoice_payments")
@Getter
@Setter
public class BasketInvoicePaymentEntity implements Serializable {

    @Id
	@Column(name="payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator="S_basket_invoice_payments")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@JsonBackReference(value="paymentsBasketInvoice")
	@JoinColumn(name="invoice_id", insertable = false, updatable = false)
	BasketInvoiceEntity paymentsBasketInvoice;

    @Column(name="invoice_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    Long invoiceId;

    @Column(name="create_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.basket.invoice.date"
    )
	private Date createDate;

    @Column(name="payment_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.basket.invoice.payment_method}]]",
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.basket.order_form.cash_on_delivery", value = "cash_on_delivery"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.money_transfer", value = "money_transfer")
				}
			)
		}
    )
	private String paymentMethod;

    @Column(name="payed_price")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.admin_invoices_detail.suma"
    )
	private BigDecimal payedPrice;


    @Column(name="closed_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="",
        visible = false,
        hidden = true,
        hiddenEditor = true
    )
	private Date closedDate;

    @Column(name="confirmed")
	private Boolean confirmed;
}
