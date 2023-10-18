package sk.iway.iwcm.ebanking;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *  Payment.java
 *
 *		Encapsulates details about payment recipient. Used in creating
 *		export files individual bank.
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.4 $
 *@created      Date: 25.8.2009 16:10:21
 *@modified     $Date: 2009/12/11 14:51:54 $
 */
public class Payment
{
	String accountNumber;

	String accountPrefix;

	String bankCode;

	BigDecimal amount;

	String currency;

	String constantSymbol;

	String variableSymbol;

	String specificSymbol;

	String description;

	String buyerName;


	public String getAccountNumber()
	{
		return this.accountNumber;
	}

	public Payment setAccountNumber(String accountNumber)
	{
		this.accountNumber = accountNumber;
		return this;
	}

	public String getAccountPrefix()
	{
		if (accountPrefix == null) return "000000";
		return this.accountPrefix;
	}

	public Payment setAccountPrefix(String accountPrefix)
	{
		this.accountPrefix = accountPrefix;
		return this;
	}

	public String getBankCode()
	{
		if (bankCode == null) return "";
		return this.bankCode;
	}

	public Payment setBankCode(String bankCode)
	{
		this.bankCode = bankCode;
		return this;
	}

	public BigDecimal getAmount()
	{
		return this.amount;
	}

	/**
	 * Vrati <code>amount</code> ako string s 2 desatinnymi miestami. Ak hodnota obsahuje viac nez 2 desatinne miesta
	 * tak ich odtrhne (pouziva ROUND_DOWN)
	 * @return
	 */
	public String getAmountString()
	{
		return this.amount.setScale(2, RoundingMode.DOWN).toPlainString();
	}

	public Payment setAmount(BigDecimal amount)
	{
		this.amount = amount;
		return this;
	}

	public String getCurrency()
	{
		if (currency == null) return "EUR";
		return this.currency;
	}

	public Payment setCurrency(String currency)
	{
		this.currency = currency;
		return this;
	}

	public String getConstantSymbol()
	{
		if (constantSymbol == null) return "";
		return this.constantSymbol;
	}

	public Payment setConstantSymbol(String constantSymbol)
	{
		this.constantSymbol = constantSymbol;
		return this;
	}

	public String getVariableSymbol()
	{
		if (variableSymbol == null) return "";
		return this.variableSymbol;
	}

	public Payment setVariableSymbol(String variableSymbol)
	{
		this.variableSymbol = variableSymbol;
		return this;
	}

	public String getSpecificSymbol()
	{
		if (specificSymbol == null) return "";
		return this.specificSymbol;
	}

	public Payment setSpecificSymbol(String specificSymbol)
	{
		this.specificSymbol = specificSymbol;
		return this;
	}

	public String getDescription()
	{
		if (description == null) return "";
		return description;
	}

	public Payment setDescription(String description)
	{
		this.description = description;
		return this;
	}

	public String getDescriptionFormatted(int length, boolean allowSpaces)
	{
		String desc = getDescription();
		if(!allowSpaces)
			desc = desc.replace(" ", "");

		return desc.substring(0,Math.min(length, desc.length()));
	}

	public String getBuyerName()
	{
		return buyerName;
	}

	public void setBuyerName(String buyerName)
	{
		this.buyerName = buyerName;
	}


}