package sk.iway.iwcm.ebanking;

import java.util.Currency;

/**
 *  CurrencyConverter.java
 *  
 *  Converts {@link Money} instance to another
 *  {@link Currency}. 
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 5.11.2010 16:40:59
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface CurrencyConverter
{
	public Money convert(Money convertThis, Currency toCurrency);
}