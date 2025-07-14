package sk.iway.iwcm.components.basket.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name="basket_invoice")
@Getter
@Setter
public class BasketInvoiceEntity implements Serializable {

	public static final Integer INVOICE_STATUS_NEW = Integer.valueOf(1);
	public static final Integer INVOICE_STATUS_PAID = Integer.valueOf(2);
	public static final Integer INVOICE_STATUS_CANCELLED = Integer.valueOf(3);
	public static final Integer INVOICE_STATUS_PARTIALLY_PAID = Integer.valueOf(4);
	public static final Integer INVOICE_STATUS_ISSUED = Integer.valueOf(5);
	public static final Integer INVOICE_STATUS_DEPOSIT_PAID = Integer.valueOf(8);

    @Id
	@Column(name="basket_invoice_id")
	@GeneratedValue(generator="WJGen_basket_invoice")
    @TableGenerator(name="WJGen_basket_invoice", pkColumnValue="basket_invoice")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="components.basket.invoice.number")
	private Long id;

	@Column(name="delivery_name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="[[#{components.basket.invoice.name}]]",
		hiddenEditor = true
    )
	private String deliveryName;

    @Column(name="create_date")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="[[#{components.basket.invoice.date_created}]]",
		tab = "basic",
		editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
	private Date createDate;

    @Column(name="status_id")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.basket.invoice.state}]]",
		tab = "basic"
    )
	private Integer statusId;

	@Lob
	@Column(name="user_note")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="[[#{components.basket.invoice.note}]]",
		hidden = true,
		tab = "basic"
    )
	private String userNote;

	@Column(name="contact_email")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="[[#{components.qa.add_action.recipient}]]",
		tab = "notify",
		sortAfter = "editorFields.sender",
		hidden = true
    )
	private String contactEmail;

    @Column(name="payment_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.basket.invoice.payment_method}]]",
		hiddenEditor = true,
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

    @Column(name="delivery_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.basket.mode_of_transport}]]",
		hiddenEditor = true,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_personally", value = "[[#{components.basket.order_form.delivery_personally}]]"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_post", value = "[[#{components.basket.order_form.delivery_post}]]"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_courier", value = "[[#{components.basket.order_form.delivery_courier}]]"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery", value = "[[#{components.basket.order_form.delivery}]]")
				}
			)
		}
    )
	private String deliveryMethod;

	//@JsonManagedReference(value="itemsBasketInvoice")
	@OneToMany(mappedBy="itemsBasketInvoice", fetch=FetchType.LAZY, cascade={CascadeType.ALL})
	private List<BasketInvoiceItemEntity> basketItems;

	//@JsonManagedReference(value="paymentsBasketInvoice")
	@OneToMany(mappedBy="paymentsBasketInvoice", fetch=FetchType.LAZY, cascade={CascadeType.ALL})
	private List<BasketInvoicePaymentEntity> basketPayments;

	@Transient
	@DataTableColumnNested
	private transient BasketInvoiceEditorFields editorFields = null;

	@Column(name="currency")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice.currency", hiddenEditor = true)
	private String currency;

	//Need for repository
	@Column(name="logged_user_id")
	private int loggedUserId;

	@Column(name="domain_id")
	private int domainId;

	// @Column(name="browser_id")
	// private Long browserId;

	// @Column(name="delivery_company")
	// private String deliveryCompany;

	// @Column(name="delivery_surname")
	// private String deliverySurName;
	// @Column(name="delivery_street")
	// private String deliveryStreet;
	// @Column(name="delivery_city")
	// private String deliveryCity;
	// @Column(name="delivery_zip")
	// private String deliveryZip;
	// @Column(name="delivery_country")
	// private String deliveryCountry;

	// @Column(name="internal_invoice_id")
	// private String internalInvoiceId;

	// @Column(name="user_lng")
	// private String userLng;

	// @Column(name="contact_title")
	// private String contactTitle;
	// @Column(name="contact_first_name")
	// private String contactFirstName;
	// @Column(name="contact_last_name")
	// private String contactLastName;

	// @Column(name="contact_phone")
	// private String contactPhone;
	// @Column(name="contact_company")
	// private String contactCompany;
	// @Column(name="contact_street")
	// private String contactStreet;
	// @Column(name="contact_city")
	// private String contactCity;
	// @Column(name="contact_zip")
	// private String contactZip;
	// @Column(name="contact_country")
	// private String contactCountry;
	// @Column(name="contact_ico")
	// private String contactIco;
	// @Column(name="contact_icdph")
	// private String contactIcdph;
	// @Column(name="contact_dic")
	// private String contactDic;

	// @Column(name="field_a")
	// private String fieldA;
	// @Column(name="field_b")
	// private String fieldB;
	// @Column(name="field_c")
	// private String fieldC;
	// @Column(name="field_d")
	// private String fieldD;
	// @Column(name="field_e")
	// private String fieldE;
	// @Column(name="field_f")
	// private String fieldF;

	// @Column(name="html_code")
	// @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	// private String htmlCode;

	/**
	 * Vrati celkovy pocet poloziek (vratane QTY)
	 * @return
	 */
	public int getTotalItems() {
		return getBasketItems().stream().mapToInt(item -> item.getItemQty().intValue()).sum();
	}

	/**
	 * Vrati celkovu cenu poloziek vratane DPH
	 * @return
	 */
	public BigDecimal getTotalPriceVat() {
		return getBasketItems().stream().map(item -> item.getItemPriceVatQty())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalPayedPrice() {
		return getBasketPayments().stream().map(item -> item.getPayedPrice())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalPriceVatIn(String currency) {
		try {
			String constantName = "kurz_" + currency + "_" + getCurrency();
			BigDecimal rate;

			// nasli sme bezny kurz
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return rate.multiply( getTotalPriceVat() );
			}

			// nevyslo, skusime opacnu konverziu
			constantName = "kurz_" + getCurrency() + "_" + currency;

			// podobne, ako hore, ale kedze ide o opacny kurz, musime spravit
			// 1/kurz
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return (BigDecimal.valueOf(1).divide(rate)).multiply( getTotalPriceVat() );
			}
		} catch (NumberFormatException e) {
			sk.iway.iwcm.Logger.error(e);
			throw new IllegalStateException("Malformed constant format for currencies " + getCurrency() + " and " + currency);
		}

		return getTotalPriceVat();
	}

	/**
	 * Vypocita autorizacny token k objednavke. Ako autorizacny token
	 * sa vezme retazec "INV"+id objednavky+Constants.getInstallName()
	 *
	 * Ak by sa tento prefix/sufix nepouzil, utocnik by ziskal pristup k {@link Password}.encrypt(ID_OBJEDNAVKY),
	 * co by potencialne mohol vyuzit pri inych autorizacnych testoch.
	 * @throws Exception ak sa nepodari enkrypcia
	 */
	public String getAuthorizationToken() {
		return getAuthorizationToken(getBasketInvoiceId());
	}

	public static String getAuthorizationToken(int invoiceId) {
		String rawKey = new StringBuilder().append("INV").append(invoiceId).append(Constants.getInstallName()).toString();
		try {
			return new Password().encrypt(rawKey);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
			return "";
		}
	}

	public int getBasketInvoiceId() {
		return id.intValue();
	}
}
