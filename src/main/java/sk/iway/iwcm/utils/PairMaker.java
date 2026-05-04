package sk.iway.iwcm.utils;

/**
 *  PairMaker.java
 *  
 *  Tranforms a source element (of third type) into a {@link Pair}&lt;type1, type2&gt;
 *  
 *  @see MapUtils
 *  @see Pair
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 15:36:03
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface PairMaker<T1, T2, T3>
{
	Pair<T1, T2> makePair(T3 source);
}