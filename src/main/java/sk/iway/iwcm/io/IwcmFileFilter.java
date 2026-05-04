package sk.iway.iwcm.io;

/**
 *  IwcmFileFilter.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: 11.9.2008 17:13:53
 *@modified     $Date: 2008/09/17 08:52:18 $
 */
public interface IwcmFileFilter 
{
	public boolean accept(IwcmFile pathname);
}
