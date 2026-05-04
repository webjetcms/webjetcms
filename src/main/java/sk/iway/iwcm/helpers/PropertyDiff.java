package sk.iway.iwcm.helpers;

/**
 *  PropertyDiff.java
 *	
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.3.2010 16:53:03
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PropertyDiff
{
	
	public Object valueBefore;
	
	public Object valueAfter;
	
	@Override
	public String toString()
	{
		return new StringBuilder().append("\nProperty changed from ").append(valueBefore).append("to").append(valueAfter).toString();
	}
}
