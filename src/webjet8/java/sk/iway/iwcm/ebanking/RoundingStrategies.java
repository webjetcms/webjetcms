package sk.iway.iwcm.ebanking;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

/**
 *  RoundingStrategies.java
 *  
 *  Facade for access to rounding strategies.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.11.2010 14:44:09
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RoundingStrategies
{
	//fill later
	private static final List<Currency> currenciesWithNoRounding = Arrays.asList(
		Currency.getInstance("EUR"), Currency.getInstance("USD")
	);
	
	/**
	 * Returns default rounding strategies required by law 
	 * for a given currency 
	 */
	public static RoundingStrategy getRoundingStrategy(Currency currency)
	{
		if(currenciesWithNoRounding.contains(currency))
			return new NoRounding();
		
		throw new IllegalArgumentException("Don't know how to round "+currency);
	}
	
	public static RoundingStrategy noRounding()
	{
		return new NoRounding();
	}
	
	public static RoundingStrategy toFiveCentsUpwards()
	{
		return upwardsTo(5);
	}
	
	public static RoundingStrategy toFiftyCentsUpwards()
	{
		return upwardsTo(50);
	}
	
	public static RoundingStrategy toFiveCentsDownwards()
	{
		return downwardsTo(5);
	}
	
	public static RoundingStrategy toFiftyCentsDownwards()
	{
		return downwardsTo(50);
	}

	public static RoundingStrategy downwardsTo(final int cents)
	{
		return new RoundingStrategy(){
			@Override
			public Money round(Money original){
				long newAmount = original.getAmount() - original.getAmount() % cents;
				return Money.fromCents(newAmount, original.getCurrency());
			}
		};
	}
	
	public static RoundingStrategy upwardsTo(final int cents)
	{
		return new RoundingStrategy(){
			@Override
			public Money round(Money original){
				long newAmount = original.getAmount() - original.getAmount() % cents;
				if (original.getAmount() % cents != 0)
					newAmount += cents;
				return Money.fromCents(newAmount, original.getCurrency());
			}
		};
	}
}