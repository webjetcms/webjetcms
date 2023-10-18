package sk.iway.iwcm.components.basket;

import java.math.BigDecimal;
import java.util.Date;

/**
 *  BasketInvoicePayment.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: bhric $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.4.2010 13:30:58
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BasketInvoicePayment
{
	private int paymentId;
	private int invoiceId;
	private Date createDate = null;
	private BigDecimal payedPrice;
	private String paymentMethod;
	private Date closedDate = null;
	private Boolean confirmed = null; 
	
	
	public int getPaymentId()
	{
		return paymentId;
	}
	public void setPaymentId(int paymentId)
	{
		this.paymentId = paymentId;
	}
	public int getInvoiceId()
	{
		return invoiceId;
	}
	public void setInvoiceId(int invoiceId)
	{
		this.invoiceId = invoiceId;
	}
	public Date getCreateDate()
	{
		return createDate == null ? null : (Date) createDate.clone();
	}
	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate == null ? null : (Date) createDate.clone();
	}
	public BigDecimal getPayedPrice()
	{
		return payedPrice;
	}
	public void setPayedPrice(BigDecimal payedPrice)
	{
		this.payedPrice = payedPrice;
	}
	public String getPaymentMethod()
	{
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod)
	{
		this.paymentMethod = paymentMethod;
	}
	public Date getClosedDate()
	{
		return closedDate == null ? null : (Date) closedDate.clone();
	}
	public void setClosedDate(Date closedDate)
	{
		this.closedDate = closedDate == null ? null : (Date) closedDate.clone();
	}
	public Boolean getConfirmed()
	{
		return confirmed;
	}
	public void setConfirmed(Boolean confirmed)
	{
		this.confirmed = confirmed;
	}
}
