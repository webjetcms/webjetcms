package sk.iway.iwcm.stat;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *  zachytava vytvorenie a zrusenie session
 *
 *@Title        Podpora kolaborativneho editovania dokumentov
 *@Company      Diplomova praca
 *@Copyright    Lubos Balat, Copyright (c) 2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Sobota, 2002, marec 30
 *@modified     $Date: 2003/01/28 11:30:13 $
 */
public class SessionListener implements HttpSessionListener
{

   /**
    *  Constructor for the SessionListener object
    */
   public SessionListener() { }

   /**
    *  Description of the Method
    *
    *@param  hse  Description of the Parameter
    */
	@Override
	public void sessionCreated(HttpSessionEvent hse)
   {
      //HttpSession session = hse.getSession();
   }

   /**
    *  Description of the Method
    *
    *@param  hse  Description of the Parameter
    */
	@Override
	public void sessionDestroyed(HttpSessionEvent hse)
   {
      HttpSession session = hse.getSession();

      SessionHolder holder = SessionHolder.getInstance();
      SessionDetails sesDetails = holder.get(session.getId());

      if (sesDetails != null)
      {
         holder.remove(session.getId());
      }
   }

}
