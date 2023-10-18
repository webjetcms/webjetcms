package sk.iway.iwcm.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Servlet mapujuci stare Struts URL, neda sa na ne standardne bindnut Spring MVC pretoze sa spracuje prioritne ako Servlet
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: $
 */
@WebServlet(name = "strutsRedirectServlet", urlPatterns = {"/admin/editor.do"})
public class StrutsRedirectServlet extends HttpServlet
{
   private static final long serialVersionUID = 1L;

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String path = PathFilter.getOrigPath(request);
      String qs = (String)request.getAttribute("path_filter_query_string");

      Identity user = UsersDB.getCurrentUser(request);
      if (path.startsWith("/admin") && (user == null || user.isAdmin()==false))
      {
         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
         return;
      }

      //uprav URL pre format Springu (odstran .do) - /admin/editor.do -> /admin/editor
      if (path.endsWith(".do")) path = path.substring(0, path.length()-3);

      response.sendRedirect(path + "?" + qs);
   }
}
