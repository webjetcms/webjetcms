package sk.iway.iwcm.ebanking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

/**
 *  Money.java
 *  
 *  Represents monetary amount. Based on {@link Long} data type,
 *  thus avoiding float/double money antipattern, notoriously
 *  known for imprecise allocation and rounding.
 *
 *  Internally, the amount is held in the smallest monetary
 *  unit of the currency: usually in cents. 
 *  
 *  Use getDouble() to obtain the amount in "dollars"
 *  Use getAmount() to obtain number of "cents"
 *  
 *  @see MoneyLongConverter for JPA transformation
 *  @see RoundingStrategy 
 *  @see CurrencyConverter
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.11.2010 13:48:10
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Money implements Comparable<Money>
{
	private final long amount;
	private final Currency currency;
	
	private Money(long amount, Currency currency)
	{
		this.amount = amount;
		this.currency = currency;
	}
	
	//---------------CREATION METHODS------------------------
	
	public static final Money NOTHING = Money.fromEuroCents(0);
	
	public static Money fromCents(long amount, Currency currency)
	{
		return new Money(amount, currency);
	}
	
	public static Money fromEuroCents(long amount)
	{
		return fromCents(amount, Currency.getInstance("EUR"));
	}
	
	public static Money fromDouble(double amount, Currency currency)
	{
		if (currency.getDefaultFractionDigits() < 1)
			return new Money((long)amount, currency);
		
		long amountAsLong = (long)(Math.round(amount * Math.pow(10.0, currency.getDefaultFractionDigits())));
		return new Money(amountAsLong, currency);
	}
	
	public static Money fromEuroDouble(double amount)
	{
		return fromDouble(amount, Currency.getInstance("EUR"));
	}
	
	//---------------OPERATIONS------------------------
	
	public Money plus(Money whatToAdd)
	{
		if (whatToAdd.getCurrency() != getCurrency())
			throw new IllegalArgumentException("Cannot add "+this+" and "+whatToAdd);
		return new Money(amount + whatToAdd.getAmount(), currency);
	}
	
	public Money plus(Money whatToAdd, CurrencyConverter converter)
	{
		Money whatToAddInOutCurrency = converter.convert(whatToAdd, this.currency);
		return new Money(amount + whatToAddInOutCurrency.getAmount(), currency);
	}
	
	public Money minus(Money whatToSubtract)
	{
		return plus(new Money(-whatToSubtract.getAmount(), whatToSubtract.getCurrency()));
	}
	
	public Money minus(Money whatToSubtract, CurrencyConverter converter)
	{
		return plus(new Money(-whatToSubtract.getAmount(), whatToSubtract.getCurrency()), converter);
	}
	
	public Money times(double scale)
	{
		return fromDouble(amount*scale, currency);
	}
	
	public Money divide(double scale)
	{
		return fromDouble(amount/scale, currency);
	}
	
	public Money round()
	{
		return RoundingStrategies.getRoundingStrategy(currency).round(this);
	}
	
	public Money round(RoundingStrategy rounding)
	{
		return rounding.round(this);
	}
	
	public List<Money> splitIntoParts(int numberOfParts)
	{
		long leftToAssign = amount;
		int signum = amount > 0 ? 1 : -1;
		List<Money> split = new ArrayList<Money>();
		
		//distribute the main part
		for (int i = 0 ; i < numberOfParts ; i++)
		{
			Money part = Money.fromCents(amount / numberOfParts, currency);
			leftToAssign -= part.getAmount();
			split.add(part);
		}
		
		//distribute leftovers evenly
		int asigneeIndex = 0;
		while(leftToAssign*signum > 0)
		{
			Money newPart = split.get(asigneeIndex % numberOfParts).plus(Money.fromCents(signum, currency));
			split.set(asigneeIndex % numberOfParts, newPart);
			leftToAssign -= signum;
			asigneeIndex++;
		}
		return split;
	}
	
	//--------------GETTERS AND SETTERS-------------------

	public double getDouble()
	{
		if (currency.getDefaultFractionDigits() < 1)
			return (double)amount;
		
		return amount / Math.pow(10.0, currency.getDefaultFractionDigits());
	}

	public long getAmount()
	{
		return amount;
	}

	public Currency getCurrency()
	{
		return currency;
	}
	
	//------------UTILITY METHODS---------------
	
	/**
	 * Compares 2 {@link Money} instances. 
	 * Work if and only if both instances are of the same currency.
	 * 
	 * Use your own {@link Comparator} if currencies differ
	 */
	@Override
	public int compareTo(Money anotherMoney)
	{
		if (anotherMoney.currency != this.currency)
			throw new IllegalStateException("Comparing two money of unequal currencies: "+this+" ,"+anotherMoney);
		return Long.signum(this.amount - anotherMoney.amount);
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (amount ^ (amount >>> 32));
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Money other = (Money) obj;
		if (amount != other.amount)
			return false;
		if (currency == null)
		{
			if (other.currency != null)
				return false;
		}
		else if (currency != other.currency)
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("%.2f %s", getDouble(), currency.toString());
	}
}