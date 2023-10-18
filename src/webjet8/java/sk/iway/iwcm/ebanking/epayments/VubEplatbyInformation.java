package sk.iway.iwcm.ebanking.epayments;

import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  VubEplatbyInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.4 $
 *@created      Date: 26.8.2009 14:11:42
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
class VubEplatbyInformation extends PaymentInformation
{

	VubEplatbyInformation()
	{
		this.merchantId = Constants.getString("basketPaymentVubEplatbyMid");
		this.key = getDecrypredKey(Constants.getString("basketPaymentVubEplatbyKey"));
		this.constantSymbol = Integer.valueOf(Constants.getInt("basketPaymentVubEplatbyConstantSymbol"));
	}		
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentVubEplatbyUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://ib.vub.sk/e-platbyeuro.aspx";
		//return "http://epaymentsimulator.monogram.sk/VUB_EPlatba.aspx";
	}
	@Override
	public String getReturnEmail()
	{
		return Constants.getString("basketPaymentVubEplatbyNotificationEmail");
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
		
		StringBuilder form = new StringBuilder().
		append("<FORM action="+getUrlString()+" METHOD=\"POST\" name='payForm'>").
			append("<INPUT type=\"hidden\" NAME=\"MID\" value='"+getMerchantId()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"AMT\" value='"+payment.getAmountString()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"VS\" value='"+payment.getVariableSymbol()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"CS\" value='"+payment.getConstantSymbol()+"' />").
			append("<INPUT type=\"hidden\" NAME=\"RURL\" value='"+request.getAttribute("RURL")+"' />").
			append("<INPUT type=\"hidden\" NAME=\"SIGN\" value='"+generateOutcomingSign(payment, request)+"' />");
		if (Tools.isNotEmpty(payment.getSpecificSymbol()))
			form.append("<INPUT type=\"hidden\" NAME=\"SS\" value='"+payment.getSpecificSymbol()+"' />");
		if(Tools.isNotEmpty(getReturnEmail()))
			form.append("<INPUT type=\"hidden\" NAME=\"REM\" value='"+getReturnEmail()+"' />");
		if(Tools.isNotEmpty(payment.getDescription()))
			form.append("<INPUT type=\"hidden\" NAME=\"DESC\" value='"+payment.getDescriptionFormatted(35, true)+"' />");
		form.append("</FORM>");
		return form.toString();
	}	
	
	private String generateOutcomingSign(Payment payment, HttpServletRequest request)
	{
		String stringToEncypher = new StringBuilder().
			append(getMerchantId()).
			append(payment.getAmountString()).
			append(payment.getVariableSymbol()).
			append(payment.getConstantSymbol()).
			append(request.getAttribute("RURL")).
			toString();
		return generateSign(stringToEncypher);
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
		String specificSymbol = "";
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
			if ("SS".equalsIgnoreCase(parameterName))
				specificSymbol = value;
			if ("RES".equalsIgnoreCase(parameterName))
				result = value;
		}
		
		if (Tools.isAnyEmpty(signWeGot, variableSymbol, result))
			throw new InvalidBankResponceException();
		
		if (!Arrays.asList("OK", "FAIL").contains(result))
			throw new InvalidBankResponceException();
		
		String signWeExpect = generateSign(variableSymbol + specificSymbol + result);
		
		if (!signWeExpect.equals(signWeGot))
			throw new InvalidSignatureException("Expected: "+signWeExpect+", got: "+signWeGot);
		
		return "OK".equals(result);
	}
}