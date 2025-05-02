package sk.iway.iwcm.components.basket.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.rest.ProductListService;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name="basket_invoice")
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_BASKET_UPDATE)
@Getter
@Setter
public class BasketInvoiceEntity extends ActiveRecordRepository implements Serializable {

	@PrePersist
	public void onPrePersist() {
		//After insert update invoice stats
		ProductListService.updateInvoiceStats(this.getId(), this.browserId, true);
	}

    @Id
	@Column(name="basket_invoice_id")
	@GeneratedValue(generator="WJGen_basket_invoice")
    @TableGenerator(name="WJGen_basket_invoice", pkColumnValue="basket_invoice")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="components.basket.invoice.number")
	private Long id;

	@Column(name="contact_first_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice.name",
		tab = "personal_info",
		hidden = true,
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.basket.invoice_email.contact")
				}
			)
		}
    )
	private String contactFirstName;

	@Column(name="contact_last_name")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice_email.surname",
		tab = "personal_info",
		hidden = true
    )
	private String contactLastName;

    @Column(name="create_date")
	@Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.basket.invoice.date_created",
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
        title="components.basket.invoice.state",
		tab = "basic"
    )
	private Integer statusId;

	@Column(name="user_note")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.basket.invoice.note",
		hidden = true,
		tab = "basic"
    )
	private String userNote;

    @Column(name="payment_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.basket.invoice.payment_method",
		hiddenEditor = true
    )
	private String paymentMethod;

    @Column(name="delivery_method")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.basket.mode_of_transport",
		hiddenEditor = true,
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_personally", value = "components.basket.order_form.delivery_personally"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_post", value = "components.basket.order_form.delivery_post"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery_courier", value = "components.basket.order_form.delivery_courier"),
					@DataTableColumnEditorAttr(key = "components.basket.order_form.delivery", value = "components.basket.order_form.delivery")
				}
			)
		}
    )
	private String deliveryMethod;

	//Need for repository
	@Column(name="logged_user_id")
	@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	private int loggedUserId;

	@Column(name="domain_id")
	private int domainId;

	/****** INFO ******/
	@Column(name="item_qty")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
		title = "components.basket.total_items",
		hiddenEditor = true
    )
	private Integer itemQty;

	@Column(name="price_no_vat")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
		renderFormat = "dt-format-number--decimal",
		title = "components.basket.price_without_DPH",
		hiddenEditor = true
    )
	private BigDecimal priceToPayNoVat;

	@Column(name="price_vat")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
		renderFormat = "dt-format-number--decimal",
		title = "components.basket.price_with_DPH",
		hiddenEditor = true
    )
	private BigDecimal priceToPayVat;

	@Column(name="balance_to_pay")
	@DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
		renderFormat = "dt-format-number--decimal",
		title = "components.basket.invoice.items.items_price.js",
		hiddenEditor = true
    )
	private BigDecimal balanceToPay;

	@Column(name="currency")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice.currency", hiddenEditor = true)
	private String currency;

	/****** CONTACT ******/
	@Column(name="contact_title")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title="components.basket.price_list.product_title",
		tab = "personal_info",
		visible = false
	)
	private String contactTitle;

	@Column(name="contact_email")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice.email",
		tab = "personal_info",
		visible = false
    )
	private String contactEmail;

	@Column(name="contact_phone")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.phone_number", tab = "personal_info", visible = false)
	private String contactPhone;

	@Column(name="user_lng")
	private String userLng;

	/****** CONTACT ADRESS ******/
	@Column(name="contact_street")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.street", tab = "personal_info", visible = false,
	editor = {
		@DataTableColumnEditor(
			attr = {
				@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
				@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.invoice.invoice_adress")
			}
		)
	})
	private String contactStreet;

	@Column(name="contact_city")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.city", tab = "personal_info", visible = false)
	private String contactCity;

	@Column(name="contact_zip")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.ZIP", tab = "personal_info", visible = false)
	private String contactZip;

	@Column(name="contact_country")
	@DataTableColumn(inputType = DataTableColumnType.SELECT, title="components.basket.invoice_email.country", tab = "personal_info", visible = false)
	private String contactCountry;

	/****** CONTACT COMPANY ******/
	@Column(name="contact_company")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.company", tab = "personal_info", visible = false,
	editor = {
		@DataTableColumnEditor(
			attr = {
				@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
				@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.invoice.company_info")
			}
		)
	})
	private String contactCompany;

	@Column(name="contact_ico")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.ico", tab = "personal_info", visible = false)
	private String contactIco;

	@Column(name="contact_dic")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.vatid", tab = "personal_info", visible = false)
	private String contactDic;

	/****** DELIVERY (different from contact) ******/
	@Column(name="delivery_name")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice.name", tab = "personal_info", hidden = true,
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.basket.delivery_address_title")
				}
			)
		}
	)
	private String deliveryName;

	@Column(name="delivery_surname")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.surname", tab = "personal_info", hidden = true)
	private String deliverySurName;

	@Column(name="delivery_street")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.street", tab = "personal_info", visible = false)
	private String deliveryStreet;

	@Column(name="delivery_city")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.city", tab = "personal_info", visible = false)
	private String deliveryCity;

	@Column(name="delivery_zip")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.ZIP", tab = "personal_info", visible = false)
	private String deliveryZip;

	@Column(name="delivery_country")
	@DataTableColumn(inputType = DataTableColumnType.SELECT, title="components.basket.invoice_email.country", tab = "personal_info", visible = false)
	private String deliveryCountry;

	@Column(name="delivery_company")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.basket.invoice_email.company", tab = "personal_info", visible = false)
	private String deliveryCompany;

	/****** FIELDS ******/
	@Column(name="field_a")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_a",
		visible = false,
		tab = "fields"
    )
	private String fieldA;

	@Column(name="field_b")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_b",
		visible = false,
		tab = "fields"
    )
	private String fieldB;

	@Column(name="field_c")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_c",
		visible = false,
		tab = "fields"
    )
	private String fieldC;

	@Column(name="field_d")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_d",
		visible = false,
		tab = "fields"
    )
	private String fieldD;

	@Column(name="field_e")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_e",
		visible = false,
		tab = "fields"
    )
	private String fieldE;

	@Column(name="field_f")
	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
		title = "components.invoice.field_f",
		visible = false,
		tab = "fields"
    )
	private String fieldF;

	@Transient
	@DataTableColumnNested
	private transient BasketInvoiceEditorFields editorFields = null;

	@Column(name="browser_id")
	private Long browserId;

	// @Column(name="contact_icdph")
	// @DataTableColumn(inputType = DataTableColumnType.TEXT, title="", tab = "personal_info", visible = false)
	// private String contactIcdph;

	// @Column(name="internal_invoice_id")
	// private String internalInvoiceId;

	// @Column(name="user_lng")
	// private String userLng;

	// @Column(name="html_code")
	// @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	// private String htmlCode;

	public BigDecimal getTotalPriceVatIn(String currency) {
		try {
			String constantName = "kurz_" + currency + "_" + getCurrency();
			BigDecimal rate;

			// nasli sme bezny kurz
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return rate.multiply( getPriceToPayVat() );
			}

			// nevyslo, skusime opacnu konverziu
			constantName = "kurz_" + getCurrency() + "_" + currency;

			// podobne, ako hore, ale kedze ide o opacny kurz, musime spravit
			// 1/kurz
			if (Tools.isNotEmpty(Constants.getString(constantName))) {
				rate = new BigDecimal( Constants.getString(constantName) );
				return (BigDecimal.valueOf(1).divide(rate)).multiply( getPriceToPayVat() );
			}
		} catch (NumberFormatException e) {
			sk.iway.iwcm.Logger.error(e);
			throw new IllegalStateException("Malformed constant format for currencies " + getCurrency() + " and " + currency);
		}

		return getPriceToPayVat();
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
		return this.id == null ? -1 : this.id.intValue();
	}

	public List<BasketInvoiceItemEntity> getBasketItems() {
		BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);
		return biir.findAllByBrowserIdAndDomainId(browserId, domainId);
	}


	//
	public BigDecimal getTotalPriceVat() {
		return priceToPayVat == null ? BigDecimal.ZERO : priceToPayVat;
	}

	public BigDecimal getTotalPrice() {
		return priceToPayNoVat == null ? BigDecimal.ZERO : priceToPayNoVat;
	}

	public int getInvoiceId() {
		return this.id == null ? -1 : this.id.intValue();
	}
	public void setInvoiceId(int invoiceId) {
		this.id = Long.valueOf(invoiceId);
	}
}