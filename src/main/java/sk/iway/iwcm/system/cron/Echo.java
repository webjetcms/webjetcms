package sk.iway.iwcm.system.cron;

import java.util.Arrays;

import sk.iway.iwcm.Logger;

/**
 *  Echo.java
 *  
 *  Prints it's argument - servers for debugging purposes
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 2.12.2010 12:35:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Echo
{
	public static void main(String[] args)
	{
		Logger.printf(Echo.class, "Echoing: %s", Arrays.toString(args));
	}
}