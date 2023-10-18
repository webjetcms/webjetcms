package sk.iway.iwcm.ebanking.epayments;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.Password;
import sk.iway.iwcm.Constants;

/**
 *  SporoPayReponseValidator.java
 *
 *		Validates a reponse coming from SporoPay. Extracted from {@link SporoPayInformation}
 *		because of it's length and discohesiveness with the rest of the class.
 *
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.9.2009 13:46:49
 *@modified     $Date: 2010/03/05 08:18:54 $
 */
class SporoPayReponseValidator
{
	String clientPrefix;
	String clientAccount;
	String clientBankCode;
	String ourAccountPrefix;
	String ourAccount;
	String ourBankCode;
	String amount;
	String currency;
	String variableSymbol;
	String specificSymbol;
	String url;
	String params;
	String result;
	String realization;
	String sign;

	public SporoPayReponseValidator(HttpServletRequest request)
	{
		clientPrefix = request.getParameter("u_predcislo");
		clientAccount = request.getParameter("u_cislo");
		clientBankCode = request.getParameter("u_kbanky");
		ourAccountPrefix = request.getParameter("pu_predcislo");
		ourAccount = request.getParameter("pu_cislo");
		ourBankCode = request.getParameter("pu_kbanky");
		amount = request.getParameter("suma");
		currency = request.getParameter("mena");
		variableSymbol = request.getParameter("vs");
		specificSymbol = request.getParameter("ss");
		url = request.getParameter("url");
		params = request.getParameter("param");
		result = request.getParameter("result");
		realization = request.getParameter("real");
		sign = request.getParameter("SIGN2");
	}

	public boolean validate() throws InvalidSignatureException, InvalidBankResponceException
	{
		checkParameters();
		String signWeExpect = generateSign();

		if (!signWeExpect.equalsIgnoreCase(sign))
			throw new InvalidSignatureException("Expected: "+signWeExpect+", got: "+sign);

		return "OK".equalsIgnoreCase(realization) && "OK".equalsIgnoreCase(result);
	}

	private String generateSign()
	{
		String toBeHashed = new StringBuilder().
			append(clientPrefix).
			append(';').append(clientAccount).
			append(';').append(clientBankCode).
			append(';').append(ourAccountPrefix).
			append(';').append(ourAccount).
			append(';').append(ourBankCode).
			append(';').append(amount).
			append(';').append(currency).
			append(';').append(variableSymbol).
			append(';').append(specificSymbol).
			append(';').append(url).
			append(';').append(params).
			append(';').append(result).
			append(';').append(realization).
			toString();
		return TripleDesHashCalculator.calculateHash(toBeHashed, getDecrypredKey(Constants.getString("basketPaymentSporoPayKey")));
	}


	private void checkParameters() throws InvalidBankResponceException
	{
		List<String> parameters = Arrays.asList(clientPrefix, clientAccount, clientBankCode,
			ourAccountPrefix, ourAccount, ourBankCode, amount, currency, variableSymbol, specificSymbol,
			url, params, result, realization, sign);

		for (String parameter : parameters)
			if (parameter == null)
				throw new InvalidBankResponceException();

		if (!Arrays.asList("OK", "NOK","ok","nok").contains(result))
			throw new InvalidBankResponceException();

		if (!Arrays.asList("OK", "NOK","ok","nok").contains(realization))
			throw new InvalidBankResponceException();

		if (!ourAccount.equals(Constants.getString("basketPaymentSporoPayAccount")))
			throw new InvalidBankResponceException();

		if (!ourAccountPrefix.equals(Constants.getString("basketPaymentSporoPayAccountPrefix")))
			throw new InvalidBankResponceException();
	}

	private String getDecrypredKey(String key)
	{
		try
		{
			Password password = new Password();
			return password.decrypt(key);
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return "";
	}

}
