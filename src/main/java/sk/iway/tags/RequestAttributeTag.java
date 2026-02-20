package sk.iway.tags;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 *  vypise string ulozeny v request objekte (vyhodne ked sa to setne v nejakej
 *  Action triede)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      $Date: 2007/01/08 14:41:24 $
 */
public class RequestAttributeTag extends TagSupport
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6670483021319241043L;
	// Name of request attribute - required
   private String name = null;
   
   @Override
	public void release()
   {
   	super.release();
   	name = null;
   }

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
         Object value = ((HttpServletRequest) pageContext.getRequest()).getAttribute(name);
         if (value != null)
         {
            String text = value.toString();
            //text = new String(text.getBytes(), "iso-8859-1");
            pageContext.getOut().write(text);
         }
      }
      catch (Exception e)
      {
      	throw new JspTagException("RequestAttributeTag: " + e.getMessage());
      }
      return EVAL_PAGE;
   }

   /**
    *  Set the required tag attribute <b>name</b> .
    *
    *@param  newName  The new name value
    */
   public final void setName(String newName)
   {
      name = newName;
   }

}
