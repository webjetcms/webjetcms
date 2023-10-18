package sk.iway.iwcm.ebanking.epayments;

import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  UniPlatbaInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.4 $
 *@created      Date: 26.8.2009 14:12:32
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
class UniPlatbaInformation extends PaymentInformation
{

	
	public UniPlatbaInformation()
	{
		//nacitaj konfiguracne hodnoty do fieldov
		merchantId = Constants.getString("basketPaymentUniPlatbaMid");
		key = getDecrypredKey(Constants.getString("basketPaymentUniPlatbaKey"));
		constantSymbol = Constants.getInt("basketPaymentUniPlatbaConstantSymbol");
	}
	
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentUniPlatbaUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "";
		//return "http://epaymentsimulator.monogram.sk/UCB_UniPlatba.aspx";
	}
	
	@Override
	public boolean hasOwnForm()
	{
		return true;
	}
	
	
	
	@Override
	public boolean validateBankResponce(HttpServletRequest request) throws Exception
	{
		String vs=null;
		String res=null;
		String sign=null;
		Enumeration<?> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements())
		{
			String parameterName = parameterNames.nextElement().toString();
			if ("VS".equalsIgnoreCase(parameterName))
				vs = request.getParameter(parameterName);
			else if("RES".equalsIgnoreCase(parameterName))
				res = request.getParameter(parameterName);				
			else if("SIGN".equalsIgnoreCase(parameterName))
				sign = request.getParameter(parameterName);
		}
		
		if (Tools.isAnyEmpty(sign, vs, res))
			throw new InvalidBankResponceException();
		
		if (!Arrays.asList("OK", "NO").contains(res))
			throw new InvalidBankResponceException();
		
		String signWeExpect = generateSign(vs + res);
		
		if (!signWeExpect.equals(sign))
			throw new InvalidSignatureException("Expected: "+signWeExpect+", got: "+sign);
		
		return "OK".equals(res);
	}
	

	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{			
		String action = getUrlString();
		
		StringBuilder form = new StringBuilder().
			append("<form method=\"get\" action=\""+action+"\" name='payForm'>").
			append("<input type='hidden' name='MID' value='"+getMerchantId()+"'/>").
			append("<input type='hidden' name='LNG' value='"+getLng()+"'/>").
			append("<input type='hidden' name='AMT' value='"+payment.getAmountString()+"'/>").
			append("<input type='hidden' name='VS' value='"+payment.getVariableSymbol()+"'/>").
			append("<input type='hidden' name='CS' value='"+payment.getConstantSymbol()+"'/>");
		if(Tools.isNotEmpty(payment.getSpecificSymbol()))
			form.append("<input type='hidden' name='SS' value='"+payment.getSpecificSymbol()+"'/>");
		if(Tools.isNotEmpty(payment.getDescription()))
			form.append("<input type='hidden' name='DESC' value='"+payment.getDescriptionFormatted(35, false)+"'/>");
		form.append("<input type='hidden' name='sign' value='"+generateOutgoingSign(payment, request)+"'/>").
			append("</form>");
		return form.toString();
	}
	
	private String generateOutgoingSign(Payment payment, HttpServletRequest request)
	{
		StringBuilder toHash = new StringBuilder().
			append(getMerchantId()).
			append(getLng()).
			append(payment.getAmountString()).
			append(payment.getVariableSymbol()).
			append(payment.getConstantSymbol());
		if(Tools.isNotEmpty(payment.getSpecificSymbol()))
			toHash.append(payment.getSpecificSymbol());
		if(Tools.isNotEmpty(payment.getDescription()))
			toHash.append(payment.getDescriptionFormatted(35, false));
			
		return generateSign(toHash.toString());
	}
	
	private String generateSign(String stringToEncypher)
	{
		return DefaultHashCalculator.calculateHash(stringToEncypher, getKey());
	}

	private String getLng()
	{
		String lng = SetCharacterEncodingFilter.getCurrentRequestBean().getLng();
		if("eng".equals(lng))
		{
			return "EN";
		}
		return "SK";
	}
	
}
