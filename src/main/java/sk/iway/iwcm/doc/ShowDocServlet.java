package sk.iway.iwcm.doc;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Streda, 2002, december 18
 *@modified     $Date: 2003/05/29 14:54:35 $
 */
public class ShowDocServlet extends HttpServlet
{
   private static final long serialVersionUID = 1L;

   /**
    *  Description of the Method
    *
    *@param  request               Description of the Parameter
    *@param  response              Description of the Parameter
    *@exception  ServletException  Description of the Exception
    *@exception  IOException       Description of the Exception
    */
   @Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      int docId = 1;
      String url = request.getRequestURI();

      if (request.getContextPath().length()>1)
      {
         url = url.substring(request.getContextPath().length());
      }

      docId = DocDB.getDocIdFromURL(url, DocDB.getDomain(request));

      request.setAttribute("docid", Integer.toString(docId));

      //Logger.println(this,"url="+url+" docId="+docId);

      getServletContext().getRequestDispatcher("/showdoc.do").forward(request, response);
   }
}
