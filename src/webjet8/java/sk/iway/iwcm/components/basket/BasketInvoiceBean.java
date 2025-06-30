package sk.iway.iwcm.components.basket;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

/**
 *  BasketInvoiceBean.java - objednavka
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff $
 *@version      $Revision: 1.10 $
 *@created      Date: 9.11.2005 15:07:39
 *@modified     $Date: 2010/01/20 11:14:06 $
 */
@Entity
@Table(name="basket_invoice")
public class BasketInvoiceBean implements Serializable
{
	private static final long serialVersionUID = -2743373083798323795L;

	public static final Integer INVOICE_STATUS_NEW = Integer.valueOf(1);
	public static final Integer INVOICE_STATUS_PAID = Integer.valueOf(2);
	public static final Integer INVOICE_STATUS_CANCELLED = Integer.valueOf(3);
	public static final Integer INVOICE_STATUS_DEPOSIT_PAID = Integer.valueOf(8);
	public static final Integer INVOICE_STATUS_PARTIALLY_PAID = Integer.valueOf(4);
	public static final Integer INVOICE_STATUS_ISSUED = Integer.valueOf(5);


	@Id
	@Column(name="basket_invoice_id")
	@GeneratedValue(generator="WJGen_basket_invoice")
   @TableGenerator(name="WJGen_basket_invoice", pkColumnValue="basket_invoice")
	private int basketInvoiceId;

	@Column(name="browser_id")
	private Long browserId;

	@Column(name="logged_user_id")
	private int loggedUserId;

	@Column(name="create_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createDate;

	@Column(name="status_id")
	private Integer statusId;

	@Column(name="delivery_company")
	private String deliveryCompany;
	@Column(name="delivery_name")
	private String deliveryName;
	@Column(name="delivery_surname")
	private String deliverySurName;
	@Column(name="delivery_street")
	private String deliveryStreet;
	@Column(name="delivery_city")
	private String deliveryCity;
	@Column(name="delivery_zip")
	private String deliveryZip;
	@Column(name="delivery_country")
	private String deliveryCountry;

	@Column(name="internal_invoice_id")
	private String internalInvoiceId;
	@Column(name="user_note")
	private String userNote;
	@Column(name="user_lng")
	private String userLng;

	@Column(name="payment_method")
	private String paymentMethod;
	@Column(name="delivery_method")
	private String deliveryMethod;

	@Column(name="contact_title")
	private String contactTitle;
	@Column(name="contact_first_name")
	private String contactFirstName;
	@Column(name="contact_last_name")
	private String contactLastName;
	@Column(name="contact_email")
	private String contactEmail;
	@Column(name="contact_phone")
	private String contactPhone;
	@Column(name="contact_company")
	private String contactCompany;
	@Column(name="contact_street")
	private String contactStreet;
	@Column(name="contact_city")
	private String contactCity;
	@Column(name="contact_zip")
	private String contactZip;
	@Column(name="contact_country")
	private String contactCountry;
	@Column(name="contact_ico")
	private String contactIco;
	@Column(name="contact_icdph")
	private String contactIcdph;
	@Column(name="contact_dic")
	private String contactDic;

	@Column(name="field_a")
	private String fieldA;
	@Column(name="field_b")
	private String fieldB;
	@Column(name="field_c")
	private String fieldC;
	@Column(name="field_d")
	private String fieldD;
	@Column(name="field_e")
	private String fieldE;
	@Column(name="field_f")
	private String fieldF;

	@Lob
	@Column(name="html_code")
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String htmlCode;

	@Column(name="currency")
	private String currency;


	@javax.persistence.OneToMany(cascade=CascadeType.ALL, targetEntity=BasketItemBean.class, mappedBy="basketInvoice")
	private List<BasketItemBean> basketItems;// = new ArrayList<BasketItemBean>();

	@Column(name="domain_id")
	private int domainId;


	/**
	 * Vrati celkovy pocet poloziek (vratane QTY)
	 * @return
	 */
	public int getTotalItems()
	{
		return(InvoiceDB.getTotalItems(getBasketItems()));
	}

	/**
	 * Vrati celkovu cenu poloziek vratane DPH
	 * @return
	 */
	public double getTotalPriceVat()
	{
		return(InvoiceDB.getTotalPriceVat(getBasketItems()));
	}

	public double getTotalPriceVatIn(String currency)
	{

		try
		{
			String constantName = "kurz_" + currency + "_" + getCurrency();
			double rate = 1.0;
			// nasli sme bezny kurz
			if (Tools.isNotEmpty(Constants.getString(constantName)))
			{
				rate = Double.valueOf(Constants.getString(constantName));
				return rate * getTotalPriceVat();
			}
			// nevyslo, skusime opacnu konverziu
			constantName = "kurz_" + getCurrency() + "_" + currency;
			// podobne, ako hore, ale kedze ide o opacny kurz, musime spravit
			// 1/kurz
			if (Tools.isNotEmpty(Constants.getString(constantName)))
			{
				rate = Double.valueOf(Constants.getString(constantName));
				return (1.0 / rate) * getTotalPriceVat();
			}
		}
		catch (NumberFormatException e)
		{
			sk.iway.iwcm.Logger.error(e);
			throw new IllegalStateException("Malformed constant format for currencies "+getCurrency()+" and "+currency);
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
	public String getAuthorizationToken()
	{
		return getAuthorizationToken(getBasketInvoiceId());
	}

	public static String getAuthorizationToken(int invoiceId)
	{
		String rawKey = new StringBuilder().append("INV").append(invoiceId).append(Constants.getInstallName()).toString();
		try
		{
			return new Password().encrypt(rawKey);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return "";
		}
	}


	/**
	 * ------- getters & setters
	 *
	 *
	 */

	public List<BasketItemBean> getBasketItems()
	{
		return basketItems;
	}
	public void setBasketItems(List<BasketItemBean> basketItems)
	{
		this.basketItems = basketItems;
	}
	public void setBasketInvoiceId(int basketInvoiceId)
	{
		this.basketInvoiceId = basketInvoiceId;
	}



	public int getBasketInvoiceId()
	{
		return basketInvoiceId;
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

	public Date getCreateDate()
	{
		return createDate == null ? null : (Date) createDate.clone();
	}

	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate == null ? null : (Date) createDate.clone();
	}

	public Integer getStatusId()
	{
		return statusId;
	}

	public void setStatusId(Integer statusId)
	{
		this.statusId = statusId;
	}

	public String getDeliveryCompany()
	{
		return deliveryCompany;
	}

	public void setDeliveryCompany(String deliveryCompany)
	{
		this.deliveryCompany = deliveryCompany;
	}

	public String getDeliveryName()
	{
		return deliveryName;
	}

	public void setDeliveryName(String deliveryName)
	{
		this.deliveryName = deliveryName;
	}

	public String getDeliverySurName() {
		return deliverySurName;
	}

	public void setDeliverySurName(String deliverySurName) {
		this.deliverySurName = deliverySurName;
	}

	public String getDeliveryStreet()
	{
		return deliveryStreet;
	}

	public void setDeliveryStreet(String deliveryStreet)
	{
		this.deliveryStreet = deliveryStreet;
	}

	public String getDeliveryCity()
	{
		return deliveryCity;
	}

	public void setDeliveryCity(String deliveryCity)
	{
		this.deliveryCity = deliveryCity;
	}

	public String getDeliveryZip()
	{
		return deliveryZip;
	}

	public void setDeliveryZip(String deliveryZip)
	{
		this.deliveryZip = deliveryZip;
	}

	public String getDeliveryCountry()
	{
		return deliveryCountry;
	}

	public void setDeliveryCountry(String deliveryCountry)
	{
		this.deliveryCountry = deliveryCountry;
	}

	public String getInternalInvoiceId()
	{
		return internalInvoiceId;
	}

	public void setInternalInvoiceId(String internalInvoiceId)
	{
		this.internalInvoiceId = internalInvoiceId;
	}

	public String getUserNote()
	{
		return userNote;
	}

	public void setUserNote(String userNote)
	{
		this.userNote = userNote;
	}

	public String getUserLng()
	{
		return userLng;
	}

	public void setUserLng(String userLng)
	{
		this.userLng = userLng;
	}

	public String getPaymentMethod()
	{
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod)
	{
		this.paymentMethod = paymentMethod;
	}

	public String getDeliveryMethod()
	{
		return deliveryMethod;
	}

	public void setDeliveryMethod(String deliveryMethod)
	{
		this.deliveryMethod = deliveryMethod;
	}

	public String getContactTitle()
	{
		return contactTitle;
	}

	public void setContactTitle(String contactTitle)
	{
		this.contactTitle = contactTitle;
	}

	public String getContactFirstName()
	{
		return contactFirstName;
	}

	public void setContactFirstName(String contactFirstName)
	{
		this.contactFirstName = contactFirstName;
	}

	public String getContactLastName()
	{
		return contactLastName;
	}

	public void setContactLastName(String contactLastName)
	{
		this.contactLastName = contactLastName;
	}

	public String getContactEmail()
	{
		return contactEmail;
	}

	public void setContactEmail(String contactEmail)
	{
		this.contactEmail = contactEmail;
	}

	public String getContactPhone()
	{
		return contactPhone;
	}

	public void setContactPhone(String contactPhone)
	{
		this.contactPhone = contactPhone;
	}

	public String getContactCompany()
	{
		return contactCompany;
	}

	public void setContactCompany(String contactCompany)
	{
		this.contactCompany = contactCompany;
	}

	public String getContactStreet()
	{
		return contactStreet;
	}

	public void setContactStreet(String contactStreet)
	{
		this.contactStreet = contactStreet;
	}

	public String getContactCity()
	{
		return contactCity;
	}

	public void setContactCity(String contactCity)
	{
		this.contactCity = contactCity;
	}

	public String getContactZip()
	{
		return contactZip;
	}

	public void setContactZip(String contactZip)
	{
		this.contactZip = contactZip;
	}

	public String getContactCountry()
	{
		return contactCountry;
	}

	public void setContactCountry(String contactCountry)
	{
		this.contactCountry = contactCountry;
	}

	public String getContactIco()
	{
		return contactIco;
	}

	public void setContactIco(String contactIco)
	{
		this.contactIco = contactIco;
	}

	public String getContactIcdph()
	{
		return contactIcdph;
	}

	public void setContactIcdph(String contactIcdph)
	{
		this.contactIcdph = contactIcdph;
	}

	public String getContactDic()
	{
		return contactDic;
	}

	public void setContactDic(String contactDic)
	{
		this.contactDic = contactDic;
	}

	public String getFieldA()
	{
		return fieldA;
	}

	public void setFieldA(String fieldA)
	{
		this.fieldA = fieldA;
	}

	public String getFieldB()
	{
		return fieldB;
	}

	public void setFieldB(String fieldB)
	{
		this.fieldB = fieldB;
	}

	public String getFieldC()
	{
		return fieldC;
	}

	public void setFieldC(String fieldC)
	{
		this.fieldC = fieldC;
	}

	public String getFieldD()
	{
		return fieldD;
	}

	public void setFieldD(String fieldD)
	{
		this.fieldD = fieldD;
	}

	public String getFieldE()
	{
		return fieldE;
	}

	public void setFieldE(String fieldE)
	{
		this.fieldE = fieldE;
	}

	public String getFieldF()
	{
		return fieldF;
	}

	public void setFieldF(String fieldF)
	{
		this.fieldF = fieldF;
	}

	public String getHtmlCode()
	{
		return htmlCode;
	}

	public void setHtmlCode(String htmlCode)
	{
		this.htmlCode = htmlCode;
	}

	public String getCurrency()
	{
		return currency;
	}

	public void setCurrency(String currency)
	{
		this.currency = currency;
	}

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