package sk.iway.iwcm.ebanking.epayments;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  SporoPayInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.6 $
 *@created      Date: 26.8.2009 14:13:15
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public class SporoPayInformation extends PaymentInformation
{

	private String accountPrefix;
	
	private String accountId;
	
	private StringBuilder form;
	
	public SporoPayInformation()
	{
		//nacitaj konfiguracne hodnoty do fieldov
		accountId = Constants.getString("basketPaymentSporoPayAccount");
		accountPrefix = Constants.getString("basketPaymentSporoPayAccountPrefix");
		key = getDecrypredKey(Constants.getString("basketPaymentSporoPayKey"));
		constantSymbol = Constants.getInt("basketPaymentSporoPayConstantSymbol");
	}
	
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentSporoPayUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://ib.slsp.sk/epayment/epayment/epayment.xml";
		//return "http://epaymentsimulator.monogram.sk/SLSP_SporoPay.aspx";
	}
	
	@Override
	public boolean hasOwnForm()
	{
		return true;
	}
	
	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{
		if (request.getAttribute("RURL") == null)
			throw new IllegalStateException("Supply return URL, please: request.setAttribute('RURL',...)."+
				"RURL is an URL address of a page to which a user will be redirected after the payment is finished");
		
		String returnUrl = request.getAttribute("RURL").toString();
		form = new StringBuilder("<form method='get' action='"+getUrlString()+"' name='payForm'/>").
			append("<INPUT NAME='pu_predcislo' TYPE='hidden' VALUE='"+prependLeadingZeros(accountPrefix, 6)+"' />").
			append("<INPUT NAME='pu_cislo' TYPE='hidden' VALUE='"+prependLeadingZeros(accountId, 10)+"' />").
			append("<INPUT NAME='pu_kbanky' TYPE='hidden' VALUE='0900' />").
			append("<INPUT NAME='suma' TYPE='hidden' VALUE='"+payment.getAmountString()+"' />").
			append("<INPUT NAME='mena' TYPE='hidden' VALUE='EUR' />").
			append("<INPUT NAME='vs' TYPE='hidden' VALUE='"+prependLeadingZeros(payment.getVariableSymbol(), 10)+"' />").
			append("<INPUT NAME='ss' TYPE='hidden' VALUE='"+prependLeadingZeros(payment.getSpecificSymbol(), 10)+"' />").
			append("<INPUT NAME='url' TYPE='hidden' VALUE='"+returnUrl+"' />").
			append("<INPUT NAME='param' TYPE='hidden' VALUE='id="+payment.getVariableSymbol()+"' />").
			append("<INPUT NAME='sign1' TYPE='hidden' VALUE='"+generateOutcomingSign(payment, request)+"' />").
			append("</form>");
		
		return form.toString();
	}
	
	private String prependLeadingZeros(String symbolToPrepend, int totalLength)
	{
		for (int i = symbolToPrepend.length(); i < totalLength ;i++)
			symbolToPrepend = "0" + symbolToPrepend;
		return symbolToPrepend;
	}
	
	private String generateOutcomingSign(Payment payment, HttpServletRequest request)
	{
		StringBuilder toHash = new StringBuilder().
			append(prependLeadingZeros(accountPrefix, 6)).
			append(';').append(prependLeadingZeros(accountId, 10)).
			append(";0900").
			append(';').append(payment.getAmountString()).
			append(";EUR").
			append(';').append(prependLeadingZeros(payment.getVariableSymbol(), 10)).
			append(';').append(prependLeadingZeros(payment.getSpecificSymbol(), 10)).
			append(';').append(request.getAttribute("RURL")).
			append(';').append("id="+payment.getVariableSymbol());
		
		return TripleDesHashCalculator.calculateHash(toHash.toString(), key);
	}

	
	@Override
	public boolean validateBankResponce(HttpServletRequest request) throws Exception
	{
		return new SporoPayReponseValidator(request).validate();
	}
}