package sk.iway.iwcm.ebanking;

/**
 *  RoundingStrategy.java
 *  
 *  Implement this interface to round instances of {@link Money}
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.11.2010 14:41:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface RoundingStrategy
{
	public Money round(Money original);
}