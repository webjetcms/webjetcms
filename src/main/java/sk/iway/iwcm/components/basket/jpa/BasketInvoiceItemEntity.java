package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.basket.rest.EshopService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name="basket_item")
@Getter
@Setter
public class BasketInvoiceItemEntity {

	@Transient
    private DocDetails doc = null;

	@Id
	@Column(name="basket_item_id")
	@GeneratedValue(generator="WJGen_basket_item")
    @TableGenerator(name="WJGen_basket_item", pkColumnValue="basket_item")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	Long id;

	@ManyToOne
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@JsonBackReference(value="itemsBasketInvoice")
	@JoinColumn(name="basket_invoice_id", insertable = false, updatable = false)
	BasketInvoiceEntity itemsBasketInvoice;

	@Column(name="basket_invoice_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    Integer invoiceId;

	@Column(name="item_title")
	@DataTableColumn(
		inputType = DataTableColumnType.OPEN_EDITOR,
		title = "components.basket.item_name",
				editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
	)
	private String itemTitle;

	@Column(name="item_price")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		renderFormat = "dt-format-number--decimal",
		title = "components.basket.price_without_DPH"
	)
	private BigDecimal itemPrice;

	@Column(name="item_qty")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "components.basket.count"
	)
	@Min(1)
	private Integer itemQty;

	@Column(name="item_note")
	@DataTableColumn(
		inputType = DataTableColumnType.TEXTAREA,
		title = "components.basket.item_note"
	)
	private String itemNote;

	@Column(name="item_id")
	@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	private Integer itemId;

	@Column(name="item_vat")
	private Integer itemVat;

	@Column(name="browser_id")
	private Long browserId;

	@Column(name="logged_user_id")
	@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
	private int loggedUserId;

	@Column(name="item_part_no")
	private String itemPartNo;

	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateInsert;

	@Column(name="domain_id")
	private int domainId;

	@Transient
	@DataTableColumnNested
	private BasketInvoiceItemEditorFields editorFields = null;

	/**
	 * Return item price without VAT, multiplied by item quantity
	 * @return
	 */
	public BigDecimal getItemPriceQty() {
		return BigDecimal.valueOf(getItemQty()).multiply(itemPrice);
	}

	public int getBasketItemId() {
		return getId() == null ? 0 : getId().intValue();
	}

    /**
	 * Vrati cenu jednej polozky vratane DPH
	 * @return
	 */
	public BigDecimal getItemPriceVat() {
		//vypocet DPH (aka VAT)
		BigDecimal vat = BigDecimal.valueOf( getItemVat() );
		vat = ( vat.divide(BigDecimal.valueOf(100)) ).add(BigDecimal.valueOf(1));
		return getItemPrice().multiply(vat);
	}

	public boolean itemAlreadyPurchased() {
		return (getInvoiceId() != null && getInvoiceId() > 0);
	}

	/**
	 * Vrati cenu vratane DPH * pocet poloziek
	 * @return
	 */
	public BigDecimal getItemPriceVatQty() {
		return getItemPriceVat().multiply( BigDecimal.valueOf(getItemQty()) );
	}

	public String getTitle() {
		if (itemAlreadyPurchased())
			return getItemTitle();
		return(getDoc().getTitle());
	}

	public synchronized DocDetails getDoc() {
		if (doc == null) {
			DocDB docDB = DocDB.getInstance();
			doc = docDB.getDoc(getItemIdInt());
		}
		return doc;
	}

	//------------------------PREPOCITAVANIE NA LOKALNE CENY POUZIVATELA-----------------------
	/**
	 * Metody na prepocitavanie cien do inych mien. Metody sa vyskytuju v roznych obmenach:
	 * getLocalPrice - vypocita cenu bez DPH
	 * getItemLocalPriceQty - vypocita cenu bez DPH krat pocet poloziek v kosiku
	 * getLocalPriceVat - vypocita cenu aj s DPH
	 * getItemLocalPriceVatQty - vypocita cenu s DPH krat pocet poloziek v kosiku
	 *
	 * Tieto metody sa vyskytuju v dvoch obmenach - s a bez zadanej meny. Ak sa mena nezada,
	 * vezme sa vychodzia mena pouzivatela.
	 */

	public BigDecimal getLocalPrice(HttpServletRequest request, String currency) {
		if (itemAlreadyPurchased())
			return getItemPrice();
		return getDoc().getLocalPrice(request, currency);
	}

	public BigDecimal getLocalPrice(HttpServletRequest request) {
		if (itemAlreadyPurchased())
			return getItemPrice();
		return getDoc().getLocalPrice(request);
	}

	public BigDecimal getItemLocalPriceQty(HttpServletRequest request, String currency) {
		return BigDecimal.valueOf(getItemQty()).multiply( getLocalPrice(request, currency) );
	}

	public BigDecimal getItemLocalPriceQty(HttpServletRequest request) {
		return BigDecimal.valueOf(getItemQty()).multiply( getLocalPrice(request) );
	}

	public BigDecimal getItemLocalPriceVatQty(HttpServletRequest request, String currency) {
		BigDecimal vat = BigDecimal.valueOf(getItemVat());
		vat = (vat.divide(BigDecimal.valueOf(100))).add(BigDecimal.valueOf(1));
		return vat.multiply(BigDecimal.valueOf(getItemQty())).multiply(getLocalPrice(request, currency));
	}

	public BigDecimal getLocalPriceVat(HttpServletRequest request, String currency) {
		BigDecimal vat = BigDecimal.valueOf(getItemVat());
		vat = (vat.divide(BigDecimal.valueOf(100))).add(BigDecimal.valueOf(1));
		return vat.multiply( getLocalPrice(request, currency) );
	}

	public BigDecimal getLocalPriceVat(HttpServletRequest request) {
		BigDecimal vat = BigDecimal.valueOf(getItemVat());
		vat = (vat.divide(BigDecimal.valueOf(100))).add(BigDecimal.valueOf(1));
		return vat.multiply( getLocalPrice(request) );
	}

	public BigDecimal getItemLocalPriceVatQty(HttpServletRequest request) {
		return getItemLocalPriceVatQty(request, EshopService.getDisplayCurrency(request));
	}

	public Date getDateInsert() {
		return dateInsert == null ? null : (Date) dateInsert.clone();
	}

	public void setDateInsert(Date dateInsert) {
		this.dateInsert = dateInsert == null ? null : (Date) dateInsert.clone();
	}

	public int getItemIdInt() {
		return getItemId() == null ? -1 : getItemId().intValue();
	}

	@Override
	public String toString() {
		return String.format("%dx%s", getItemQty(), getItemTitle());
	}
}