package sk.iway.iwcm.stat.heat_map;

/**
 *  GeneratorSizeAdjustment.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.6.2010 12:55:55
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
interface GeneratorSizeAdjustment
{
	public int calculateCircleSize();
	public float calculateMaximumAlphaForOneCircle();
}
