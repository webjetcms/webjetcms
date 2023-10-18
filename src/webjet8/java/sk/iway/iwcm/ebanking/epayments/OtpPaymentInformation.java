package sk.iway.iwcm.ebanking.epayments;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  OtpPaymentInformation.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.2 $
 *@created      Date: 31.8.2009 14:08:23
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public class OtpPaymentInformation extends PaymentInformation
{
	
	OtpPaymentInformation()
	{
		this.merchantId = Constants.getString("basketPaymentOtpMid");
		this.key = getDecrypredKey(Constants.getString("basketPaymentOtpKey"));
	}		
	
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentOtpUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://www.otpdirekt.sk/index_eshop1024.html";
	}
	
	@Override
	public boolean hasOwnForm()
	{
		return true;
	}
	
	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{
		StringBuilder form = new StringBuilder().
		append("<FORM action="+getUrlString()+" METHOD=\"GET\" name='payForm'>").
			append("<INPUT type=\"hidden\" NAME=\"ESHOP\" value="+getMerchantId()+" />").
			append("<INPUT type=\"hidden\" NAME=\"CASTKA\" value="+payment.getAmountString()+" />").
			append("<INPUT type=\"hidden\" NAME=\"VS\" value="+payment.getVariableSymbol()+" />").
			append("<INPUT type=\"hidden\" NAME=\"CS\" value="+payment.getConstantSymbol()+" />").
			append("<INPUT type=\"hidden\" NAME=\"URL\" value="+request.getAttribute("RURL")+" />");
		if (Tools.isNotEmpty(payment.getSpecificSymbol()))
			form.append("<INPUT type=\"hidden\" NAME=\"SS\" value="+payment.getSpecificSymbol()+" />");
		form.append("</FORM>");
		return form.toString();
	}
	
	@Override
	public boolean validateBankResponce(HttpServletRequest request) throws Exception
	{
		//nemaju SIGN ani nic????, treba pockat na dospecifikovanie
		return true;
	}
}