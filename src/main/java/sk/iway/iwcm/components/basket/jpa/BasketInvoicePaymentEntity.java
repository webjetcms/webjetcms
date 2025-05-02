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
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

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

    @Column(name="invoice_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    Long invoiceId;

    @Column(name="payment_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.basket.invoice.payment_method"
    )
    private String paymentMethod;

    @Column(name="payed_price")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.admin_invoices_detail.suma"
    )
    private BigDecimal payedPrice;

    @Column(name="confirmed")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.basket.invoice_payments.confirmed"
    )
    private Boolean confirmed;

    @Column(name="payment_status")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.basket.invoice_payments.status.title",
        hiddenEditor = true
    )
    private Integer paymentStatus;

    @Column(name="create_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.basket.invoice.date_created"
    )
    private Date createDate;

    @Column(name="payment_description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.basket.invoice_payments.description",
        className = "wrap"
    )
    @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String paymentDescription;

    @Column(name="closed_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.basket.close_date",
        className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private Date closedDate;

    @Column(name="real_payment_id")
    private String realPaymentId;

    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonBackReference(value="paymentsBasketInvoice")
    @JoinColumn(name="invoice_id", insertable = false, updatable = false)
    BasketInvoiceEntity paymentsBasketInvoice;

    @Transient
	@DataTableColumnNested
	private transient BasketInvoicePaymentEditorFields editorFields = null;

    public int getItemId() {
        return this.id == null ? -1 : this.id.intValue();
    }
}