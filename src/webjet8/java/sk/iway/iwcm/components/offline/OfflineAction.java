package sk.iway.iwcm.components.offline;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.users.UsersDB;

/**
 *  OfflineAction.java - generovanie HTML verzie stranky (na CD, alebo nieco ine)
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.28 $
 *@created      Date: 22.11.2004 22:05:14
 *@modified     $Date: 2010/02/09 08:39:12 $
 */
@WebServlet(name = "offlineServlet", urlPatterns = {"/admin/offline.do"})
public class OfflineAction extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(OfflineAction.class,"DeleteServlet  CALLED - GET");
        execute(request,response);
    }

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(OfflineAction.class,"DeleteServlet  CALLED - POST");
        execute(request,response);
    }

	public void execute(
         HttpServletRequest request,
         HttpServletResponse response)
         throws IOException
   {
		Logger.println(OfflineAction.class,"offlineAction");

		HttpSession session = request.getSession();
		if (session == null)
		{
			response.sendRedirect("/admin/");
			return;
		}

		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false)
		{
			response.sendRedirect("/admin/");
			return;
		}

		OfflineService service = new OfflineService();
		service.execute(user, request, response);
   }

}
