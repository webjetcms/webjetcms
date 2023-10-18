package sk.iway.iwcm.ebanking.epayments;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  DexiaPayInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.3 $
 *@created      Date: 27.8.2009 16:36:02
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
class DexiaPayInformation extends PaymentInformation
{
	public DexiaPayInformation()
	{
		//nacitaj konfiguracne hodnoty do fieldov
		merchantId = Constants.getString("basketPaymentDexiaPayMid");
		key = getDecrypredKey(Constants.getString("basketPaymentDexiaPayKey"));
		constantSymbol = Constants.getInt("basketPaymentDexiaPayConstantSymbol");
	}
	
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentDexiaPayUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
		return "https://eb.dexia.sk/dexiapay/default.aspx";
		//return "http://epaymentsimulator.monogram.sk/DB_DexiaPay.aspx";
	}

	@Override
	public String getReturnEmail()
	{
		return Constants.getString("basketPaymentDexiaPayNotificationEmail");
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
		String ss=null;
		String res=null;
		String sign=null;
		Enumeration<?> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements())
		{
			String parameterName = parameterNames.nextElement().toString();
			String value = request.getParameter(parameterName);
			if ("VS".equals(parameterName))
				vs = value;
			if ("SS".equals(parameterName))
				ss = value;
			if("RES".equals(parameterName))
				res = value;				
			if("SIGN".equals(parameterName))
				sign = value;
		}
		
		if (Tools.isAnyEmpty(sign, vs, ss, res))
			throw new InvalidBankResponceException();
		
		if (!Arrays.asList("OK", "NO").contains(res))
			throw new InvalidBankResponceException();
		
		String signWeExpect = generateSign("VS="+vs+";SS="+ss+";RES="+res);
		
		if (!signWeExpect.equals(sign))
			throw new InvalidSignatureException("Expected: "+signWeExpect+", got: "+sign);
		
		return "OK".equals(res);
	}

	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{			
		String action = getUrlString();
		
		StringBuilder form = new StringBuilder().
			append("<form method=\"get\" action=\""+action+"\" name=\"payForm\">").
			append("<input type='hidden' name='MID' value='"+getMerchantId()+"'/>").
			append("<input type='hidden' name='AMT' value='"+amountFormatted(payment.getAmount())+"'/>").
			append("<input type='hidden' name='VS' value='"+payment.getVariableSymbol()+"'/>").
			append("<input type='hidden' name='SS' value='"+payment.getSpecificSymbol()+"'/>");
		if(Tools.isNotEmpty(payment.getSpecificSymbol()))
			form.append("<input type='hidden' name='CS' value='"+payment.getConstantSymbol()+"'/>");
		if(Tools.isNotEmpty(payment.getDescription()))
			form.append("<input type='hidden' name='DESC' value='"+payment.getDescriptionFormatted(35, false)+"'/>");
		
		String rurl=(String)request.getAttribute("RURL");
		if(Tools.isNotEmpty(rurl))
			form.append("<input type='hidden' name='RURL' value='"+rurl+"'/>");
		if(Tools.isNotEmpty(getReturnEmail()))
			form.append("<INPUT type='hidden' NAME='REM' value='"+getReturnEmail()+"' />");
		form.append("<input type='hidden' name='SIGN' value='"+generateOutgoingSign(payment,request)+"'/>").
			append("</form>");
		return form.toString();
	}

	
	private String generateOutgoingSign(Payment payment, HttpServletRequest request)
	{
		StringBuilder toHash = new StringBuilder().
		append("MID="+getMerchantId()).
		append(";AMT="+amountFormatted(payment.getAmount())).
		append(";VS="+payment.getVariableSymbol()).
		append(";SS="+payment.getSpecificSymbol());
	if(Tools.isNotEmpty(payment.getConstantSymbol()))
		toHash.append(";CS="+payment.getConstantSymbol());
	return generateSign(toHash.toString());
	}
	
	
	private String generateSign(String stringToEncrypt)
	{
		String sign = TripleDesHashCalculator.calculateHash(stringToEncrypt, getKey());
		return sign;
	}

	private String amountFormatted(BigDecimal amount)
	{
		return new DecimalFormat("000").format(amount.multiply(BigDecimal.valueOf(100)));
	}
	
	
}
