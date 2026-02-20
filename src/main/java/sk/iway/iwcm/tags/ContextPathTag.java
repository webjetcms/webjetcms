package sk.iway.iwcm.tags;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 *  Vypise do stranky aktualny contextPath
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2010/01/20 11:15:08 $
 */
public class ContextPathTag extends TagSupport
{
	private static final long serialVersionUID = -4197645092015892217L;

	/**
    *  Description of the Method
    *
    *@return                   Description of the Return Value
    *@exception  JspException  Description of the Exception
    */
	@Override
   public final int doEndTag() throws JspException
   {
      try
      {
         HttpServletRequest request= (HttpServletRequest) pageContext.getRequest();

         //Logger.println(this,"cp: " + request.getContextPath());
         if (request!=null)
         {
            pageContext.getOut().write(request.getContextPath());
         }
      }
      catch (Exception e)
      {
         sk.iway.iwcm.Logger.error(e);
      }
      return EVAL_PAGE;
   }
}
