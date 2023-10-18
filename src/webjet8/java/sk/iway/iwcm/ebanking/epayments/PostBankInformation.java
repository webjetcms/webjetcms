package sk.iway.iwcm.ebanking.epayments;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  PostBankInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.2 $
 *@created      Date: 26.8.2009 14:12:54
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public class PostBankInformation extends PaymentInformation
{

	public PostBankInformation()
	{
		this.merchantId = Constants.getString("basketPaymentPostBankMid");
	}
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentPostBankUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://ibpb2.pabk.sk/inbank/gateposk.asp";
	}
	
	@Override
	public boolean hasOwnForm()
	{
		return true;
	}
	
	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{			
		StringBuilder action = new StringBuilder(getUrlString()).append('?');
		action.append("P1=").append(payment.getAmount());
		action.append("&P2=0&P3=0&P7=").append(payment.getVariableSymbol()).append("&P8=").append(getMerchantId());
		String form = "<form method=\"post\" action=\""+action.toString()+"\" name=\"cardPayForm\"/>";
		return form;
	}
}
