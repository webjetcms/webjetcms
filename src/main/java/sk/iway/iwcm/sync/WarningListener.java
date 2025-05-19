package sk.iway.iwcm.sync;

import java.beans.ExceptionListener;

import sk.iway.iwcm.Logger;

/**
 *  WarningListener.java - Preskakuje Warning Messages pri Synchronizacii
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2005/10/25 06:48:03 $
 *@modified     $Date: 2005/10/25 06:48:03 $
 */
public class WarningListener implements ExceptionListener
{
	 @Override
   public void exceptionThrown(Exception e)
   {
   	sk.iway.iwcm.Logger.error(e);
   	Logger.error(this,"Time to update your software");
   }
}
