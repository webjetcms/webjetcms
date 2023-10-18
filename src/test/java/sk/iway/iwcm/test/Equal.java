package sk.iway.iwcm.test;

/**
 *  Equal.java
 *		
 *		Allows us to redefine Object.equal() method for testing purposes
 *
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 4.12.2009 14:49:41
 *@modified     $Date: 2009/12/11 15:46:32 $
 */
public interface Equal<T1, T2>
{
	public boolean areEqual(T1 object1, T2 object2);
}
