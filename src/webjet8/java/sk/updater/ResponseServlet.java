package sk.updater;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  ResponseServlet.java - defaultna odpoved na vsetky requesty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2004/08/18 15:24:59 $
 *@modified     $Date: 2004/08/18 15:24:59 $
 */
public class ResponseServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
		String path = request.getRequestURI();

		String message = "Web site is updating. Please try it later.";

		PrintWriter out = response.getWriter();
		out.println("<html><head>");
		if (path!=null && (path.indexOf("update_refresher.jsp")!=-1 || path.indexOf("logon.jsp")!=-1))
		{
			out.println("<meta http-equiv='refresh' content='10; url=/admin/logon.jsp'>");
			message = "System is updating, please wait.";
		}
		out.println("</head><body>");
		out.println(message);
		out.println("</body></html>");
   }

	@Override
	public void destroy()
	{
		System.out.println("Destroying ResponseServlet");
		super.destroy();
	}
}
