package sk.iway.iwcm.ebanking;

/**
 *  NoRounding.java
 *  
 *  Does not round - a suitable policy for EUR or USD calculations
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.11.2010 14:43:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class NoRounding implements RoundingStrategy
{
	@Override
	public Money round(Money original)
	{
		return original;
	}
}