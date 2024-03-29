package sk.iway.iwcm.form;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * univerzalne poslanie mailu - definicia servletu, aby sa v zakaznickych instalaciach
 * dal modifikovat kod FormMailAction
 *
 *@Title        iwcm
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.30 $
 *@created      ďż˝tvrtok, 2002, marec 28
 *@modified     $Date: 2004/03/23 19:23:02 $
 */
@WebServlet(name = "FormMailAction",
		urlPatterns = {"/formmail.do"}
)
public class FormMailActionServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		FormMailAction.execute(request,response);
	}
}
