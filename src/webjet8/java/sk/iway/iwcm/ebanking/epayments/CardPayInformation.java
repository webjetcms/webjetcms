package sk.iway.iwcm.ebanking.epayments;

import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  CardPayInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.1 $
 *@created      Date: 30.11.2009 10:33:18
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public class CardPayInformation extends PaymentInformation
{
	CardPayInformation()
	{
		this.merchantId = Constants.getString("basketPaymentTatraPayMid");
		this.key = getDecrypredKey(Constants.getString("basketPaymentTatraPayKey"));
		this.constantSymbol = Integer.valueOf(Constants.getInt("basketPaymentTatraPayConstantSymbol"));
	}
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentTatraPayUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://moja.tatrabanka.sk/cgi-bin/e-commerce/start/e-commerce.jsp";
		//return "http://epaymentsimulator.monogram.sk/TB_CardPay.aspx";
	}
	@Override
	public String getReturnEmail()
	{
		return Constants.getString("basketPaymentTatraPayNotificationEmail");
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
			throw new IllegalStateException("HttpServletRequest needs to have 'RURL'(return URL) attribute");
	
		String buyerName = payment.getBuyerName();
		if(Tools.isEmpty(buyerName))
			throw new IllegalStateException("Attribute 'Payment.buyerName' is required in CardPay");
		
		buyerName = getBuyerNameFormatted(buyerName);
		StringBuilder form = new StringBuilder().
		append("<FORM action='"+getUrlString()+"' METHOD='GET' name='payForm'>").
			append("<INPUT type=\"hidden\" NAME=\"PT\" value=\"CardPay\" />").
			append("<INPUT type=\"hidden\" NAME=\"MID\" value='"+getMerchantId()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"AMT\" value='"+payment.getAmountString()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"VS\" value='"+payment.getVariableSymbol()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"CS\" value='"+payment.getConstantSymbol()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"RURL\" value='"+request.getAttribute("RURL")+"' />").
			append("<INPUT type=\"hidden\" NAME=\"SIGN\" value='"+generateOutcomingSign(payment, request)+"' />").
			append("<INPUT type=\"hidden\" NAME=\"CURR\" value=\"978\" />").//978 = ISO EURO
			append("<INPUT type=\"hidden\" NAME=\"IPC\" value=\""+request.getRemoteAddr()+"\" />").
			append("<INPUT type=\"hidden\" NAME=\"NAME\" value=\""+buyerName+"\" />");
		if (Tools.isNotEmpty(getReturnEmail()))
			form.append("<INPUT type=\"hidden\" NAME=\"REM\" value='"+getReturnEmail()+"' />");			
		form.append("</FORM>");
		return form.toString();
	}	
	
	private String generateOutcomingSign(Payment payment, HttpServletRequest request)
	{
		StringBuilder stringToEncypher = new StringBuilder().
			append(getMerchantId()).
			append(payment.getAmountString()).
			append("978"). //978 = ISO EURO
			append(payment.getVariableSymbol()).
			append(payment.getConstantSymbol()).
			append(request.getAttribute("RURL")).
			append(request.getRemoteAddr()).
			append(getBuyerNameFormatted(payment.getBuyerName()));
		return generateSign(stringToEncypher.toString());
	}
	
	private String generateSign(String stringToEncypher)
	{
		return DefaultHashCalculator.calculateHash(stringToEncypher, getKey());
	}
	
	
	@Override
	public boolean validateBankResponce(HttpServletRequest request) throws Exception
	{
		String signWeGot = null;
		String variableSymbol = null;
		String ac="";
		String result = null;
		
		Enumeration<?> parameters = request.getParameterNames();
		while (parameters.hasMoreElements())
		{
			String parameterName = parameters.nextElement().toString();
			String value = request.getParameter(parameterName);;
			if ("SIGN".equalsIgnoreCase(parameterName))
				signWeGot = value;
			if ("VS".equalsIgnoreCase(parameterName))
				variableSymbol = value;
			if ("RES".equalsIgnoreCase(parameterName))
				result = value;
			if ("AC".equalsIgnoreCase(parameterName))
				ac = value;
		}
		
		if (Tools.isAnyEmpty(signWeGot, variableSymbol, result))
			throw new InvalidBankResponceException();
		
		if (!Arrays.asList("OK", "FAIL", "TOUT").contains(result))
			throw new InvalidBankResponceException();
		
		String signWeExpect = generateSign(variableSymbol + result + ac);
		
		if (!signWeExpect.equals(signWeGot))
			throw new InvalidSignatureException("Expected: "+signWeExpect+", got: "+signWeGot);
		
		return "OK".equals(result);
	}
	
	private String getBuyerNameFormatted(String buyerName)
	{
		String retBuyerName = DB.internationalToEnglish(buyerName);
		retBuyerName = retBuyerName.substring(0,Math.min(30, retBuyerName.length()));
		return retBuyerName;
	}
}
