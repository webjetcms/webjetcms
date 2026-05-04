package sk.iway.iwcm.system.stripes;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.exception.ExceptionHandler;
import net.sourceforge.stripes.exception.StripesServletException;

/**
 *  IwayStripesExceptionHandler.java - handler chyb pre Stripes
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.4.2009 23:00:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwayStripesExceptionHandler implements ExceptionHandler
{
	/** Simply rethrows the exception passed in. */
	@Override
   public void handle(Throwable throwable,
                      HttpServletRequest request,
                      HttpServletResponse response) throws ServletException
   {
   	System.out.println("EXCEPTION: "+throwable.getMessage());
   	sk.iway.iwcm.Logger.error(throwable);

       if (throwable instanceof ServletException) {
           throw (ServletException) throwable;
       }
       else {
           throw new StripesServletException
                   ("Unhandled exception caught by the default exception handler.", throwable);
       }

   }

   /** Does nothing. */
	@Override
   public void init(Configuration configuration) throws Exception { }
}
