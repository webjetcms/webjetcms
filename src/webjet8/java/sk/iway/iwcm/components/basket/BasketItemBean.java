package sk.iway.iwcm.components.basket;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
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
import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

/**
 *  BasketItemBean.java - Polozka nakupneho kosika
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: murbanec $
 *@version      $Revision: 1.5 $
 *@created      Date: 9.11.2005 15:06:05
 *@modified     $Date: 2009/11/25 16:34:07 $
 */

@Entity
@Table(name="basket_item")
public class BasketItemBean implements Serializable
{
	private static final long serialVersionUID = 1281182025300889704L;

	private transient DocDetails doc;

	@Id
	@Column(name="basket_item_id")
	@GeneratedValue(generator="WJGen_basket_item")
   @TableGenerator(name="WJGen_basket_item", pkColumnValue="basket_item")
	private int basketItemId;

	@Column(name="browser_id")
	private Long browserId;

	@Column(name="logged_user_id")
	private int loggedUserId;

	@Column(name="item_id")
	private int itemId;

	@Column(name="item_title")
	private String itemTitle;

	@Column(name="item_part_no")
	private String itemPartNo;

	@Column(name="item_price")
	private Double itemPrice;

	@Column(name="item_vat")
	private Double itemVat;

	@Column(name="item_qty")
	private Integer itemQty;

	@Column(name="item_note")
	private String itemNote;

	@Column(name="date_insert")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateInsert;

	@ManyToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="basket_invoice_id")
	BasketInvoiceBean basketInvoice;

	@Column(name="domain_id")
	private int domainId;

	public double getItemPriceQty()
	{
		return(getItemQty().intValue() * getItemPrice().doubleValue());
	}

	/**
	 * Vrati cenu jednej polozky vratane DPH
	 * @return
	 */
	public double getItemPriceVat()
	{
		//TODO: vypocet DPH
		double vat = getItemVat().doubleValue();
		vat = (vat / 100) + 1;

		return(getItemPrice().doubleValue() * vat);
	}

	public boolean itemAlreadyPurchased()
	{
		return getBasketItemId() > 0;
	}

	/**
	 * Vrati cenu vratane DPH * pocet poloziek
	 * @return
	 */
	public double getItemPriceVatQty()
	{
		return(getItemQty().intValue() * getItemPriceVat());
	}

	public String getTitle()
	{
		if (itemAlreadyPurchased())
			return getItemTitle();
		return(getDoc().getTitle());
	}

	public synchronized DocDetails getDoc()
	{
		if (doc == null)
		{
			DocDB docDB = DocDB.getInstance();
			doc = docDB.getDoc(getItemId());
		}
		return(doc);
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

	public double getLocalPrice(HttpServletRequest request, String currency)
	{
		if (itemAlreadyPurchased())
			return getItemPrice();
		return getDoc().getLocalPriceDouble(request,currency);
	}

	public double getLocalPrice(HttpServletRequest request)
	{
		if (itemAlreadyPurchased())
			return getItemPrice();
		return getDoc().getLocalPriceDouble(request);
	}

	public double getItemLocalPriceQty(HttpServletRequest request, String currency)
	{
		return getItemQty()*getLocalPrice(request, currency);
	}

	public double getItemLocalPriceQty(HttpServletRequest request)
	{
		return getItemQty()*getLocalPrice(request);
	}

	public double getItemLocalPriceVatQty(HttpServletRequest request, String currency)
	{
		double vat = getItemVat().doubleValue();
		vat = (vat / 100) + 1;
		return vat*getItemQty()*getLocalPrice(request, currency);
	}

	public double getLocalPriceVat(HttpServletRequest request, String currency)
	{
		double vat = getItemVat().doubleValue();
		vat = (vat / 100.0) + 1.0;
		return vat*getLocalPrice(request, currency);
	}

	public double getLocalPriceVat(HttpServletRequest request)
	{
		double vat = getItemVat().doubleValue();
		vat = (vat / 100.0) + 1.0;
		return vat*getLocalPrice(request);
	}

	public double getItemLocalPriceVatQty(HttpServletRequest request)
	{
		return getItemLocalPriceVatQty(request, BasketDB.getDisplayCurrency(request));
	}



	/**
	 * ------- getters & setters
	 *
	 *
	 */

	/**
	 *	Vrati identifikator faktury, ku ktorej patri nakupna polozka
	 */
	public int getBasketInvoiceId()
	{
		if (basketInvoice == null)
			return 0;
		return basketInvoice.getBasketInvoiceId();
	}

	public int getBasketItemId()
	{
		return basketItemId;
	}

	public void setBasketItemId(int basketItemId)
	{
		this.basketItemId = basketItemId;
	}

	public Long getBrowserId()
	{
		return browserId;
	}

	public void setBrowserId(Long browserId)
	{
		this.browserId = browserId;
	}

	public int getLoggedUserId()
	{
		return loggedUserId;
	}

	public void setLoggedUserId(int loggedUserId)
	{
		this.loggedUserId = loggedUserId;
	}

	public int getItemId()
	{
		return itemId;
	}

	public void setItemId(int itemId)
	{
		this.itemId = itemId;
	}

	public String getItemTitle()
	{
		return itemTitle;
	}

	public void setItemTitle(String itemTitle)
	{
		this.itemTitle = itemTitle;
	}

	public String getItemPartNo()
	{
		return itemPartNo;
	}

	public void setItemPartNo(String itemPartNo)
	{
		this.itemPartNo = itemPartNo;
	}

	public Double getItemPrice()
	{
		return itemPrice;
	}

	public void setItemPrice(Double itemPrice)
	{
		this.itemPrice = itemPrice;
	}

	public Double getItemVat()
	{
		return itemVat;
	}

	public void setItemVat(Double itemVat)
	{
		this.itemVat = itemVat;
	}

	public Integer getItemQty()
	{
		return itemQty;
	}

	public void setItemQty(Integer itemQty)
	{
		this.itemQty = itemQty;
	}

	public String getItemNote()
	{
		return itemNote;
	}

	public void setItemNote(String itemNote)
	{
		this.itemNote = itemNote;
	}

	public Date getDateInsert()
	{
		return dateInsert == null ? null : (Date) dateInsert.clone();
	}

	public void setDateInsert(Date dateInsert)
	{
		this.dateInsert = dateInsert == null ? null : (Date) dateInsert.clone();
	}

	public BasketInvoiceBean getBasketInvoice()
	{
		return basketInvoice;
	}

	public void setBasketInvoice(BasketInvoiceBean basketInvoice)
	{
		this.basketInvoice = basketInvoice;
	}


	@Override
	public String toString()
	{
		return String.format("%dx%s", getItemQty(), getItemTitle());
	}

	//public int getDomainId() {
	//	return domainId;
	//}

	//public void setDomainId(int domainId) {
	//	this.domainId = domainId;
	//}

	/**
	 * Vrati aktualnu domainId
	 * @return
	 */
	public int getDomainId() {
		return domainId;
		//return CloudToolsForCore.getDomainId();
	}

	/**
	 * Force nastavi objektu domainId aktualnej domeny
	 *
	 */
	public void setDomainId(int domainId) {
		//this.domainId = domainId;
		this.domainId = CloudToolsForCore.getDomainId();
	}

}