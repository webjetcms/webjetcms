package sk.iway.iwcm.system.stripes;

import net.sourceforge.stripes.action.ForwardResolution;

/**
 *  ErrorResolution.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: Sep 7, 2009 2:04:37 PM
 *@modified     $Date: 2009/09/07 12:07:53 $
 */
public class ErrorResolution extends ForwardResolution
{
	public ErrorResolution()
	{
		super("/components/maybeError.jsp");
	}
}
