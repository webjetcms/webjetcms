package sk.iway.iwcm;

/**
 *  CacheListener.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 11.5.2009 12:53:27
 *@modified     $Date: 2009/05/11 11:37:09 $
 */
public interface CacheListener
{	
	public void objectRemoved(CacheBean theObject);
	
	public void objectAdded(CacheBean theObject);
}
